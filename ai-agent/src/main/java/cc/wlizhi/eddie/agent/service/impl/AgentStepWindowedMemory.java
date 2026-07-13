/**
 * @author Eddie
 * {@code @date} 2026-07-07
 */

package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.dao.AgentMsgStepDao;
import cc.wlizhi.eddie.agent.entity.AgentMsgStepEntity;
import cc.wlizhi.eddie.chat.entity.dto.ChatToolExecutionEvent;
import cc.wlizhi.eddie.memory.shortterm.AbstractWindowedMemory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行模式步骤记忆 — conversationId = "msgId:step"
 * <p>
 * 以 msgId 和 step 的复合值作为会话 ID，每个步骤拥有独立记忆窗口。
 * 从 ai_agent_session_msg_step 表懒加载，不限制轮数。
 * <p>
 * 与 {@link AgentShortTermMemory} 不同，此记忆管理步骤执行上下文
 * （prompt → toolCalls → content），而非对话消息。
 */
@Slf4j
@Component
public class AgentStepWindowedMemory extends AbstractWindowedMemory {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private AgentMsgStepDao agentMsgStepDao;

    @Override
    protected int resolveMaxRounds(String conversationId) {
        // 步骤记忆不限制轮数，保证所有步骤上下文可用
        return 10_000;
    }

    @Override
    @NonNull
    protected List<Message> loadHistory(String conversationId, int maxRounds) {
        // conversationId 格式: "msgId:step"，例如 "42:3"
        String[] parts = conversationId.split(":");
        if (parts.length != 2) {
            log.warn("非法的 conversationId 格式: {}", conversationId);
            return List.of();
        }

        long msgId;
        int step;
        try {
            msgId = Long.parseLong(parts[0]);
            step = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            log.warn("解析 conversationId 失败: {}", conversationId, e);
            return List.of();
        }

        // 精准查询：只查该 msgId + step 相关的交互记录（可能有多次迭代）
        List<AgentMsgStepEntity> rows = agentMsgStepDao.findByMsgIdAndStep(msgId, step);
        if (rows.isEmpty()) {
            return List.of();
        }

        // 遍历每行记录，按 id 正序组装消息序列
        List<Message> messages = new ArrayList<>();
        for (AgentMsgStepEntity row : rows) {
            // 1. user: prompt（步骤指令或模型追问）
            if (row.getPrompt() != null && !row.getPrompt().isEmpty()) {
                messages.add(new UserMessage(
                        "> **步骤 " + step + " 指令**\n\n" + row.getPrompt()));
            }

            // 2. tool: ToolExecutionEvent → 提取 result.data 为自然语言文本
            String toolText = formatToolResults(row.getToolCalls());
            if (!toolText.isEmpty()) {
                ToolResponseMessage toolMsg = ToolResponseMessage.builder()
                        .responses(List.of(new ToolResponseMessage.ToolResponse("", "", toolText)))
                        .metadata(java.util.Map.of())
                        .build();
                messages.add(toolMsg);
            }

            // 3. assistant: content（模型输出，不含 thinking）
            if (row.getContent() != null && !row.getContent().isEmpty()) {
                messages.add(new AssistantMessage(
                        "> **步骤 " + step + " 执行结果**\n\n" + row.getContent()));
            }
        }
        return messages;
    }

    /**
     * 清除指定消息的所有步骤记忆缓存
     * <p>
     * 任务结束后调用，主动释放不再需要的内存。
     *
     * @param msgId 消息 ID
     */
    public void clearByMsgId(Long msgId) {
        clearByPrefix(msgId + ":");
    }

    private String formatToolResults(String toolCallsJson) {
        if (toolCallsJson == null || toolCallsJson.isEmpty() || "[]".equals(toolCallsJson)) {
            return "";
        }
        try {
            List<ChatToolExecutionEvent> events = objectMapper.readValue(
                    toolCallsJson,
                    new TypeReference<List<ChatToolExecutionEvent>>() {
                    });
            if (events.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder("工具调用结果：\n\n");
            for (ChatToolExecutionEvent event : events) {
                String toolName = event.getToolName();
                String arguments = event.getArguments();
                String result = event.getResult();

                sb.append("工具名称：").append(toolName != null ? toolName : "未知").append("\n");
                if (arguments != null && !arguments.isEmpty()) {
                    sb.append("调用参数：\n").append(arguments).append("\n");
                }

                if (result == null || result.isEmpty()) {
                    sb.append("结果：无返回\n\n");
                    continue;
                }

                // result 可能是 JSON 包装（如 {"code":200,"data":"搜索内容...","message":"ok"}）
                // 优先提取 data 字段，否则直接用 result 文本
                try {
                    JsonNode resultNode = objectMapper.readTree(result);
                    JsonNode dataNode = resultNode.get("data");
                    if (dataNode != null && dataNode.isTextual()) {
                        sb.append("结果：\n").append(dataNode.asText()).append("\n\n");
                    } else if (dataNode != null) {
                        sb.append("结果：\n").append(dataNode.toPrettyString()).append("\n\n");
                    } else {
                        sb.append("结果：\n").append(resultNode.asText()).append("\n\n");
                    }
                } catch (Exception e) {
                    // result 不是 JSON，直接作为纯文本
                    sb.append("结果：").append(result).append("\n\n");
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("解析 toolCalls JSON 失败，降级为纯文本展示", e);
            return "工具调用结果：\n\n```json\n" + toolCallsJson + "\n```";
        }
    }
}
