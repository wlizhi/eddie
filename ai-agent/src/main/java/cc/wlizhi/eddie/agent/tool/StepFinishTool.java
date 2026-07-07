/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.tool;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentStepStreamContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskPlan;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskStep;
import cc.wlizhi.eddie.common.agent.enums.StepStatus;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class StepFinishTool implements AgentToolProvider {

    private static final Logger log = LoggerFactory.getLogger(StepFinishTool.class);

    @Tool(name = "agent_step_finish",
            description = """
                    更新当前步骤的执行状态。调用此工具后禁止再输出任何文本内容。
                    """)
    public ApiResult<String> stepFinish(
            @ToolParam(description = "消息 ID") Long msgId,
            @ToolParam(description = "步骤编号，对应完整计划清单中的步骤 id 值") Integer step,
            @ToolParam(description = """
                    取值范围：processing/completed/failed。必填。
                    processing — 本轮执行完成，但还需要继续迭代（如工具调用后需继续推理）；
                    completed — 步骤已成功完成；
                    failed — 步骤执行失败""") String status,
            @ToolParam(description = """
                    步骤执行结果描述，包含：
                    1. 当前步骤做了什么
                    2. 遇到哪些问题（有则写，无则不写）
                    3. 得到了什么结果
                    4. 哪些产出对后续步骤有帮助（最后的步骤完成/失败时除外）
                    使用简洁明了的逻辑描述""") String result,
            ToolContext toolContext) {

        // 1. 参数校验
        if (msgId == null) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "msgId 不能为空");
        }
        if (step == null) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "step 不能为空");
        }
        if (status == null) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "status 不能为空");
        }

        // 2. 校验 status 不能是 PENDING
        StepStatus stepStatus;
        try {
            stepStatus = StepStatus.fromValue(status);
        } catch (IllegalArgumentException e) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "无效的步骤状态: " + status);
        }
        if (stepStatus == StepStatus.PENDING) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "不允许将步骤状态设为 pending");
        }

        // 3. 从 ToolContext 获取 AgentChatContext
        AgentChatContext ctx = (AgentChatContext) toolContext.getContext()
                .get("agentChatContext");
        if (ctx == null) {
            return ApiResult.error(ApiResultCode.INTERNAL_ERROR, "无法获取当前请求上下文");
        }

        // 4. 交叉校验 msgId 与上下文中的消息 ID 一致
        if (!msgId.equals(ctx.getAgentMsg().getId())) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "消息 ID 不匹配");
        }

        // 5. 用 step 参数定位 taskPlan.steps 中的步骤
        AgentTaskPlan taskPlan = ctx.getTaskPlan();
        if (taskPlan != null && taskPlan.getSteps() != null
                && step > 0 && step <= taskPlan.getSteps().size()) {
            AgentTaskStep stepObj = taskPlan.getSteps().get(step - 1);
            stepObj.setStatus(stepStatus.getValue());
            stepObj.setResult(result != null ? result : "");
        } else {
            return ApiResult.error(ApiResultCode.BAD_REQUEST,
                    "步骤编号 " + step + " 超出任务计划范围");
        }

        // 6. 修改迭代缓冲：stepStreamContext.stepStatus
        AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
        if (stepCtx != null) {
            stepCtx.setStepStatus(stepStatus);
        }

        log.info("[StepFinishTool] 步骤 {} 标记{}, msgId={}",
                step, stepStatus.getValue(), msgId);
        return ApiResult.success("步骤 " + step + " 已标记为 " + stepStatus.getValue());
    }
}
