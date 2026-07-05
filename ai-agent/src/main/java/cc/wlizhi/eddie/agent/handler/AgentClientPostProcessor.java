package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 轮次迭代中，构建合适参数的客户端并获取响应流
 */
public interface AgentClientPostProcessor {

    boolean support(AgentMode agentMode);

    ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(AgentChatContext context);
}
