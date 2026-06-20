/**
 * ChatContext — 聊天请求上下文
 * <p>
 * 贯穿整个聊天调用链路的上下文对象，从请求预处理到响应后处理，
 * 每个阶段都可以往 Context 中写入数据，后续阶段可直接读取。
 * <p>
 * 所有扩展点（InputInterceptor、ThinkingHandler 等）都通过此对象
 * 交换数据，避免方法参数膨胀和 ThreadLocal 的线程安全问题。
 */
package cc.wlizhi.eddieai.chat.entity.dto;

import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import org.springframework.ai.chat.model.ChatResponse;

public class ChatContext {

    // ==================== 阶段一：原始请求 ====================

    /** 用户原始请求 */
    private ChatRequest originalRequest;

    // ==================== 阶段二：路由 & Provider ====================

    /** 路由后的供应商实体 */
    private ModelProviderEntity provider;

    /** 供应商代码 (openai / deepseek / anthropic ...) */
    private String providerCode;

    /** 最终请求的模型 ID（经过 ModelNameMapper 映射后） */
    private String resolvedModelId;

    // ==================== 阶段三：执行 & 响应 ====================

    /** 请求开始时间戳（毫秒） */
    private long startTime;

    /** 最后一次 ChatResponse（用于提取 token 用量等元数据） */
    private ChatResponse lastResponse;

    // ==================== getter / setter ====================

    public ChatRequest getOriginalRequest() {
        return originalRequest;
    }

    public void setOriginalRequest(ChatRequest originalRequest) {
        this.originalRequest = originalRequest;
    }

    public ModelProviderEntity getProvider() {
        return provider;
    }

    public void setProvider(ModelProviderEntity provider) {
        this.provider = provider;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getResolvedModelId() {
        return resolvedModelId;
    }

    public void setResolvedModelId(String resolvedModelId) {
        this.resolvedModelId = resolvedModelId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public ChatResponse getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(ChatResponse lastResponse) {
        this.lastResponse = lastResponse;
    }
}
