package cc.wlizhi.eddieai.settings.remote;

import cc.wlizhi.eddieai.common.exception.BadRequestException;
import cc.wlizhi.eddieai.common.util.UrlUtil;
import cc.wlizhi.eddieai.settings.entity.response.ModelVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
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
 * 自动翻页：如果返回的 total 达到 page_size（100），继续请求下一页合并数据。
 * <p>
 * 文档：<a href="https://help.aliyun.com/zh/model-studio/list-deployable-models-api">列举可部署模型</a>
 */
@Component
public class DashScopeModelFetcher implements RemoteModelFetcher {

    private static final String MODELS_PATH = "/api/v1/deployments/models";
    private static final String PROVIDER_CODE = "dashscope";
    private static final int PAGE_SIZE = 100;

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
        List<ModelVO> allModels = new ArrayList<>();
        int pageNo = 1;
        int total;

        // 从 baseUrl 中提取 origin（协议+域名），去除兼容模式路径部分
        // 例如 https://dashscope.aliyuncs.com/compatible-mode/v1 → https://dashscope.aliyuncs.com
        java.net.URI uri = java.net.URI.create(baseUrl);
        String origin = uri.getScheme() + "://" + uri.getHost();
        if (uri.getPort() != -1) {
            origin += ":" + uri.getPort();
        }

        do {
            String url = UrlUtil.join(origin, MODELS_PATH)
                    + "?page_no=" + pageNo
                    + "&page_size=" + PAGE_SIZE
                    + "&version=v1.0"
                    + "&model_source=base";

            String json = restClient.get()
                    .uri(url)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(String.class);

            PageResult pageResult = parsePage(json);
            allModels.addAll(pageResult.models());
            total = pageResult.total();
            pageNo++;
        } while (total >= pageNo * PAGE_SIZE);
        allModels.sort(Comparator.comparing(ModelVO::getCode));
        allModels = allModels.reversed();
        return allModels;
    }

    /**
     * 解析单页返回数据，返回模型列表和总数
     */
    private PageResult parsePage(String json) {
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

            // 解析 total
            int total = 0;
            Object totalObj = output.get("total");
            if (totalObj instanceof Number) {
                total = ((Number) totalObj).intValue();
            }

            // 解析 models
            Object modelsObj = output.get("models");
            List<ModelVO> models = new ArrayList<>();
            if (modelsObj instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> modelsList = (List<Map<String, Object>>) modelsObj;
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
                    models.add(vo);
                }
            }

            return new PageResult(models, total);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析百炼远程模型列表返回数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 单页解析结果
     */
    private record PageResult(List<ModelVO> models, int total) {
    }
}
