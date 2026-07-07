/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.dao.AgentMsgDao;
import cc.wlizhi.eddie.agent.entity.dto.*;
import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.agent.handler.AgentClientPostProcessorRouter;
import cc.wlizhi.eddie.agent.handler.AgentEventPublisher;
import cc.wlizhi.eddie.agent.handler.ResponseStreamProcessorRouter;
import cc.wlizhi.eddie.agent.service.AgentChatService;
import cc.wlizhi.eddie.chat.advisor.ModelThrottleAdvisor;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.agent.enums.StepStatus;
import cc.wlizhi.eddie.common.agent.enums.TaskPlanStatus;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.exception.SwitchModeToPlanException;
import cc.wlizhi.eddie.common.exception.UserStopException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    @Resource
    private AgentStepWindowedMemory agentStepWindowedMemory;

    @Resource
    private AgentMsgDao agentMsgDao;

    @Resource
    private ObjectMapper objectMapper;

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

                // 检测当前迭代步骤是否已完成（由 agent_step_finish 工具标记）
                handleStepCompletionIfNeeded(ctx);
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
            // 任务完成，主动释放步骤记忆缓存
            try {
                agentStepWindowedMemory.clearByMsgId(ctx.getAgentMsg().getId());
            } catch (Exception ignored) {
                // 清理非关键操作，忽略异常
            }
            // 显式关闭 Flux，结束 SSE 连接
            try {
                ctx.getSink().complete();
            } catch (Exception ignored) {
                // sink 可能已被关闭或因中断异常
            }
        }
    }

    /**
     * 持久化 taskPlan 到数据库
     */
    private void persistTaskPlan(AgentChatContext ctx) {
        AgentTaskPlan taskPlan = ctx.getTaskPlan();
        if (taskPlan == null) {
            return;
        }
        Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
        if (msgId == null) {
            return;
        }
        try {
            String taskPlanJson = objectMapper.writeValueAsString(taskPlan);
            agentMsgDao.updateTaskPlan(msgId, taskPlanJson);
        } catch (Exception e) {
            log.warn("持久化 taskPlan 失败, msgId={}: {}", msgId, e.getMessage());
        }
    }

    /**
     * 检测当前 EXECUTE 模式下的步骤是否已完成或失败，若是则推进到下一步或结束任务。
     * <p>
     * 此方法仅在流结束后执行（不修改流处理中的 currentStep），
     * 通过 stepStreamContext.stepStatus 缓冲检测步骤终态。
     * <p>
     * 步骤失败（FAILED）时采用"继续执行"策略：任务标记 FAILED 但仍尝试执行剩余步骤。
     */
    private void handleStepCompletionIfNeeded(AgentChatContext ctx) {
        AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
        if (stepCtx == null) {
            return;
        }
        StepStatus status = stepCtx.getStepStatus();
        if (status != StepStatus.COMPLETED && status != StepStatus.FAILED) {
            return;
        }
        if (ctx.getIteratorState().getAgentMode() != AgentMode.EXECUTE) {
            return;
        }

        AgentTaskPlan taskPlan = ctx.getTaskPlan();
        if (taskPlan == null || taskPlan.getSteps() == null || taskPlan.getSteps().isEmpty()) {
            return;
        }

        boolean isFailed = status == StepStatus.FAILED;
        // 使用 stepStreamContext.step（工具实际更新的步骤编号）而非 ctx.getCurrentStep()
        int currentStep = stepCtx.getStep() != null ? stepCtx.getStep() : ctx.getCurrentStep();
        int totalSteps = taskPlan.getSteps().size();

        if (currentStep < totalSteps) {
            // 推进到下一步
            int nextStep = currentStep + 1;
            ctx.setCurrentStep(nextStep);
            AgentTaskStep nextPlanStep = taskPlan.getSteps().get(nextStep - 1);
            nextPlanStep.setStatus(StepStatus.PROCESSING.getValue());
            log.info("步骤 {} {}，推进至步骤 {}", currentStep, isFailed ? "失败" : "完成", nextStep);
        } else {
            // 最后一步完成或失败
            taskPlan.setStatus(isFailed ? TaskPlanStatus.FAILED.getValue() : TaskPlanStatus.COMPLETED.getValue());
            ctx.getIteratorState().setAgentMode(AgentMode.CHAT);
            log.info("所有步骤执行{}，切换回聊天模式", isFailed ? "（含失败步骤）" : "完成");
        }

        // 持久化 taskPlan 并推送更新事件
        persistTaskPlan(ctx);
        ctx.getEventPublisher().updateTaskPlan(ctx, taskPlan);
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
