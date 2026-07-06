/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.agent.controller;

import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.agent.service.AgentChatService;
import cc.wlizhi.eddie.common.dto.ApiResult;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 智能体 API
 * <p>
 * 使用 SSE (Server-Sent Events) 实现流式响应。
 * 事件类型：
 * <ul>
 *   <li><code>event: thinking</code> — 模型思考内容</li>
 *   <li><code>event: answer</code> — 模型回答内容</li>
 *   <li><code>event: tool_execution</code> — 工具执行状态</li>
 *   <li><code>event: milestone</code> — 关键里程碑</li>
 *   <li><code>event: round_start</code> — 新一轮迭代开始</li>
 *   <li><code>event: metadata</code> — 执行完毕元数据</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/agent")
public class AgentChatController {

    @Resource
    private AgentChatService agentChatService;

    /**
     * 发送 Agent 聊天消息，返回 SSE 流式响应
     * <p>
     * Agent 将自动进行任务规划、工具调用、多轮迭代，
     * 通过不同事件类型实时推送执行状态。
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@Validated @RequestBody AgentChatRequest request) {
        return agentChatService.chat(request);
    }

    /**
     * 停止 Agent 执行
     */
    @PostMapping("/stop")
    public ApiResult<Void> stop(@RequestParam(name = "messageId") Long messageId,
                                @RequestParam(name = "mode", defaultValue = "stop_msg") String mode) {
        agentChatService.stop(messageId, mode);
        return ApiResult.success();
    }
}
