/**
 * @author Eddie
 * {@code @date} 2026-07-15
 */

package cc.wlizhi.eddie.assistant.service;

import cc.wlizhi.eddie.assistant.entity.request.SelectionAssistantRequest;
import cc.wlizhi.eddie.assistant.entity.request.SelectionAssistantStopRequest;
import cc.wlizhi.eddie.common.dto.ApiResult;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * 划词助手业务接口
 * <p>
 * 根据 action 类型（translate / summarize / explain / beautify）
 * 自动解析模型配置，以 SSE 流式返回 AI 处理结果。
 */
public interface SelectionAssistantService {

    /**
     * 流式处理选中文本
     *
     * @param request 请求参数
     * @return SSE 事件流：<ul>
     *                <li><code>event: start</code> — 回答开始，携带会话唯一序列号 seq</li>
     *                <li><code>event: delta</code> — 模型输出片段</li>
     *                <li><code>event: cancelled</code> — 用户停止回答</li>
     *                <li><code>event: metadata</code> — 执行完毕元数据</li>
     *                <li><code>event: error</code> — 异常信息</li>
     *                </ul>
     */
    Flux<ServerSentEvent<String>> stream(SelectionAssistantRequest request);

    /**
     * 优雅停止当前回答
     * <p>
     * 通过 EventRegistry 注册停止事件，stream 流中的 takeWhile 检测到后终止。
     *
     * @param request 停止请求参数（action + 会话序列号）
     * @return ApiResult，序列号不存在时返回错误，前端可忽略
     */
    ApiResult<Void> stop(SelectionAssistantStopRequest request);
}
