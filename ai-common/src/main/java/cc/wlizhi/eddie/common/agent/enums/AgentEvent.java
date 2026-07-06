package cc.wlizhi.eddie.common.agent.enums;

import lombok.Getter;

@Getter
public enum AgentEvent {

    // ==================== 模型事件 =======================
    SWITCH_MODE_PLAN("切换至规划模式"),

    // ==================== SSE 事件（后端 → 前端） ====================

    MESSAGE_CREATED("消息已创建，当聊天内容发起时，将会先创建消息，JSON"),
    THINKING("模型思考内容，JSON"),
    ANSWER("模型回答内容，JSON"),
    TOOL_EXECUTION("工具执行结果 JSON"),
    UPDATE_TASK_PLAN("更新任务清单（全量任务清单内容） JSON"),
    ROUND_START("循环开始 JSON"),
    METADATA("每一轮执行完毕的元数据 JSON"),
    CANCELLED("任务取消 JSON"),
    ERROR("执行过程发生错误 JSON"),
    TASK_FINISH("任务结束，这表示前端层面本轮对话结束，后端不会再进行消息推送 JSON"),

    // ==================== 内部事件（EventRegistry 跨请求通信），由前端发起 ====================
    NEW_MSG_IN_TASK("任务中途用户发出新的指示"),
    STOP_MSG("停止本次对话回复或当前任务"),
    ;

    private final String eventDesc;

    AgentEvent(String eventDesc) {
        this.eventDesc = eventDesc;
    }
}
