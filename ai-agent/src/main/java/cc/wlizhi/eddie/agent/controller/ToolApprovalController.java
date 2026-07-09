/**
 * @author Eddie
 * {@code @date} 2026-07-09
 */

package cc.wlizhi.eddie.agent.controller;

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
 * 工具审批控制器
 * <p>
 * 前端用户在聊天页面点击"批准/拒绝"时调用此接口，
 * 通过 {@link EventRegistry} 通知被阻塞的审批拦截器。
 * <p>
 * 审批 key 格式：{@code tool_approval:{ownerType}:{msgId}:{stepId}:{toolName}}
 * <ul>
 *   <li>{@code ownerType} — 归属类型：{@code agent} / {@code assistant}</li>
 *   <li>{@code msgId} — 消息 ID</li>
 *   <li>{@code stepId} — 步骤 ID（聊天场景为 {@code null}）</li>
 *   <li>{@code toolName} — 工具名称</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/agent/tools/approve")
public class ToolApprovalController {

    @Resource
    private EventRegistry eventRegistry;

    @PostMapping
    public ApiResult<Void> approveTool(@RequestBody ToolApprovalRequest request) {
        String key = EventRegistry.key("tool_approval",
                request.getOwnerType() + ":" + request.getMsgId()
                        + ":" + request.getStepId() + ":" + request.getSeq());
        eventRegistry.register(key, request.isApproved() ? "approved" : "rejected");
        return ApiResult.success();
    }

    @Getter
    @Setter
    public static class ToolApprovalRequest {
        /** 归属类型：agent / assistant */
        private String ownerType;
        /** 消息 ID */
        private Long msgId;
        /** 步骤 ID（聊天场景为 null） */
        private Integer stepId;
        /** 工具调用序号（审批 key 唯一标识，替代 toolName） */
        private int seq;
        /** 是否批准 */
        private boolean approved;
    }
}
