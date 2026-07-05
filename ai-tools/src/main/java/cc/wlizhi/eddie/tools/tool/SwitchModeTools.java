/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.tools.tool;

import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.tool.BuiltInToolProvider;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 内置智能体模式切换工具。<p>
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
 * 调用后通过 {@link EventRegistry} 注册模式切换事件，
 * 智能体执行引擎在下一轮迭代中拾取事件并生效。
 */
@Component
public class SwitchModeTools implements BuiltInToolProvider {

    @Override
    public String getMcpServerName() {
        return "BuiltInAgentTools";
    }

    private static final Logger log = LoggerFactory.getLogger(SwitchModeTools.class);

    @Resource
    private EventRegistry eventRegistry;

    @Tool(name = "built_in_switch_mode",
            description = """
                    切换当前消息的执行模式。
                    
                    当需要规划任务步骤时，切换到 PLAN 模式；
                    当执行过程中需要拆分为独立子任务时，切换到 SUB_TASK 模式。
                    
                    可用模式：
                    - PLAN — 规划模式，拆解任务步骤
                    - SUB_TASK — 子任务模式，独立子任务处理
                    """)
    public ApiResult<String> switchMode(
            @ToolParam(description = "消息 ID") Long msgId,
            @ToolParam(description = "目标执行模式，可选值：PLAN / SUB_TASK") String agentMode) {

        // 1. 参数校验
        if (msgId == null) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "messageId 不能为空");
        }
        if (agentMode == null || agentMode.isBlank()) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "agentMode 不能为空");
        }

        // 2. 校验 agentMode 是否为有效的枚举值
        AgentMode mode;
        try {
            mode = AgentMode.valueOf(agentMode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST,
                    "无效的执行模式: " + agentMode + "，可选值：PLAN, SUB_TASK");
        }
        if (mode == AgentMode.CHAT || mode == AgentMode.EXECUTE) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST,
                    "无效的执行模式: " + agentMode + "，可选值：PLAN, SUB_TASK"
            );
        }

        // 3. 注册模式切换事件
        // 以 AgentMode 枚举类名作为事件类型前缀，messageId 作为业务 ID
        eventRegistry.register(AgentMode.class.getSimpleName(), msgId.toString(), mode);

        log.info("[SwitchModeTools] 模式切换事件已注册: messageId={}, agentMode={}", msgId, mode.name());

        return ApiResult.success("智能体执行模式已切换为 " + mode.name() + "(" + mode.getDesc() + ")");
    }
}
