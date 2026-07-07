/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.tool;

import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.dto.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class StepFinishTool implements AgentToolProvider {

    private static final Logger log = LoggerFactory.getLogger(StepFinishTool.class);


    // TODO 此工具待实现
    @Tool(name = "agent_step_finish",
            description = """
                    当前步骤完成时，请调用此工具。
                    
                    可选值：
                    - PLAN — 切换至规划模式
                    """)
    public ApiResult<String> switchMode(
            @ToolParam(description = "消息 ID") Long msgId,
            @ToolParam(description = "目标模式，可选值：PLAN") AgentMode mode,
            ToolContext toolContext) {

        return ApiResult.success();
    }
}
