/**
 * @author Eddie
 * {@code @date} 2026-07-03
 */

package cc.wlizhi.eddie.settings.remote;

import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.common.util.UrlUtil;
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
 * Anthropic 远程模型获取器
 * <p>
 * Anthropic API 模型列表返回格式与 OpenAI 不同：
 * <pre>
 * GET {baseUrl}/v1/models
 * {
 *   "data": [
 *     {
 *       "type": "model",           // OpenAI 的 "object"
 *       "id": "claude-sonnet-5",
 *       "created_at": "2025-06-01T00:00:00Z"  // OpenAI 的 "created"（Unix 时间戳）
 *     }
 *   ],
 *   "has_more": false,
 *   "first_id": "claude-sonnet-5",
 *   "last_id": "claude-haiku-4-5"
 * }
 * </pre>
 * 文档：<a href="https://docs.anthropic.com">https://docs.anthropic.com</a>
 */
@Component
public class AnthropicModelFetcher implements RemoteModelFetcher {

    private static final String MODELS_PATH = "/v1/models";

    private final RestClient restClient;

    @Resource
    private ObjectMapper objectMapper;

    public AnthropicModelFetcher() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public List<String> supportedProviderCodes() {
        return List.of("anthropic");
    }

    @Override
    public List<ModelVO> fetchModels(String baseUrl, String apiKey) {
        String url = UrlUtil.join(baseUrl, MODELS_PATH);

        String json = restClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .retrieve()
                .body(String.class);

        return parseResponse(json);
    }

    /**
     * 解析 Anthropic 特有格式：{ data: [{ type, id, created_at }], has_more, first_id, last_id }
     */
    private List<ModelVO> parseResponse(String json) {
        try {
            Map<String, Object> root = objectMapper.readValue(json, new TypeReference<>() {
            });
            Object dataObj = root.get("data");
            if (!(dataObj instanceof List<?>)) {
                throw new BadRequestException("远程返回数据格式异常: data 字段缺失或非数组");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
            List<ModelVO> result = new ArrayList<>();

            for (Map<String, Object> item : dataList) {
                ModelVO vo = new ModelVO();
                // id 字段名相同
                Object idObj = item.get("id");
                vo.setCode(idObj != null ? idObj.toString() : null);
                // Anthropic 用 "type" 而非 "object"
                Object typeObj = item.get("type");
                vo.setObject(typeObj != null ? typeObj.toString() : null);
                // Anthropic 不提供 owned_by，使用默认值
                vo.setOwnedBy("anthropic");
                // Anthropic 用 "created_at"（ISO 8601 字符串），转换为 Unix 时间戳
                Object createdAtObj = item.get("created_at");
                if (createdAtObj instanceof String createdAtStr && !createdAtStr.isEmpty()) {
                    try {
                        java.time.Instant instant = java.time.Instant.parse(createdAtStr);
                        vo.setCreated(instant.getEpochSecond());
                    } catch (Exception ignored) {
                        // 非 ISO 格式则忽略
                    }
                }
                result.add(vo);
            }
            return result;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析 Anthropic 远程模型列表返回数据失败: " + e.getMessage(), e);
        }
    }
}
