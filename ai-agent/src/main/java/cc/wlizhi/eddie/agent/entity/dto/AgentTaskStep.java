/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 任务计划中的单个步骤 — 对应 AgentTaskPlan.steps[] 的元素。<p>
 * 规划模式（PLAN）下由模型输出，执行过程中后端更新 status / result 字段。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentTaskStep {

    /**
     * 步骤序号（从 1 开始）
     */
    @NotNull
    @JsonPropertyDescription("步骤序号，从1开始递增")
    private Integer id;

    @JsonPropertyDescription("当前步骤标题，简短概括，30字以内")
    private String title;

    /**
     * 步骤描述 — 自包含的可执行描述，执行模型独立使用
     */
    @NotBlank
    @Size(max = 500)
    @JsonPropertyDescription("步骤描述，写清楚该步骤做什么、需要哪些信息、预期产出，要自包含可执行")
    private String description;

    /**
     * 完成标志 — 用于判断该步骤执行结果是否符合预期
     */
    @NotBlank
    @Size(max = 200)
    @JsonPropertyDescription("该步骤的完成标志，用于判断执行结果是否符合预期")
    private String goal;

    /**
     * 步骤状态：<br>
     * pending — 等待执行（模型输出初始值）<br>
     * processing — 执行中（后端开始执行时更新）<br>
     * completed — 已完成<br>
     * failed — 执行失败
     */
    @JsonPropertyDescription("步骤状态：pending/processing/completed/failed，初始值固定为 pending")
    private String status = "pending";

    /**
     * 步骤执行结果摘要（执行完成后由后端填入）
     */
    @JsonPropertyDescription("步骤执行结果摘要，初始为空字符串，执行完成后由后端填入")
    private String result = "";

    /**
     * 依赖的步骤 ID 列表（空列表表示无依赖，可与其他无依赖步骤并行执行）
     */
    @JsonProperty("depends_on")
    @JsonPropertyDescription("依赖的步骤ID列表，空数组表示无依赖，可与其他无依赖步骤并行执行")
    private List<Integer> dependsOn;

    /**
     * 预估复杂度：simple / medium / complex
     */
    @JsonProperty("estimated_complexity")
    @JsonPropertyDescription("预估复杂度：simple/medium/complex")
    private String estimatedComplexity;
}
