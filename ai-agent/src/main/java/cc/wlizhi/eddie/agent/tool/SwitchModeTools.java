/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.tool;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 智能体模式切换工具。<p>
 * 允许 AI 模型在对话过程中动态切换智能体的执行模式，
 * 以实现不同的处理策略（如从自由对话切换到任务规划）。
 * <p>
 * <b>模型可控的切换（通过调用此工具）：</b>
 * <ul>
 *   <li><b>CHAT → PLAN</b> — 从自由对话切换到规划模式</li>
 *   <li><b>EXECUTE → SUB_TASK</b> — 执行过程中需要拆分为独立子任务</li>
 * </ul>
 * <b>程序自动流转（无需工具调用）：</b>
 * <ul>
 *   <li><b>PLAN → EXECUTE</b> — 规划完成，程序自动进入执行阶段</li>
 *   <li><b>SUB_TASK → 上下文注入</b> — 子任务完成，结果自动注入</li>
 *   <li><b>任务完成 → CHAT</b> — 执行完毕自动回到对话模式</li>
 * </ul>
 * <p>
 * 通过 {@link ToolContext} 获取当前请求的 {@link AgentChatContext}，
 * 直接修改 {@link cc.wlizhi.eddie.agent.entity.dto.AgentIteratorState#agentMode}，
 * 无需经过 {@link cc.wlizhi.eddie.common.cache.EventRegistry} 桥接。
 */
@Component
public class SwitchModeTools implements AgentToolProvider {

    private static final Logger log = LoggerFactory.getLogger(SwitchModeTools.class);

    @Tool(name = "agent_switch_mode",
            description = """
                    切换当前消息的执行模式。
                    
                    可选值：
                    - PLAN — 切换至规划模式
                    - SUB_TASK — 切换至子任务模式
                    """)
    public ApiResult<String> switchMode(
            @ToolParam(description = "消息 ID") Long msgId,
            @ToolParam(description = "目标模式，可选值：PLAN / SUB_TASK") AgentMode mode,
            ToolContext toolContext) {

        // 1. 参数校验
        if (msgId == null) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "messageId 不能为空");
        }
        if (mode == null) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "mode 不能为空");
        }

        // 2. 校验只允许切换 PLAN 和 SUB_TASK
        if (mode != AgentMode.PLAN && mode != AgentMode.SUB_TASK) {
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

        return ApiResult.success("智能体执行模式已切换为 " + mode.name()
                + "(" + mode.getDesc() + ")");
    }
}
