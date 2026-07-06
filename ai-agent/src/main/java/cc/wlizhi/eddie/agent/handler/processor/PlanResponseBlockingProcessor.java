/**
 * @author Eddie
 * {@code @date} 2026-07-06
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.context.AgentTaskPlan;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.enums.ApiResultCode;
import cc.wlizhi.eddie.common.exception.AppException;
import cc.wlizhi.eddie.common.exception.UserStopException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

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
    protected ChatResponse doBlock(AgentChatContext ctx, ChatClient.ChatClientRequestSpec requestSpec) {
        long streamStart = System.currentTimeMillis();

        try {
            // 阻塞式调用模型 — CallResponseSpec 为延迟求值（lazy），
            // .call() 仅返回规格对象，不发起 API 调用。
            // entity() 作为首个终端方法注入 schema 并触发调用，
            // chatResponse() 复用同一底层缓存的响应获取 token 统计。
            var callSpec = requestSpec.call();

            // 使用 useProviderStructuredOutput() 通过 provider API 级别
            // 强制结构化输出，确保模型严格遵循 AgentTaskPlan JSON Schema
            AgentTaskPlan taskPlan = callSpec.entity(AgentTaskPlan.class,
                    ChatClient.EntityParamSpec::useProviderStructuredOutput);

            // 获取 ChatResponse 供基类 afterStream() 提取 token 统计
            ChatResponse chatResponse = callSpec.chatResponse();

            // 通过 AgentEventPublisher 统一发射 update_task_plan 事件
            // 自动包装为 {"msgId":..., "stepId":null, "data":{...}} JSON envelope
            publisher.updateTaskPlan(ctx, taskPlan);

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
    protected void afterBlock(AgentChatContext ctx) {
    }
}
