/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import org.springframework.stereotype.Component;

/**
 * CHAT 模式流式响应处理器
 * <p>
 * 聊天模式：仅推送 thinking + answer 事件，无额外的模式特有事件。
 * 所有逻辑均由 {@link AbstractStreamProcessor} 基类提供，无需自定义。
 */
@Component
public class ChatResponseStreamProcessor extends AbstractStreamProcessor {

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.CHAT == agentMode;
    }
}
