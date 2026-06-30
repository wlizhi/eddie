/**
 * @author Eddie
 * {@code @date} 2026-06-22
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
 * DeepSeek 远程模型获取器
 * <p>
 * DeepSeek API 返回格式：
 * <pre>
 * {
 *   "object": "list",
 *   "data": [
 *     { "id": "deepseek-v4-flash", "object": "model", "owned_by": "deepseek" }
 *   ]
 * }
 * </pre>
 * 接口路径：{baseUrl}/models
 */
@Component
public class DeepSeekModelFetcher implements RemoteModelFetcher {

    private static final String MODELS_PATH = "/models";

    private final RestClient restClient;

    @Resource
    private ObjectMapper objectMapper;

    public DeepSeekModelFetcher() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public List<String> supportedProviderCodes() {
        return List.of("deepseek");
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
     * 解析 DeepSeek 返回的 {object, data[]} 格式
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
