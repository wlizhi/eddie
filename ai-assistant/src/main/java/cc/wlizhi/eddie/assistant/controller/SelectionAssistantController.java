/**
 * @author Eddie
 * {@code @date} 2026-07-15
 */

package cc.wlizhi.eddie.assistant.controller;

import cc.wlizhi.eddie.assistant.entity.request.SelectionAssistantRequest;
import cc.wlizhi.eddie.assistant.service.SelectionAssistantService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 划词助手 API
 * <p>
 * 提供翻译、总结、解释、美化功能的 SSE 流式接口。
 * 无需传入 providerId / modelId，后端根据 action 自动解析模型配置。
 * <p>
 * 事件类型：
 * <ul>
 *   <li><code>event: delta</code> — 模型输出片段（data: {"content":"..."}）</li>
 *   <li><code>event: metadata</code> — 执行完毕元数据（data: {"durationMs":1200}）</li>
 *   <li><code>event: error</code> — 异常信息（data: {"message":"请先在设置中配置翻译模型"}）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/selection-assistant")
public class SelectionAssistantController {

    @Resource
    private SelectionAssistantService selectionAssistantService;

    /**
     * 流式处理选中文本
     * <p>
     * 根据 action 自动选择模型：
     * <ul>
     *   <li>translate → 使用全局配置的翻译模型</li>
     *   <li>summarize / explain / beautify → 优先快速模型，降级到默认模型</li>
     * </ul>
     *
     * @param request 请求参数
     * @return SSE 事件流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@Validated @RequestBody SelectionAssistantRequest request) {
        return selectionAssistantService.stream(request);
    }
}
