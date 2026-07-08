/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * CHAT 模式流式响应处理器
 * <p>
 * 聊天模式：仅推送 thinking + answer 事件，无额外的模式特有事件。
 * 流结束后调用 {@link #afterStream(AgentChatContext)} 持久化累积的回复内容。
 */
@Component
public class ChatResponseStreamProcessor extends AbstractStreamProcessor {

    private static final Logger log = LoggerFactory.getLogger(ChatResponseStreamProcessor.class);

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.CHAT == agentMode;
    }

    @Override
    protected boolean breakInStreamIfNecessary(AgentChatContext ctx) {
        return ctx.getIteratorState().getAgentMode() == AgentMode.PLAN;
    }

    @Override
    protected void handleAnswer(AgentChatContext ctx, ChatResponse response) {
        if (breakInStreamIfNecessary(ctx)) {
            // 任务规划中，不推送事件了，也不保存。
        } else {
            super.handleAnswer(ctx, response);
        }
    }

    @Override
    protected void afterStream(AgentChatContext ctx) {
        // 1. 基类通用逻辑：token 提取 + 增量持久化 + metadata 推送
        super.afterStream(ctx);

        // 2. CHAT 模式特有：持久化累积的 thinking / content，更新状态为 COMPLETED
        persistAccumulatedContent(ctx);
    }

    /**
     * 将流式处理过程中累积的 thinking / content / toolCalls 写回数据库，并更新 msgStatus = COMPLETED
     */
    private void persistAccumulatedContent(AgentChatContext ctx) {
        Long agentMsgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        if (agentMsgId == null) {
            log.warn("agentMsgId 为空，跳过内容持久化");
            return;
        }

        String content = ctx.getFullAnswer().toString();
        String thinking = ctx.getFullThinking().toString();

        // 序列化工具调用记录（与 ai-chat 模块的 ToolExecutionEvent 格式一致）
        String toolCallsJson = "[]";
        List<ToolExecutionEvent> toolCalls = ctx.getToolCalls();
        if (toolCalls != null && !toolCalls.isEmpty()) {
            try {
                toolCallsJson = ctx.getObjectMapper().writeValueAsString(toolCalls);
            } catch (JsonProcessingException e) {
                log.warn("序列化工具调用记录失败", e);
            }
        }

        if (content.isEmpty() && thinking.isEmpty() && "[]".equals(toolCallsJson)) {
            log.debug("累积内容为空，仅更新状态为 COMPLETED, agentMsgId={}", agentMsgId);
        }

        agentMsgDao.updateContentAndStatus(
                agentMsgId, content, thinking,
                toolCallsJson, "COMPLETED");

        log.info("AI 回复内容持久化完成, agentMsgId={}, contentLen={}, thinkingLen={}, toolCallsSize={}",
                agentMsgId, content.length(), thinking.length(), toolCalls.size());
    }
}
