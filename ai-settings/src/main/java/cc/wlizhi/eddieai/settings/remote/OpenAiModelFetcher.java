package cc.wlizhi.eddieai.settings.remote;

import cc.wlizhi.eddieai.common.exception.BadRequestException;
import cc.wlizhi.eddieai.common.util.UrlUtil;
import cc.wlizhi.eddieai.settings.entity.response.ModelVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 远程模型获取器（同时也是所有未知服务商的默认 fallback）
 * <p>
 * OpenAI List Models API 返回格式：
 * <pre>
 * {
 *   "object": "list",
 *   "data": [
 *     { "id": "gpt-4o", "object": "model", "created": 1693721698, "owned_by": "system" }
 *   ]
 * }
 * </pre>
 * 接口路径：{baseUrl}/models
 * <p>
 * 同时也是默认 fallback：当未找到特定服务商实现时，默认使用 OpenAI 兼容协议拉取。
 */
@Component
public class OpenAiModelFetcher implements RemoteModelFetcher {

    private static final String MODELS_PATH = "/models";
    private static final String PROVIDER_CODE = "openai";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiModelFetcher() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public List<String> supportedProviderCodes() {
        return List.of(PROVIDER_CODE);
    }

    @Override
    public List<ModelVO> fetchModels(String baseUrl, String apiKey) {
        String url = UrlUtil.join(baseUrl, MODELS_PATH);

        String json = restClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .body(String.class);

        return parseResponse(json);
    }

    /**
     * 解析 OpenAI 兼容的 {object, data[]} 格式响应
     */
    private List<ModelVO> parseResponse(String json) {
        try {
            Map<String, Object> root = objectMapper.readValue(json, new TypeReference<>() {
            });

            // 检查错误响应（部分服务商会在 JSON body 中返回错误信息）
            if (root.containsKey("code") && root.containsKey("message")) {
                String message = (String) root.get("message");
                throw new BadRequestException("远程拉取失败: " + message);
            }

            Object dataObj = root.get("data");
            if (!(dataObj instanceof List<?>)) {
                throw new BadRequestException("远程返回数据格式异常: data 字段缺失或非数组");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
            List<ModelVO> result = new ArrayList<>();

            for (Map<String, Object> item : dataList) {
                ModelVO vo = new ModelVO();
                Object idObj = item.get("id");
                vo.setCode(idObj != null ? idObj.toString() : null);
                Object objectObj = item.get("object");
                vo.setObject(objectObj != null ? objectObj.toString() : null);
                Object ownedByObj = item.get("owned_by");
                vo.setOwnedBy(ownedByObj != null ? ownedByObj.toString() : null);
                Object createdObj = item.get("created");
                if (createdObj instanceof Number) {
                    vo.setCreated(((Number) createdObj).longValue());
                }
                result.add(vo);
            }
            return result;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析远程模型列表返回数据失败: " + e.getMessage(), e);
        }
    }
}
