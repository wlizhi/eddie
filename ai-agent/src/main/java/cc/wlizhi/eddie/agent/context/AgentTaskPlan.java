/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 任务规划 — 对应 ai_agent_session_msg.task_plan 字段的 JSON 结构。<p>
 * 规划模式（PLAN）下由模型输出，也可由后端在运行时更新状态字段。
 * <p>
 * 使用 Spring AI 的 {@code chatClient.call().entity(AgentTaskPlan.class)}
 * 可直接将模型输出的 JSON 反序列化为该对象。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentTaskPlan {

    /**
     * 消息 ID（后端运行时绑定，不参与模型 JSON 输出）
     */
    @JsonIgnore
    private Long messageId;

    /**
     * 任务标题（10字以内）
     */
    @NotBlank
    @Size(max = 50)
    @JsonPropertyDescription("任务标题，简短精炼，30字以内")
    private String title;

    /**
     * 任务概述
     */
    @NotBlank
    @Size(max = 300)
    @JsonPropertyDescription("简短的描述概括本次任务的目标和范围（不超过200字）")
    private String summary;

    /**
     * 任务状态：<br>
     * planned — 已规划（模型输出初始值）<br>
     * executing — 执行中（后端开始执行时更新）<br>
     * completed — 已完成（所有步骤完成）<br>
     * failed — 任务失败（出现不可恢复错误）
     */
    @JsonPropertyDescription("任务状态：planned/executing/completed/failed，初始值固定为 planned")
    private String status = "planned";

    /**
     * 任务完成描述（最终汇总文本，所有步骤完成后由后端填入）
     */
    @JsonPropertyDescription("任务完成后的最终汇总描述，初始为空字符串")
    private String result = "";

    /**
     * 步骤列表
     */
    @NotNull
    @Size(min = 1)
    @JsonPropertyDescription("步骤清单，每一个步骤应当是简单的，至少有一个步骤")
    private List<AgentTaskStep> steps;
}
