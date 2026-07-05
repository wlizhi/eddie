/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Agent 聊天请求参数
 * <p>
 * 与 ChatRequest 不同，Agent 请求由智能体接管执行流程，
 * 支持多轮迭代、工具调用、任务规划等复杂编排。
 * <p>
 * 用户临时选择的参数（模型、联网搜索等）通过此请求传入，
 * 优先级高于智能体配置，仅当前请求生效。
 */
@Getter
@Setter
public class AgentChatRequest {
    /**
     * 会话 ID，用于记忆隔离（续聊时传入）
     */
    @NotNull(message = "sessionId 不能为空")
    private Long sessionId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "message 不能为空")
    private String message;

    /**
     * 工具选择模式（auto / manual / none）
     */
    private String toolSelectionMode;

    /**
     * 手动模式下指定的工具名称列表
     */
    private List<String> toolNames;

    // ==================== 用户临时覆盖参数 ====================

    /**
     * 临时覆盖的主模型服务商 ID（优先级高于 Agent 配置，仅当前请求生效）
     */
    private Long providerId;

    /**
     * 临时覆盖的主模型 ID（优先级高于 Agent 配置，仅当前请求生效）
     */
    private String modelId;
}
