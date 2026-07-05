/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.tools.tool;

import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
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
                    
                    可选值：
                    - SWITCH_MODE_PLAN — 切换至规划模式
                    - SWITCH_MODE_SUB_TASK — 切换至子任务模式
                    """)
    public ApiResult<String> switchMode(
            @ToolParam(description = "消息 ID") Long msgId,
            @ToolParam(description = "切换模式，可选值：SWITCH_MODE_PLAN / SWITCH_MODE_SUB_TASK") String switchMode) {

        // 1. 参数校验
        if (msgId == null) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "messageId 不能为空");
        }
        if (switchMode == null || switchMode.isBlank()) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST, "switchMode 不能为空");
        }

        // 2. 校验并解析为 AgentEvent 常量
        AgentEvent switchEvent;
        try {
            switchEvent = AgentEvent.valueOf(switchMode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST,
                    "无效的切换模式: " + switchMode + "，可选值：SWITCH_MODE_PLAN, SWITCH_MODE_SUB_TASK");
        }
        if (switchEvent != AgentEvent.SWITCH_MODE_PLAN && switchEvent != AgentEvent.SWITCH_MODE_SUB_TASK) {
            return ApiResult.error(ApiResultCode.BAD_REQUEST,
                    "无效的切换模式: " + switchMode + "，可选值：SWITCH_MODE_PLAN, SWITCH_MODE_SUB_TASK");
        }

        // 3. 注册模式切换事件
        // 使用 AgentEvent 类名作为事件类型前缀（与消费者 switchModeIfNecessary 约定一致）
        eventRegistry.register(AgentEvent.class.getSimpleName(), msgId.toString(), switchEvent);

        log.info("[SwitchModeTools] 模式切换事件已注册: messageId={}, switchEvent={}", msgId, switchEvent.name());

        return ApiResult.success("智能体执行模式已切换为 " + switchEvent.name() + "(" + switchEvent.getEventDesc() + ")");
    }
}
