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
 * 阿里云百炼远程模型获取器
 * <p>
 * 百炼 API（DashScope）返回格式：
 * <pre>
 * {
 *   "request_id": "f7da015c-...",
 *   "output": {
 *     "page_no": 1,
 *     "page_size": 100,
 *     "total": 5,
 *     "models": [
 *       {
 *         "model_name": "qwen3-8b",
 *         "plans": [ ... ]
 *       }
 *     ]
 *   }
 * }
 * </pre>
 * 接口路径：{baseUrl}/api/v1/deployments/models
 * <p>
 * 文档：<a href="https://help.aliyun.com/zh/model-studio/list-deployable-models-api">列举可部署模型</a>
 */
@Component
public class BailianModelFetcher implements RemoteModelFetcher {

    private static final String MODELS_PATH = "/api/v1/deployments/models";
    private static final String PROVIDER_CODE = "dashscope";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BailianModelFetcher() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public List<String> supportedProviderCodes() {
        return List.of(PROVIDER_CODE);
    }

    @Override
    public List<ModelVO> fetchModels(String baseUrl, String apiKey) {
        String url = UrlUtil.join(baseUrl, MODELS_PATH)
                + "?page_no=1&page_size=100&version=v1.0";

        String json = restClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .body(String.class);

        return parseResponse(json);
    }

    /**
     * 解析百炼返回的 {request_id, output} 格式
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

            Object outputObj = root.get("output");
            if (!(outputObj instanceof Map<?, ?>)) {
                throw new BadRequestException("远程返回数据格式异常: output 字段缺失或非对象");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> output = (Map<String, Object>) outputObj;

            Object modelsObj = output.get("models");
            if (!(modelsObj instanceof List<?>)) {
                return new ArrayList<>();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> modelsList = (List<Map<String, Object>>) modelsObj;
            List<ModelVO> result = new ArrayList<>();

            for (Map<String, Object> item : modelsList) {
                ModelVO vo = new ModelVO();
                Object nameObj = item.get("model_name");
                String modelName = nameObj != null ? nameObj.toString() : null;
                if (modelName == null) {
                    continue;
                }
                vo.setCode(modelName);
                vo.setObject("model");
                vo.setOwnedBy(PROVIDER_CODE);
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
