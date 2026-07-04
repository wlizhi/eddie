/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.chat.advisor;

import cc.wlizhi.eddie.memory.cache.ModelThrottleCache;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 模型调用节流 Advisor。
 * <p>
 * 实现 {@link BaseAdvisor} 接口，在每次模型调用前检查节流阀。
 * 从 {@link ChatClientRequest#context()} 中读取 {@code providerId} 和 {@code modelCode} 参数，
 * 调用 {@link ModelThrottleCache#checkAndThrottle(Long, String)} 进行节流判断。
 * <p>
 * 调用方需在 advisors 回调中传递参数：
 * <pre>{@code
 * .advisors(a -> a
 *     .param("providerId", providerId)
 *     .param("modelCode", modelCode))
 * }</pre>
 *
 * @author Eddie
 * {@code @date} 2026-07-04
 */
@Order(0)
@Component
public class ModelThrottleAdvisor implements BaseAdvisor {

    @Resource
    private ModelThrottleCache throttleCache;

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        Object providerIdObj = request.context().get("providerId");
        Object modelCodeObj = request.context().get("modelCode");
        if (providerIdObj instanceof Long providerId && modelCodeObj instanceof String modelCode) {
            throttleCache.checkAndThrottle(providerId, modelCode);
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    @Override
    public String getName() {
        return "modelThrottleAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
