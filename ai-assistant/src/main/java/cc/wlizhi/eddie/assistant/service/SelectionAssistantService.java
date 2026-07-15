/**
 * @author Eddie
 * {@code @date} 2026-07-15
 */

package cc.wlizhi.eddie.assistant.service;

import cc.wlizhi.eddie.assistant.entity.request.SelectionAssistantRequest;
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
     *                <li><code>event: delta</code> — 模型输出片段</li>
     *                <li><code>event: metadata</code> — 执行完毕元数据</li>
     *                <li><code>event: error</code> — 异常信息</li>
     *                </ul>
     */
    Flux<ServerSentEvent<String>> stream(SelectionAssistantRequest request);
}
