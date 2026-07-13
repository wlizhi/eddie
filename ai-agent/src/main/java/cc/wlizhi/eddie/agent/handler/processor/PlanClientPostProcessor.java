/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.AgentClientPostProcessor;
import cc.wlizhi.eddie.agent.handler.AgentPromptsResolver;
import cc.wlizhi.eddie.agent.service.impl.AgentShortTermMemory;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PLAN 模式 ChatClient 请求构建器
 * <p>
 * 规划模式不需要注册任何工具和记忆窗口，只需注入系统提示词和用户消息。
 * 响应处理由 {@link PlanResponseBlockingProcessor} 通过 {@code .call().entity(AgentTaskPlan.class)} 完成结构化输出。
 */
@Slf4j
@Component
public class PlanClientPostProcessor implements AgentClientPostProcessor {

    @Resource
    private AgentPromptsResolver agentPromptsResolver;
    @Resource
    private AgentShortTermMemory agentShortTermMemory;

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.PLAN == agentMode;
    }

    @Override
    public ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(AgentChatContext ctx) {
        String resolvePrompts = agentPromptsResolver.resolvePrompts(ctx);
        log.debug("PLAN 模式系统提示词：\n{}", resolvePrompts);
        var memoryAdvisor = MessageChatMemoryAdvisor.builder(agentShortTermMemory).build();
        return ctx.getEvent().getChatClient().mutate()
                .defaultAdvisors(memoryAdvisor)
                .build().prompt()
                .system(resolvePrompts)
                .user(ctx.getOriginalRequest().getMessage())
                .toolContext(Map.of("agentChatContext", ctx))
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", ctx.getOriginalRequest().getConversationId())
                        .param("providerId", ctx.getModelProvider().getId())
                        .param("modelCode", ctx.getUseModelInfo().getId()));
    }
}
