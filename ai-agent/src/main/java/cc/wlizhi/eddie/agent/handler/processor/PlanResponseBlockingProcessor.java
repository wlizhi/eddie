/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskPlan;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskStep;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.agent.enums.StepStatus;
import cc.wlizhi.eddie.common.agent.enums.TaskPlanStatus;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.exception.AppException;
import cc.wlizhi.eddie.common.exception.UserStopException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * PLAN 模式响应处理器
 * <p>
 * 与流式模式不同，规划模式使用阻塞式 {@code .call().entity(AgentTaskPlan.class)}
 * 让模型输出结构化 JSON，反序列化后向前端推送，不走逐 token 流式渲染。
 * <p>
 * 事件推送由 {@link cc.wlizhi.eddie.agent.handler.AgentEventPublisher} 统一管理：
 * <ul>
 *   <li>{@code update_task_plan} — 规划结果（完整 JSON 结构）</li>
 *   <li>{@code metadata} — token 用量与耗时统计（{@code super.afterStream()} 自动处理）</li>
 *   <li>{@code error} — 异常情况（由 {@code publisher.error()} 发射）</li>
 * </ul>
 * <p>
 * 规划开始/完成的前端状态切换分别由 {@code round_start} 和 {@code task_finish} 事件承担，
 * 这些事件在 {@link cc.wlizhi.eddie.agent.service.impl.AgentChatServiceImpl} 中统一发射。
 */
@Component
public class PlanResponseBlockingProcessor extends AbstractBlockingProcessor {

    private static final Logger log = LoggerFactory.getLogger(PlanResponseBlockingProcessor.class);

    @Override
    public boolean support(AgentMode agentMode) {
        return AgentMode.PLAN == agentMode;
    }

    @Override
    public void process(AgentChatContext ctx, ChatClient.ChatClientRequestSpec requestSpec) {
        super.process(ctx, requestSpec);
    }

    @Override
    protected ChatResponse doBlock(AgentChatContext ctx, ChatClient.ChatClientRequestSpec requestSpec) {
        long streamStart = System.currentTimeMillis();

        try {
            // 发射规划开始事件，前端可据此显示"正在生成任务计划..."的加载指示器
            publisher.planStarted(ctx);

            // 阻塞式调用模型 — CallResponseSpec 为延迟求值（lazy），
            // .call() 仅返回规格对象，不发起 API 调用。
            // entity() 作为首个终端方法注入 schema 并触发调用，
            // chatResponse() 复用同一底层缓存的响应获取 token 统计。
            var callSpec = requestSpec.call();

            // 结构化输出
            AgentTaskPlan taskPlan = callSpec.entity(AgentTaskPlan.class, ChatClient.EntityParamSpec::validateSchema);

            // 发射规划生成成功事件，前端可据此隐藏"正在生成任务计划..."的加载指示器
            publisher.planGenerated(ctx, taskPlan);

            // 获取 ChatResponse 供基类 afterStream() 提取 token 统计
            ChatResponse chatResponse = callSpec.chatResponse();

            // 将规划清单存入上下文，供后续执行步骤使用
            ctx.setTaskPlan(taskPlan);

            // 持久化到数据库 task_plan 字段
            Long msgId = ctx.getAgentMsg().getId();
            try {
                String taskPlanJson = ctx.getObjectMapper().writeValueAsString(taskPlan);
                agentMsgDao.updateTaskPlan(msgId, taskPlanJson);
            } catch (Exception e) {
                log.warn("[PlanProcessor] 持久化 task_plan 失败, msgId={}: {}", msgId, e.getMessage());
            }

            // 更新 agentMsg 的 content（用于前端对话气泡展示简要文本）
            String summary = String.format("【任务规划】%s — %s", taskPlan.getTitle(), taskPlan.getSummary());
            ctx.getFullAnswer().append(summary);

            long elapsed = System.currentTimeMillis() - streamStart;
            log.debug("[PlanProcessor] 规划完成, title={}, steps={}, 耗时={}ms",
                    taskPlan.getTitle(),
                    taskPlan.getSteps() != null ? taskPlan.getSteps().size() : 0,
                    elapsed);
            return chatResponse;
        } catch (UserStopException e) {
            // 用户终止回答，直接透传，不包装
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - streamStart;
            log.warn("[PlanProcessor] 规划调用异常 after {}ms: {}", elapsed, e.getMessage(), e);
            throw new AppException(ApiResultCode.PROVIDER_CALL_FAILED,
                    "模型规划调用异常: " + e.getMessage(), e);
        }
    }

    @Override
    protected void extractBlockingContent(AgentChatContext ctx, ChatResponse response) {
        // 计划清单生成的时候，无需记录内容
    }

    @Override
    protected void afterBlock(AgentChatContext ctx) {
        // 切换到执行模式
        ctx.getIteratorState().setAgentMode(AgentMode.EXECUTE);
        // 初始化每个步骤的任务上下文
        ctx.setCurrentStep(1);
        List<AgentTaskStep> steps = ctx.getTaskPlan().getSteps();
        ctx.setTaskStepList(new ArrayList<>(steps.size()));
        steps.forEach(c -> ctx.getTaskStepList().add(new ArrayList<>()));

        // 更新任务计划及首个步骤的状态
        AgentTaskPlan taskPlan = ctx.getTaskPlan();
        if (taskPlan != null) {
            taskPlan.setStatus(TaskPlanStatus.EXECUTING.getValue());
            List<AgentTaskStep> planSteps = taskPlan.getSteps();
            if (planSteps != null && !planSteps.isEmpty()) {
                planSteps.getFirst().setStatus(StepStatus.PROCESSING.getValue());
            }

            // 推送更新后的 plan 给前端，使前端立即显示"执行中 / processing"状态
            publisher.updateTaskPlan(ctx, taskPlan);
        }
    }
}
