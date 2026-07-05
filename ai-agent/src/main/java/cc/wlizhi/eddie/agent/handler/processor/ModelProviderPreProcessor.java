/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import jakarta.annotation.Resource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Objects;

/**
 * 模型服务商数据预处理器 — 填充 {@link AgentChatContext#modelProvider}
 * <p>
 * 从 {@link AgentChatContext#agent} 的 mainProviderId 查询模型服务商配置。
 * 支持请求级覆盖：若 {@link AgentChatRequest#getProviderId()} 不为 null，
 * 则优先使用用户临时指定的服务商（仅当前请求生效）。
 * <p>
 * 校验服务商存在性、启用状态及 API Key 是否已配置。
 */
@Component
@Order(3)
public class ModelProviderPreProcessor implements AgentChatPreProcessor {

    @Resource
    private ModelProviderContext modelProviderContext;

    @Override
    public void process(AgentChatContext ctx) {
        AgentChatRequest request = ctx.getOriginalRequest();
        AgentEntity agent = ctx.getAgent();

        // 确定服务商 ID：请求级覆盖 ＞ Agent 配置
        Long providerId = request.getProviderId() != null
                ? request.getProviderId()
                : agent.getMainProviderId();

        ModelProviderEntity provider = modelProviderContext.getModelProviderById(providerId);
        if (provider == null) {
            throw new BadRequestException("providerId=" + providerId + " 不存在的模型服务商");
        }
        if (Objects.equals(provider.getEnabled(), 0)) {
            throw new BadRequestException(provider.getName() + " 模型服务已禁用");
        }
        if (ObjectUtils.isEmpty(provider.getApiKey()) && Objects.equals(provider.getBuiltIn(), 1)) {
            throw new BadRequestException("请先为 [" + provider.getName() + "] 配置 API Key");
        }

        ctx.setModelProvider(provider);
    }
}
