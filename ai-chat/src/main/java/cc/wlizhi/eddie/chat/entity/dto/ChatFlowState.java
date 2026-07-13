/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.chat.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ChatFlowState — 聊天上下文流转状态
 * <p>
 * 存放 ChatContext 在执行链路中产生的运行时状态值，
 * 从 ChatContext 中抽取出来，减少上下文类的字段膨胀。
 */
@Getter
@Setter
public class ChatFlowState {

    /** 请求开始时间戳（毫秒） */
    private long startTime;

    /** 是否被用户中断（手动取消或网络断开） */
    private volatile boolean interrupted;

    /** 中断原因模式（如 "user" / "timeout"），仅当 interrupted=true 时有意义 */
    private String cancelMode;

    /**
     * 会话锁 token（由 SessionLockManager#tryLock 返回的 nanoTime）
     * <p>0 表示未持有锁，在 doFinally 中传递给 unlock 用以原子比对释放。
     */
    private long lockNanoTime;

    /**
     * 工具调用序号计数器（从 1 开始自动递增），
     * 用于构建唯一审批 key，区分同一轮对话中同一工具的多次调用。
     */
    private final AtomicInteger toolCallSequence = new AtomicInteger(0);
}
