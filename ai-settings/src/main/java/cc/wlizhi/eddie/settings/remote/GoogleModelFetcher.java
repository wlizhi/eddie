/**
 * @author Eddie
 * {@code @date} 2026-07-03
 */

package cc.wlizhi.eddie.settings.remote;

import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.settings.entity.response.ModelVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini 远程模型获取器
 * <p>
 * OpenAI 兼容模式下不支持 GET /models，需调用 Gemini 原生 API：
 * <pre>
 * GET https://generativelanguage.googleapis.com/v1beta/models?key={apiKey}
 * {
 *   "models": [
 *     {
 *       "name": "models/gemini-2.5-pro",
 *       "version": "001",
 *       "displayName": "Gemini 2.5 Pro",
 *       "description": "...",
 *       "inputTokenLimit": 1048576,
 *       "outputTokenLimit": 8192,
 *       "supportedGenerationMethods": ["generateContent","countTokens"],
 *       "createTime": "2024-04-09T17:00:50.737346Z",
 *       "updateTime": "2024-12-02T18:10:16.932271Z",
 *       "labels": [{"name":"model-type","value":"pro"}]
 *     }
 *   ]
 * }
 * </pre>
 * 文档：<a href="https://ai.google.dev/api/models">https://ai.google.dev/api/models</a>
 */
@Component
public class GoogleModelFetcher implements RemoteModelFetcher {

    /**
     * Gemini 原生 API 路径（与 OpenAI 兼容模式的 baseUrl 不同）
     */
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta";
    private static final String MODELS_PATH = "/models";

    private final RestClient restClient;

    @Resource
    private ObjectMapper objectMapper;

    public GoogleModelFetcher() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public List<String> supportedProviderCodes() {
        return List.of("google");
    }

    @Override
    public List<ModelVO> fetchModels(String baseUrl, String apiKey) {
        // 忽略传入的 baseUrl，Gemini 模型列表需调用原生 API
        String url = GEMINI_API_BASE + MODELS_PATH + "?key=" + apiKey;

        String json = restClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .retrieve()
                .body(String.class);

        return parseResponse(json);
    }

    /**
     * 解析 Gemini 原生格式：{ models: [{ name, displayName, ... }] }
     */
    private List<ModelVO> parseResponse(String json) {
        try {
            Map<String, Object> root = objectMapper.readValue(json, new TypeReference<>() {
            });
            Object modelsObj = root.get("models");
            if (!(modelsObj instanceof List<?>)) {
                throw new BadRequestException("远程返回数据格式异常: models 字段缺失或非数组");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> modelsList = (List<Map<String, Object>>) modelsObj;
            List<ModelVO> result = new ArrayList<>();

            for (Map<String, Object> item : modelsList) {
                ModelVO vo = new ModelVO();
                // name 格式为 "models/gemini-2.5-pro"，提取最后一段作为 id
                Object nameObj = item.get("name");
                if (nameObj instanceof String nameStr) {
                    String id = nameStr.contains("/") ? nameStr.substring(nameStr.lastIndexOf('/') + 1) : nameStr;
                    vo.setCode(id);
                } else {
                    continue;
                }
                vo.setObject("model");
                vo.setOwnedBy("google");
                // createTime 为 ISO 8601 格式
                Object createTimeObj = item.get("createTime");
                if (createTimeObj instanceof String createTimeStr && !createTimeStr.isEmpty()) {
                    try {
                        java.time.Instant instant = java.time.Instant.parse(createTimeStr);
                        vo.setCreated(instant.getEpochSecond());
                    } catch (Exception ignored) {
                    }
                }
                result.add(vo);
            }
            return result;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析 Google Gemini 远程模型列表返回数据失败: " + e.getMessage(), e);
        }
    }
}
