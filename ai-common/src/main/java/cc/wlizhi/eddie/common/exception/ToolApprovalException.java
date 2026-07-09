/**
 * @author Eddie
 * {@code @date} 2026-07-09
 */

package cc.wlizhi.eddie.common.exception;

import cc.wlizhi.eddie.common.enums.ApiResultCode;

/**
 * 工具审批被拒绝或中断时抛出的业务异常
 * <p>
 * 继承 {@link AppException}，在 {@code AgentChatServiceImpl.doChat()} 的内层 catch 中被捕获，
 * 仅打印一行 info 日志，不触发 error 告警，不浪费模型 token。
 * <p>
 * 使用场景：
 * <ul>
 *   <li>用户点击"拒绝"按钮 → 抛出此异常 → 立即结束当前轮次</li>
 *   <li>用户停止回答或断连导致审批等待中断 → 抛出此异常</li>
 * </ul>
 */
public class ToolApprovalException extends AppException {

    public ToolApprovalException(String message) {
        super(ApiResultCode.SUCCESS, message);
    }
}
