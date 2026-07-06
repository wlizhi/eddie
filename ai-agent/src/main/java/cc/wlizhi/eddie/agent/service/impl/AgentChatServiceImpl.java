/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentIteratorState;
import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.agent.handler.AgentClientPostProcessorRouter;
import cc.wlizhi.eddie.agent.handler.AgentEventPublisher;
import cc.wlizhi.eddie.agent.handler.ResponseStreamProcessorRouter;
import cc.wlizhi.eddie.agent.service.AgentChatService;
import cc.wlizhi.eddie.chat.advisor.ModelThrottleAdvisor;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.exception.SwitchModeToPlanException;
import cc.wlizhi.eddie.common.exception.UserStopException;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

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

    @Override
    public Flux<ServerSentEvent<String>> chat(AgentChatRequest request) {
        AgentChatContext ctx = new AgentChatContext();
        ctx.setStartTime(System.currentTimeMillis());
        ctx.setOriginalRequest(request);
        ctx.setEventPublisher(publisher);
        ctx.setEventRegistry(eventRegistry);

        // 按 @Order 顺序执行所有预处理器，填充 AgentChatContext 字段
        preProcessors(ctx);

        return Flux.create(sink -> {
            ctx.setSink(sink);
            // 消息已持久化（preProcessors 中已完成），通知前端消息 ID
            publisher.messageCreated(ctx);
            Thread agentThread = Thread.ofVirtual().name("demo-agent").start(() -> {
                doChat(ctx);
            });
            ctx.setAgentThread(agentThread);
            // 客户端断连时中断虚拟线程
            sink.onDispose(agentThread::interrupt);
        });
    }

    private void doChat(AgentChatContext ctx) {
        try {
            while (!shouldBreakIterator(ctx)) {
                log.debug("开始迭代：{}", ctx.getIteratorState().getCurrentIterator());
                // 迭代次数 + 1
                int currentRounds = ctx.getIteratorState().getCurrentIterator().addAndGet(1);

                // 通知前端本轮迭代开始
                publisher.roundStart(ctx, currentRounds);

                // 构建当前轮次合适的客户端，并获取阻塞式流
                ChatClient.ChatClientRequestSpec requestSpec = agentClientRouter.buildChatClientRequestSpec(ctx);
                // 委托策略处理器处理流并向前端推送事件
                // process() 内部在 afterStream() 中自动完成 token 提取 + 持久化 + metadata 推送
                try {
                    responseStreamRouter.process(ctx, requestSpec);
                } catch (SwitchModeToPlanException ignored) {
                    log.info("聊天模式切换至规划模式，进入下一轮迭代，当前迭代轮次：{}", ctx.getIteratorState().getCurrentIterator());
                }
                // 智能体内置工具（如 built_in_switch_mode）已在执行过程中
                // 通过 ToolContext 直接修改了 ctx.getIteratorState().agentMode
                // 会话结束还处于聊天模式，则退出循环
                if (shouldBreakIterator(ctx)) {
                    break;
                }
            }
        } catch (UserStopException e) {
            // 用户终止回答，仅打印一行 info 日志，不触发错误告警
            log.info("用户已停止回答: messageId={}", ctx.getAgentMsg().getId());
        } catch (Exception e) {
            // 检查是否被中断（sink.onDispose 触发）
            if (Thread.currentThread().isInterrupted()) {
                log.warn("Agent 虚拟线程被中断: messageId={}", ctx.getAgentMsg().getId());
                publisher.cancelled(ctx, "线程中断");
            } else {
                log.warn("Agent doChat 异常: messageId={}", ctx.getAgentMsg().getId(), e);
                publisher.error(ctx, "处理异常: " + e.getMessage());
            }
        } finally {
            // 通知前端本轮对话结束
            publisher.taskFinish(ctx);
            // 显式关闭 Flux，结束 SSE 连接
            try {
                ctx.getSink().complete();
            } catch (Exception ignored) {
                // sink 可能已被关闭或因中断异常
            }
        }
    }

    private boolean shouldBreakIterator(AgentChatContext ctx) {
        AgentIteratorState iteratorState = ctx.getIteratorState();
        if (ctx.getAgentThread().isInterrupted()) {
            return true;
        }
        String stopKey = EventRegistry.key(AgentEvent.STOP_MSG.name().toLowerCase(), ctx.getAgentMsg().getId().toString());
        // 用户发送了停止事件
        if (eventRegistry.get(stopKey) != null) {
            return true;
        }
        // 聊天模式，已经经过了一轮。
        if (iteratorState.getAgentMode() == AgentMode.CHAT && ctx.getIteratorState().getCurrentIterator().get() > 0) {
            return true;
        }
        // TODO 测试计划模式，回头删掉这个判断
        if (iteratorState.getAgentMode() == AgentMode.PLAN && ctx.getIteratorState().getCurrentIterator().get() > 1) {
            return true;
        }
        return iteratorState.getCurrentIterator().get() >= iteratorState.getMaxIterations();
    }

    private void preProcessors(AgentChatContext ctx) {
        for (AgentChatPreProcessor processor : preProcessors) {
            processor.process(ctx);
        }
        ChatClient newClient = ctx.getChatClient().mutate().defaultAdvisors(modelThrottleAdvisor).build();
        ctx.setChatClient(newClient);
    }

    @Override
    public void stop(Long messageId, String mode) {
        log.info("用户发送停止指令, Agent 任务: messageId={}", messageId);
        eventRegistry.register(AgentEvent.STOP_MSG.name().toLowerCase(), messageId.toString(),
                AgentEvent.STOP_MSG.name().toLowerCase());
    }
}
