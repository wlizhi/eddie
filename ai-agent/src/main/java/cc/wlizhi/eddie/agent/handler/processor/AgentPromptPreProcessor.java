package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.util.PromptVariableResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(3)
@Slf4j
@Component
public class AgentPromptPreProcessor implements AgentChatPreProcessor {
    @Resource
    private PromptVariableResolver promptVariableResolver;

    @Override
    public void process(AgentChatContext ctx) {
        // 聊天模式系统提示词
        if (ctx.getIteratorState().getAgentMode() == AgentMode.CHAT) {
            String systemPrompt = ctx.getAgent().getSystemPrompt();
            String resolvePrompt = promptVariableResolver.resolve(systemPrompt);
            log.debug("{} 模式系统提示词预处理结果：{}", AgentMode.CHAT, resolvePrompt);
            ctx.getAgent().setSystemPrompt(resolvePrompt);
        }
        // TODO 规划模式系统提示词
        // TODO 执行模式系统提示词
        // TODO 子任务模式系统提示词
    }
}
