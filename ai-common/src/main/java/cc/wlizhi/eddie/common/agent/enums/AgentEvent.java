package cc.wlizhi.eddie.common.agent.enums;

import lombok.Getter;

@Getter
public enum AgentEvent {

    // ==================== 模型事件 =======================
    SWITCH_MODE_PLAN("切换至规划模式"),

    // ==================== SSE 事件（后端 → 前端） ====================

    MESSAGE_CREATED("消息已创建，这是模型调用前的预创建消息，JSON"),
    THINKING("模型思考内容，JSON"),
    TOOL_EXECUTION("工具执行结果 JSON"),
    ANSWER("模型回答内容，JSON"),
    ROUND_START("循环开始，代码层面的while循环，每一轮一个事件 JSON"),
    PLAN_STARTED("规划开始，模型开始生成任务清单 JSON"),
    PLAN_GENERATED("规划生成成功，任务清单首次生成完毕 JSON"),
    // ROUND_END 前端暂未使用
    ROUND_END("本轮循环结束"),
    UPDATE_TASK_PLAN("更新任务清单（全量任务清单内容） JSON"),
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
