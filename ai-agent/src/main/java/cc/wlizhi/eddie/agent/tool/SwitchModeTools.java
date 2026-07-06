/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.tool;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class SwitchModeTools implements AgentToolProvider {

    private static final Logger log = LoggerFactory.getLogger(SwitchModeTools.class);

    @Tool(name = "agent_switch_mode",
            description = """
                    切换当前消息的执行模式。
                    
                    可选值：
                    - PLAN — 切换至规划模式
                    """)
    public ApiResult<String> switchMode(
            @ToolParam(description = "消息 ID") Long msgId,
            @ToolParam(description = "目标模式，可选值：PLAN") AgentMode mode,
            ToolContext toolContext) {

        // 1. 参数校验
        if (msgId == null) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "messageId 不能为空");
        }
        if (mode == null) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "mode 不能为空");
        }

        // 2. 校验只允许切换 PLAN 和 SUB_TASK
        if (mode != AgentMode.PLAN) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST,
                    "无效的切换模式: " + mode + "，可选值：PLAN, SUB_TASK");
        }

        // 3. 从 ToolContext 获取 AgentChatContext，直接修改迭代器状态
        AgentChatContext ctx = (AgentChatContext) toolContext.getContext()
                .get("agentChatContext");
        if (ctx == null) {
            return ApiResult.error(ApiResultCode.INTERNAL_ERROR, "无法获取当前请求上下文");
        }

        AgentMode currentMode = ctx.getIteratorState().getAgentMode();
        ctx.getIteratorState().setAgentMode(mode);

        log.info("[SwitchModeTools] 模式切换成功: messageId={}, {} → {}", msgId, currentMode, mode);

        return ApiResult.success(AgentEvent.SWITCH_MODE_PLAN.name());
    }
}
