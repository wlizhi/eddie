/**
 * @author Eddie
 * {@code @date} 2026-07-07
 */

package cc.wlizhi.eddie.agent.entity.dto;

import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.common.agent.enums.StepStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 步骤级流式累加器 — 执行模式（EXECUTE）下，单次步骤迭代过程中
 * thinking/answer/toolCalls 的独立累加容器。
 * <p>
 * 与消息级别累加器（{@code AgentChatContext.fullAnswer/fullThinking}）隔离，
 * 每次迭代在 {@code beforeStream} 中创建，流结束后持久化到步骤表。
 */
@Setter
@Getter
public class AgentStepStreamContext {

    /**
     * 步骤记录 ID（预创建占位后得到的自增 ID）
     */
    private Long stepId;

    /**
     * 步骤序号（1-based，对应任务清单中的步骤编号）
     */
    private Integer step;

    /**
     * 当前迭代次数，从1开始
     */
    private Integer currentIterator = 1;

    /**
     * Prompt 提示词（轮次结束后获取，写入步骤记录的 prompt 字段）
     */
    private String prompt;

    /**
     * 步骤描述信息
     */
    private String stepDesc;

    /**
     * 当前迭代步骤的缓冲状态（从 taskPlan.steps[currentStep-1].status 同步而来）。
     * 工具执行中可修改此值，流结束后由 doChat 循环检测并回写到 taskPlan。
     */
    private StepStatus stepStatus;

    /**
     * 累积的 thinking 内容（reasoning_content）
     */
    private final StringBuilder fullThinking = new StringBuilder();

    /**
     * 累积的 answer 内容
     */
    private final StringBuilder fullAnswer = new StringBuilder();

    /**
     * 工具调用记录列表
     */
    private final List<ToolExecutionEvent> toolCalls = new ArrayList<>();
}
