package cc.wlizhi.eddie.chat.entity.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ChatAssistantMsgContext {
    /**
     * 占位 assistant 消息的 ID（流开始前插入，doFinally 中通过此 ID 更新内容）
     */
    private Long assistantMsgId;
    /**
     * 最后一次 ChatResponse（用于提取 token 用量等元数据）
     */
    private ChatResponse lastResponse;
    /**
     * 完整思考内容（StringBuilder，流式拼接）
     */
    private final StringBuilder fullThinking = new StringBuilder();

    /**
     * 完整回答内容（StringBuilder，流式拼接）
     */
    private final StringBuilder fullAnswer = new StringBuilder();

    /**
     * 响应元数据（由 ChatMetadataHandler 构建，同时供 SSE 和持久化使用）
     */
    private MetadataInfo metadata;

    /**
     * 工具执行记录列表（用于持久化到 ai_session_msg.tool_calls）
     */
    private List<ToolExecutionEvent> toolCalls = new ArrayList<>();
}
