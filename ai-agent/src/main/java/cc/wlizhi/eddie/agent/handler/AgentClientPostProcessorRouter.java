package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentClientPostProcessorRouter {
    @Resource
    private List<AgentClientPostProcessor> processors;

    public ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(AgentChatContext context) {
        for (AgentClientPostProcessor processor : processors) {
            if (processor.support(context.getIteratorState().getAgentMode())) {
                return processor.buildChatClientRequestSpec(context);
            }
        }
        throw new NotFoundException("找不到支持 " + context.getIteratorState().getAgentMode() + " 的 AgentClientPostProcessor");
    }
}
