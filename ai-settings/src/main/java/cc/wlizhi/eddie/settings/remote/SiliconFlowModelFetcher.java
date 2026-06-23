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
 * 硅基流动远程模型获取器
 * <p>
 * 硅基流动 API 返回格式（OpenAI 兼容）：
 * <pre>
 * {
 *   "object": "list",
 *   "data": [
 *     { "id": "stabilityai/stable-diffusion-xl-base-1.0", "object": "model", "owned_by": "" }
 *   ]
 * }
 * </pre>
 * 接口路径：{baseUrl}/models
 */
@Component
public class SiliconFlowModelFetcher implements RemoteModelFetcher {

    private static final String MODELS_PATH = "/models";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SiliconFlowModelFetcher() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public List<String> supportedProviderCodes() {
        return List.of("siliconflow");
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
     * 解析硅基流动返回的 {object, data[]} 格式（OpenAI 兼容）
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
                Object idObj = item.get("id");
                vo.setCode(idObj != null ? idObj.toString() : null);
                Object objectObj = item.get("object");
                vo.setObject(objectObj != null ? objectObj.toString() : null);
                Object ownedByObj = item.get("owned_by");
                vo.setOwnedBy(ObjectUtils.isEmpty(ownedByObj) ? "siliconflow" : ownedByObj.toString());
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
            throw new RuntimeException("解析硅基流动远程模型列表返回数据失败: " + e.getMessage(), e);
        }
    }
}
