package cc.wlizhi.eddie.settings.remote;

import cc.wlizhi.eddie.settings.entity.response.ModelVO;

import java.util.List;

/**
 * 远程模型获取器接口
 * <p>
 * 不同服务商返回的数据结构和接口路径可能不同，各自实现此接口处理差异化逻辑，
 * 包括 URL 拼接、HTTP 调用、响应解析等。
 */
public interface RemoteModelFetcher {

    /**
     * 返回该获取器支持的服务商 code 列表
     */
    List<String> supportedProviderCodes();

    /**
     * 从远程拉取模型列表
     *
     * @param baseUrl API 基础地址
     * @param apiKey  API 密钥
     * @return 模型列表（仅包含远程返回的基本信息，能力/价格等由调用方后续填充）
     */
    List<ModelVO> fetchModels(String baseUrl, String apiKey);
}
