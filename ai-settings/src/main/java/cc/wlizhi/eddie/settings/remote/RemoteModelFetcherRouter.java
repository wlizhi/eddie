package cc.wlizhi.eddie.settings.remote;

import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.settings.entity.response.ModelVO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 远程模型获取器路由
 * <p>
 * 根据服务商 code 分发到对应的 {@link RemoteModelFetcher} 实现。
 * 新增服务商时，只需实现 {@link RemoteModelFetcher} 接口并注册为 Spring Bean，
 * 路由会自动发现。
 * <p>
 * 未匹配到特定服务商实现时，默认降级使用 {@link OpenAiModelFetcher}（OpenAI 兼容协议）拉取。
 */
@Component
public class RemoteModelFetcherRouter {

    private final Map<String, RemoteModelFetcher> fetcherMap;
    private final RemoteModelFetcher defaultFetcher;

    public RemoteModelFetcherRouter(List<RemoteModelFetcher> fetchers) {
        Map<String, RemoteModelFetcher> map = new HashMap<>();
        RemoteModelFetcher fallback = null;
        for (RemoteModelFetcher fetcher : fetchers) {
            for (String code : fetcher.supportedProviderCodes()) {
                map.put(code, fetcher);
            }
            if (fetcher instanceof OpenAiModelFetcher) {
                fallback = fetcher;
            }
        }
        this.fetcherMap = map;
        this.defaultFetcher = fallback;
    }

    /**
     * 根据服务商 code 远程拉取模型列表
     * <p>
     * 优先查找特定服务商实现，未找到时使用 {@link OpenAiModelFetcher} 降级拉取。
     *
     * @param providerCode 服务商业务 code（如 deepseek）
     * @param baseUrl      API 基础地址
     * @param apiKey       API 密钥
     * @return 模型列表
     * @throws BadRequestException 无可用的获取器（包括默认 fallback）时抛出
     */
    public List<ModelVO> fetchModels(String providerCode, String baseUrl, String apiKey) {
        RemoteModelFetcher fetcher = fetcherMap.get(providerCode);
        if (fetcher == null) {
            fetcher = defaultFetcher;
        }
        if (fetcher == null) {
            throw new BadRequestException("暂不支持远程拉取该服务商的模型列表: " + providerCode);
        }
        List<ModelVO> models = fetcher.fetchModels(baseUrl, apiKey);
        boolean blankCreated = models.stream().anyMatch(m -> m.getCreated() == null);
        if (!blankCreated) {
            // 统一按创建时间倒序排列，最新在前；created 为 null 的排最后
            models.sort(Comparator.nullsLast(
                    Comparator.comparingLong(ModelVO::getCreated).reversed()
            ));
        }
        return models;
    }
}
