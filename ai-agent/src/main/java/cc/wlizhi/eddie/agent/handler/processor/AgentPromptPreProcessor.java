package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
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
        String systemPrompt = ctx.getAgent().getSystemPrompt();
        String resolvePrompt = promptVariableResolver.resolve(systemPrompt);
        ctx.getAgent().setSystemPrompt(resolvePrompt);
    }
}
