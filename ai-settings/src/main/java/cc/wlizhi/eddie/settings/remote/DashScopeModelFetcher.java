package cc.wlizhi.eddie.settings.remote;

import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.common.util.UrlUtil;
import cc.wlizhi.eddie.settings.entity.response.ModelVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 阿里云百炼远程模型获取器（OpenAI 兼容接口）
 * <p>
 * 百炼 OpenAI 兼容接口返回格式：
 * <pre>
 * {
 *   "object": "list",
 *   "data": [
 *     { "id": "qwen-plus", "object": "model", "created": 1234567890, "owned_by": "system" }
 *   ]
 * }
 * </pre>
 * 接口路径：{baseUrl}/models
 * <p>
 * 文档：<a href="https://help.aliyun.com/zh/model-studio/compatibility-of-openai-with-dashscope">OpenAI 接口兼容说明</a>
 */
@Component
public class DashScopeModelFetcher implements RemoteModelFetcher {

    private static final String MODELS_PATH = "/models";
    private static final String PROVIDER_CODE = "dashscope";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashScopeModelFetcher() {
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
     * 解析 OpenAI 兼容格式的 {object, data[]} 响应
     */
    private List<ModelVO> parseResponse(String json) {
        try {
            Map<String, Object> root = objectMapper.readValue(json, new TypeReference<>() {
            });

            // 检查错误响应
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
                vo.setOwnedBy(ObjectUtils.isEmpty(ownedByObj) ? PROVIDER_CODE : ownedByObj.toString());
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
            throw new RuntimeException("解析百炼远程模型列表返回数据失败: " + e.getMessage(), e);
        }
    }
}
