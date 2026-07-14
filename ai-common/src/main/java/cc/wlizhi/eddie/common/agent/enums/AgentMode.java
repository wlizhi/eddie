package cc.wlizhi.eddie.common.agent.enums;

import lombok.Getter;


/**
 * 智能体执行模式
 */
@Getter
public enum AgentMode {
    CHAT("聊天模式"),
    PLAN("规划模式"),
    EXECUTE("执行模式"),
    // TODO 暂未实现
    SUB_TASK("子任务模式"),
    ;

    AgentMode(String desc) {
        this.desc = desc;
    }

    private final String desc;
}
