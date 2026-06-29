package cc.wlizhi.eddie.settings.remote;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.enums.ModelCapability;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型能力标签解析器
 * <p>
 * 根据模型 ID 关键词推断能力标签，
 * 同时支持从 classpath:model-capability-mapping.json 加载补充信息。
 * <p>
 * <b>解析策略（优先级）：</b>
 * <ol>
 *   <li><b>关键词推断（主要方式）</b>—— 根据模型名称中的关键词（如 vl → 视觉、r1 → 推理）推断能力</li>
 *   <li><b>静态映射补充</b> —— JSON 中精确匹配到的条目会在推断结果基础上<b>追加</b>能力（不覆盖）</li>
 * </ol>
 * 这种策略确保新发布的模型也能正确打标，同时静态映射可补充名称无法体现的能力（如 DashScope 的 web_search）。
 */
@Component
public class ModelCapabilityResolver {

    /**
     * 映射文件路径（位于 ai-app/src/main/resources/）
     */
    private static final String MAPPING_FILE = "model-capability-mapping.json";

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 映射数据：providerCode → (modelCode → List<ModelCapability>)
     */
    private final Map<String, Map<String, List<ModelCapability>>> mapping = new ConcurrentHashMap<>();
    @Resource
    private InitScheduler initScheduler;

    @PostConstruct
    public void init() {
        initScheduler.addTask(this.getClass().getSimpleName(), 0, this::doInit);
    }

    private void doInit() {
        try {
            ClassPathResource resource = new ClassPathResource(MAPPING_FILE);
            if (!resource.exists()) {
                return;
            }
            try (InputStream is = resource.getInputStream()) {
                Map<String, Map<String, List<String>>> raw = objectMapper.readValue(
                        is, new TypeReference<Map<String, Map<String, List<String>>>>() {
                        });

                for (Map.Entry<String, Map<String, List<String>>> providerEntry : raw.entrySet()) {
                    String providerCode = providerEntry.getKey();
                    Map<String, List<ModelCapability>> providerMap = new ConcurrentHashMap<>();

                    for (Map.Entry<String, List<String>> modelEntry : providerEntry.getValue().entrySet()) {
                        String modelCode = modelEntry.getKey();
                        List<ModelCapability> caps = modelEntry.getValue().stream()
                                .map(ModelCapability::fromCode)
                                .filter(c -> c != null)
                                .toList();
                        providerMap.put(modelCode, caps);
                    }

                    mapping.put(providerCode, providerMap);
                }
            }
        } catch (Exception e) {
            System.err.println("加载模型能力标签映射文件失败: " + e.getMessage());
        }
    }

    /**
     * 固定排序：按 ModelCapability 枚举声明顺序排列
     */
    private static final Comparator<ModelCapability> CAP_ORDER = Comparator.comparingInt(ModelCapability::ordinal);

    /**
     * 根据服务商 code 和模型 code 获取能力标签
     * <p>
     * 解析顺序：
     * <ol>
     *   <li>关键词推断（主要方式，覆盖所有模型）</li>
     *   <li>静态映射表补充（追加推断结果中可能缺失的能力，不覆盖）</li>
     *   <li>固定排序（确保前端展示顺序一致）</li>
     * </ol>
     *
     * @param providerCode 服务商 code
     * @param modelCode    模型 ID
     * @return 能力标签列表（不为 null），固定顺序：视觉 → 联网 → 推理 → 工具 → 重排 → 嵌入
     */
    public List<ModelCapability> resolve(String providerCode, String modelCode) {
        // 1) 关键词推断（主要方式，覆盖 90%+ 的模型）
        List<ModelCapability> inferred = inferCapabilities(modelCode);

        // 2) 静态映射表补充（处理名称无法体现的能力）
        List<ModelCapability> exact = lookupExact(providerCode, modelCode);
        List<ModelCapability> result;
        if (exact != null && !exact.isEmpty()) {
            // 合并：推断结果 + 映射表中额外的能力
            Set<ModelCapability> merged = new LinkedHashSet<>(inferred);
            merged.addAll(exact);
            result = new ArrayList<>(merged);
        } else {
            result = new ArrayList<>(inferred);
        }

        // 3) 固定排序：视觉 → 联网 → 推理 → 工具 → 重排 → 嵌入
        result.sort(CAP_ORDER);
        return Collections.unmodifiableList(result);
    }

    /**
     * 精确查找映射表
     */
    private List<ModelCapability> lookupExact(String providerCode, String modelCode) {
        if (providerCode == null || modelCode == null) {
            return null;
        }
        Map<String, List<ModelCapability>> providerMap = mapping.get(providerCode);
        if (providerMap == null) {
            return null;
        }
        return providerMap.get(modelCode);
    }

    /**
     * reload 方法，方便后续扩展为热加载
     */
    public void reload() {
        mapping.clear();
        init();
    }

    // ========================================================================
    //  关键词推断（核心逻辑）
    // ========================================================================

    /**
     * 根据模型 ID 关键词推断能力标签
     * <p>
     * 推断规则（按优先级）：
     * <ol>
     *   <li><b>专用模型</b>（嵌入/重排/图像生成）→ 返回特定能力，不追加工具调用</li>
     *   <li><b>聊天模型</b> → 逐个检查视觉/推理/联网搜索关键词，最后追加工具调用</li>
     * </ol>
     */
    private static List<ModelCapability> inferCapabilities(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return List.of(ModelCapability.FUNCTION_CALLING);
        }
        String id = modelId.toLowerCase().trim();

        // ======== 1. 专用模型（非聊天模型）优先匹配 ========

        // 嵌入模型：embedding / embed / bge(不含rerank)
        if (hasEmbeddingKeyword(id)) {
            return List.of(ModelCapability.EMBEDDING);
        }

        // 重排模型
        if (hasRerankKeyword(id)) {
            return List.of(ModelCapability.RERANK);
        }

        // 图像/视频生成模型（不支持任何聊天能力）
        if (isImageGenerationModel(id)) {
            return List.of();
        }

        // ======== 2. 聊天模型 — 推断各项能力 ========

        List<ModelCapability> caps = new ArrayList<>();

        // 视觉能力
        if (hasVisionKeyword(id)) {
            caps.add(ModelCapability.VISION);
        }

        // 推理能力
        if (hasReasoningKeyword(id)) {
            caps.add(ModelCapability.REASONING);
        }

        // 联网搜索能力
        if (hasSearchKeyword(id)) {
            caps.add(ModelCapability.WEB_SEARCH);
        }

        // 工具调用：所有聊天模型默认支持
        caps.add(ModelCapability.FUNCTION_CALLING);

        return caps;
    }

    // ---- 关键词匹配方法 ----

    /**
     * 嵌入模型关键词
     */
    private static boolean hasEmbeddingKeyword(String id) {
        if (id.contains("embedding") || id.contains("embed")) {
            return true;
        }
        // BGE 系列嵌入模型（BAAI/bge-m3, BAAI/bge-large-zh-v1.5 等）
        // 注意排除 bge-rerank 系列
        return id.contains("bge") && !id.contains("rerank");
    }

    /**
     * 重排模型关键词
     */
    private static boolean hasRerankKeyword(String id) {
        return id.contains("rerank") || id.contains("re-rank");
    }

    /**
     * 图像/视频生成模型关键词
     * <p>
     * 这类模型既不是聊天模型也不是嵌入/重排模型，不支持任何已定义的能力标签。
     */
    private static boolean isImageGenerationModel(String id) {
        return id.contains("stable-diffusion")
                || id.contains("sdxl")
                || id.contains("flux")
                || id.contains("dall-e")
                || id.contains("dalle");
    }

    /**
     * 视觉能力关键词
     * <p>
     * 覆盖：通用视觉词、多模态词、Omni/4o(OpenAI)、Qwen视觉推理系列
     */
    private static boolean hasVisionKeyword(String id) {
        // 通用视觉关键词
        if (id.contains("vision") || id.contains("visual")
                || id.contains("multimodal")
                || id.contains("image") || id.contains("video")) {
            return true;
        }
        // VL 系列（Vision-Language）：qwen-vl, deepseek-vl, llama-vision 等
        if (id.contains("vl") || id.contains("vl2")) {
            return true;
        }
        // Omni 多模态系列
        if (id.contains("omni")) {
            return true;
        }
        // OpenAI GPT-4o 系列（4o = omni）
        if (id.contains("4o")) {
            return true;
        }
        // Qwen QVQ 系列（视觉+推理）
        if (id.contains("qvq")) {
            return true;
        }
        return false;
    }

    /**
     * 推理能力关键词
     * <p>
     * 覆盖：OpenAI o1/o3 系列、DeepSeek R1/Reasoner、Qwen QwQ/QVQ、
     * 通用推理关键词 thinking/reasoning
     */
    private static boolean hasReasoningKeyword(String id) {
        // 推理通用词
        if (id.contains("reasoner") || id.contains("reasoning")
                || id.contains("thinking") || id.contains("think")
                || id.contains("deepthink")) {
            return true;
        }
        // DeepSeek R1 系列（含蒸馏版）
        if (id.contains("r1")) {
            return true;
        }
        // OpenAI o1/o3/o4 系列（前缀匹配）
        if (id.startsWith("o1") || id.startsWith("o3") || id.startsWith("o4")) {
            return true;
        }
        // Qwen QwQ/QVQ 推理系列
        if (id.contains("qwq") || id.contains("qvq")) {
            return true;
        }
        return false;
    }

    /**
     * 联网搜索能力关键词
     */
    private static boolean hasSearchKeyword(String id) {
        if (id.contains("search")) {
            return true;
        }
        // "web" 关键词，排除 webp 图片格式
        return id.contains("web") && !id.contains("webp");
    }
}
