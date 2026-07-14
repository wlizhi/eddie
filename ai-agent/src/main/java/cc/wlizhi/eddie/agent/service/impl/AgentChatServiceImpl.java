/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.entity.dto.*;
import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.agent.handler.AgentClientPostProcessorRouter;
import cc.wlizhi.eddie.agent.handler.AgentEventPublisher;
import cc.wlizhi.eddie.agent.handler.ResponseStreamProcessorRouter;
import cc.wlizhi.eddie.agent.service.AgentChatService;
import cc.wlizhi.eddie.chat.advisor.ModelThrottleAdvisor;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.exception.ToolApprovalException;
import cc.wlizhi.eddie.common.exception.UserStopException;
import com.openai.errors.InternalServerException;
import com.openai.errors.RateLimitException;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletionException;

@Service
public class AgentChatServiceImpl implements AgentChatService {

    private static final Logger log = LoggerFactory.getLogger(AgentChatServiceImpl.class);

    @Resource
    private EventRegistry eventRegistry;
    @Resource
    private List<AgentChatPreProcessor> preProcessors;
    @Resource
    private ModelThrottleAdvisor modelThrottleAdvisor;
    @Resource
    private AgentClientPostProcessorRouter agentClientRouter;
    @Resource
    private ResponseStreamProcessorRouter responseStreamRouter;
    @Resource
    private AgentEventPublisher publisher;
    @Resource
    private AgentStepWindowedMemory agentStepWindowedMemory;

    @Override
    public Flux<ServerSentEvent<String>> chat(AgentChatRequest request) {
        AgentChatContext ctx = new AgentChatContext();
        ctx.getMetrics().setStartTime(System.currentTimeMillis());
        ctx.setOriginalRequest(request);
        ctx.getEvent().setEventPublisher(publisher);
        ctx.getEvent().setEventRegistry(eventRegistry);


        return Flux.create(sink -> {
            ctx.getEvent().setSink(sink);
            Thread agentThread = Thread.ofVirtual().name("agent-chat").start(() -> {
                ctx.getEvent().setAgentThread(Thread.currentThread());
                doChat(ctx);
            });
            // 客户端断连时中断虚拟线程
            sink.onDispose(agentThread::interrupt);
        });
    }

    private void doChat(AgentChatContext ctx) {
        try {
            // 按 @Order 顺序执行所有预处理器，填充 AgentChatContext 字段
            preProcessors(ctx);

            // 消息已持久化（preProcessors 中已完成），通知前端消息 ID
            publisher.messageCreated(ctx);

            while (!shouldBreakIterator(ctx)) {
                log.debug("开始迭代：{}", ctx.getIteratorState().getCurrentIterator());
                // 迭代次数 + 1
                ctx.getIteratorState().getCurrentIterator().addAndGet(1);

                // 通知前端本轮迭代开始
                publisher.round(ctx, AgentEvent.ROUND_START);

                // 构建当前轮次合适的客户端，并获取阻塞式流
                ChatClient.ChatClientRequestSpec requestSpec = agentClientRouter.buildChatClientRequestSpec(ctx);
                // 委托策略处理器处理流并向前端推送事件
                // process() 内部在 afterStream() 中自动完成 token 提取 + 持久化 + metadata 推送
                try {
                    responseStreamRouter.process(ctx, requestSpec);
                } catch (ToolApprovalException e) {
                    log.info("工具审批被中断/拒绝: {}", e.getMessage());
                } catch (Exception ex) {
                    handleExceptionOnStreamProcess(ctx, ex);
                }

                publisher.round(ctx, AgentEvent.ROUND_END);
            }
        } catch (UserStopException e) {
            // 用户终止回答，仅打印一行 info 日志，不触发错误告警
            log.info("用户已停止回答: messageId={}", ctx.getAgentMsg().getId());
        } catch (Exception e) {
            handleExceptionOnDoChat(ctx, e);
        } finally {
            // 打印全链路耗时统计
            logStopWatch(ctx);
            // 通知前端本轮对话结束
            publisher.taskFinish(ctx);
            // 任务完成，主动释放步骤记忆缓存
            try {
                agentStepWindowedMemory.clearByMsgId(ctx.getAgentMsg().getId());
            } catch (Exception ignored) {
                // 清理非关键操作，忽略异常
            }
            // 显式关闭 Flux，结束 SSE 连接
            try {
                ctx.getEvent().getSink().complete();
            } catch (Exception ignored) {
                // sink 可能已被关闭或因中断异常
            }
        }
    }

    private void handleExceptionOnDoChat(AgentChatContext ctx, Exception e) {
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Agent 虚拟线程被中断: messageId={}", msgId);
            publisher.cancelled(ctx, "线程中断");
        }
        if (e.getCause() instanceof CompletionException) {
            Throwable actualCause = e.getCause().getCause();
            if (actualCause instanceof RateLimitException) {
                log.warn("{}: {}", ApiResultCode.AGENT_RATE_LIMIT.getMessage(), actualCause.getMessage());
                publisher.error(ctx, ApiResultCode.AGENT_RATE_LIMIT, actualCause.getMessage(), null);
                return;
            }
            if (actualCause instanceof InternalServerException) {
                log.warn("模型服务商返回服务端异常: statusCode={}, message={}",
                        ((InternalServerException) actualCause).statusCode(),
                        actualCause.getMessage());
                publisher.error(ctx, ApiResultCode.PROVIDER_CALL_FAILED,
                        "模型服务商接口异常（" + actualCause.getMessage() + "），请稍后重试", null);
                return;
            }
        }
        if (e.getCause() instanceof InterruptedException) {
            log.info("agent-chat 执行中断: messageId={}", msgId);
            return;
        }
        log.warn("Agent doChat 异常: messageId={}, exceptionType={}, message={}",
                msgId, e.getClass().getName(), e.getMessage(), e);
        String detail = e.getClass().getName() + ": " + e.getMessage();
        publisher.error(ctx, ApiResultCode.AGENT_INTERNAL_ERROR,
                "智能体处理异常，请稍后重试", detail);
    }

    private void handleExceptionOnStreamProcess(AgentChatContext ctx, Exception ex) throws Exception {
        String toolNameErrPrefix = "No ToolCallback found for tool name:";
        if (ex instanceof IllegalStateException && ex.getMessage() != null && ex.getMessage().startsWith(toolNameErrPrefix)) {
            log.warn("模型调用工具时，工具名输入错误：{}", ex.getMessage());
            String toolName = ex.getMessage().substring(toolNameErrPrefix.length()).trim();
            String toolErrorFeedback = """
                    注意：你刚才调用了工具 "%s"，但该工具不存在。请确认工具名称是否正确，仅使用系统提供的可用工具。
                    """.formatted(toolName);
            ctx.getEvent().getToolErrorFeedback().append(toolErrorFeedback);
            return;
        }
        throw ex;
    }

    private boolean shouldBreakIterator(AgentChatContext ctx) {
        AgentIteratorState iteratorState = ctx.getIteratorState();
        if (ctx.getEvent().getAgentThread().isInterrupted()) {
            return true;
        }
        String stopKey = EventRegistry.key(AgentEvent.STOP_MSG.name().toLowerCase(), ctx.getAgentMsg().getId().toString());
        // 用户发送了停止事件
        if (eventRegistry.get(stopKey) != null) {
            return true;
        }
        // 各 Processor 在适当时候设置 finished=true（语义正交于 agentMode）
        if (iteratorState.isFinished()) {
            return true;
        }
        return iteratorState.getCurrentIterator().get() >= iteratorState.getMaxIterations();
    }

    private void preProcessors(AgentChatContext ctx) {
        for (AgentChatPreProcessor processor : preProcessors) {
            ctx.getMetrics().getStopWatch().start(processor.getClass().getSimpleName());
            try {
                processor.process(ctx);
            } finally {
                ctx.getMetrics().getStopWatch().stop();
            }
        }
        ChatClient newClient = ctx.getEvent().getChatClient().mutate().defaultAdvisors(modelThrottleAdvisor).build();
        ctx.getEvent().setChatClient(newClient);
    }

    /**
     * 打印全链路耗时统计（各阶段、各策略实现类的逐条耗时 + 累计耗时）
     * <p>
     * 使用纳秒 API ({@link org.springframework.util.StopWatch.TaskInfo#getTimeNanos()}) 避免亚毫秒操作被截断为 0ms。
     * 小于 1ms 的操作显示微秒（μs），≥ 1ms 的操作显示毫秒（ms），累计值始终显示毫秒。
     */
    private void logStopWatch(AgentChatContext ctx) {
        var sw = ctx.getMetrics().getStopWatch();
        long cumulativeNanos = 0;
        for (var task : sw.getTaskInfo()) {
            long taskNanos = task.getTimeNanos();
            cumulativeNanos += taskNanos;
            if (taskNanos < 1_000_000) {
                log.info("[agent-chat-{}] {} 耗时 {}μs（累计 {}ms）",
                        ctx.getAgentMsg().getId(),
                        task.getTaskName(),
                        taskNanos / 1_000,
                        cumulativeNanos / 1_000_000);
            } else {
                log.info("[agent-chat-{}] {} 耗时 {}ms（累计 {}ms）",
                        ctx.getAgentMsg().getId(),
                        task.getTaskName(),
                        taskNanos / 1_000_000,
                        cumulativeNanos / 1_000_000);
            }
        }
        log.info("[agent-chat-{}] ===== 总耗时 {}ms =====",
                ctx.getAgentMsg().getId(),
                sw.getTotalTimeNanos() / 1_000_000);
    }

    @Override
    public void stop(Long messageId, String mode) {
        log.info("用户发送停止指令, Agent 任务: messageId={}", messageId);
        eventRegistry.register(AgentEvent.STOP_MSG.name().toLowerCase(), messageId.toString(),
                AgentEvent.STOP_MSG.name().toLowerCase());
    }
}
