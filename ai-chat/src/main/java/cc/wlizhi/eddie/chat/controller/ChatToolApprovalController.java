/**
 * @author Eddie
 * {@code @date} 2026-07-10
 */

package cc.wlizhi.eddie.chat.controller;

import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.dto.ApiResult;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 助手聊天工具审批控制器
 * <p>
 * 前端用户在聊天页面点击"批准/拒绝"时调用此接口，
 * 通过 {@link EventRegistry} 通知被阻塞的审批拦截器。
 * <p>
 * 审批 key 格式：{@code tool_approval:assistant:{msgId}:{seq}}
 * <ul>
 *   <li>{@code msgId} — 消息 ID</li>
 *   <li>{@code seq} — 工具调用序号</li>
 * </ul>
 * <p>
 * 与智能体审批 {@code ToolApprovalController} 独立，无需 stepId。
 */
@RestController
@RequestMapping("/api/chat/tools/approve")
public class ChatToolApprovalController {

    @Resource
    private EventRegistry eventRegistry;

    @PostMapping
    public ApiResult<Void> approveTool(@RequestBody ChatToolApprovalRequest request) {
        String key = EventRegistry.key("tool_approval",
                "assistant:" + request.getMsgId() + ":" + request.getSeq());
        eventRegistry.register(key, request.isApproved() ? "approved" : "rejected");
        return ApiResult.success();
    }

    @Getter
    @Setter
    public static class ChatToolApprovalRequest {
        /** 消息 ID */
        private Long msgId;
        /** 工具调用序号 */
        private int seq;
        /** 是否批准 */
        private boolean approved;
    }
}
