/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.ResponseStreamProcessor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.codec.ServerSentEvent;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 流式响应处理器 — 抽象模板方法基类
 * <p>
 * 定义流式处理的公共骨架：
 * <ol>
 *     <li>{@link #beforeStream(AgentChatContext)} — 流开始前钩子</li>
 *     <li>遍历每条 {@link ChatResponse}，依次执行：</li>
 *     <ul>
 *         <li>{@link #handleThinking(AgentChatContext, ChatResponse)} — 提取思考内容</li>
 *         <li>{@link #handleAnswer(AgentChatContext, ChatResponse)} — 提取回答内容</li>
 *         <li>{@link #handleCustomEvent(AgentChatContext, ChatResponse)} — 模式特有事件</li>
 *     </ul>
 *     <li>{@link #afterStream(AgentChatContext)} — 流结束后钩子</li>
 * </ol>
 * 子类只需覆盖需要自定义的钩子方法，公共逻辑由基类统一提供。
 * <p>
 * 适用于需要实时推送打字机效果的场景（thinking / answer 逐块推送）。
 * 若需要阻塞式等待全量结果，请使用 {@link AbstractBlockingProcessor}。
 */
public abstract class AbstractStreamProcessor implements ResponseStreamProcessor {

    @Override
    public void process(AgentChatContext ctx, ChatClient.ChatClientRequestSpec requestSpec) {
        // 钩子：流开始前的预处理
        beforeStream(ctx);

        // 遍历响应流
        requestSpec.stream().chatResponse().toStream().forEach(res -> {
            // 保存最后一次响应，供后续提取 tool_calls / token 用量
            ctx.setLastResponse(res);

            // 1. 提取并推送思考内容
            handleThinking(ctx, res);

            // 2. 提取并推送回答内容
            handleAnswer(ctx, res);

            // 3. 模式特有事件（子类可覆盖）
            handleCustomEvent(ctx, res);
        });

        // 钩子：流结束后的收尾
        afterStream(ctx);
    }

    // ================ 公共方法（子类通常无需覆盖） ================

    /**
     * 从 ChatResponse 提取模型思考内容（reasoning_content）
     * 兼容 OpenAI / DeepSeek 等不同服务商的 metadata key
     */
    protected void handleThinking(AgentChatContext ctx, ChatResponse response) {
        try {
            var metadata = Objects.requireNonNull(response.getResult()).getOutput().getMetadata();
            // key "reasoningContent" 兼容 OpenAI reasoning API
            Object reasoning = metadata.get("reasoningContent");
            if (reasoning == null) {
                // fallback: DeepSeek 使用的 key
                reasoning = metadata.get("reasoning_content");
            }
            if (reasoning != null && !reasoning.toString().isEmpty()) {
                ctx.getSink().next(ServerSentEvent.<String>builder()
                        .event("thinking")
                        .data(reasoning.toString())
                        .build());
            }
        } catch (Exception ignored) {
            // 忽略解析异常
        }
    }

    /**
     * 从 ChatResponse 提取模型生成的文本内容
     */
    protected void handleAnswer(AgentChatContext ctx, ChatResponse response) {
        try {
            String answer = response.getResults().stream()
                    .map(Generation::getOutput)
                    .map(AbstractMessage::getText).filter(Objects::nonNull)
                    .filter(f -> !f.isEmpty())
                    .collect(Collectors.joining());
            if (!answer.isEmpty()) {
                ctx.getSink().next(ServerSentEvent.<String>builder()
                        .event("answer")
                        .data(answer)
                        .build());
            }
        } catch (Exception ignored) {
            // 忽略解析异常
        }
    }

    // ================ 钩子方法（子类可按需覆盖） ================

    /**
     * 流开始前的预处理
     * <p>
     * 子类可在此向 sink 推送前置事件（如 "mode_switch"、"plan_start" 等）
     */
    protected void beforeStream(AgentChatContext ctx) {
        // 默认空实现
    }

    /**
     * 模式特有事件处理
     * <p>
     * 子类可在此推送该模式专属的 SSE 事件（如 "tool_call"、"execute_step" 等）
     */
    protected void handleCustomEvent(AgentChatContext ctx, ChatResponse response) {
        // 默认空实现
    }

    /**
     * 流结束后的收尾
     * <p>
     * 子类可在此推送结束事件（如 "plan_complete"、"task_done"）或执行资源清理
     */
    protected void afterStream(AgentChatContext ctx) {
        // 默认空实现
    }
}
