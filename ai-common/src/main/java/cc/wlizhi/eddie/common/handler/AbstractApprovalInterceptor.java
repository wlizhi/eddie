/**
 * @author Eddie
 * {@code @date} 2026-07-09
 */

package cc.wlizhi.eddie.common.handler;

import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.exception.ToolApprovalException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

/**
 * 抽象工具审批拦截器
 * <p>
 * 包装原始 {@link ToolCallback}，在调用前通过 {@link EventRegistry#waitFor(String)}
 * 阻塞等待用户审批。子类负责实现 {@link #emitPendingApproval(String, String)} 方法，
 * 以不同的机制向前端发射 {@code pending_approval} 事件。
 * <p>
 * 审批 key 格式：{@code tool_approval:{ownerType}:{msgId}:{stepId}:{toolName}}
 * <ul>
 *   <li>{@code ownerType} — 归属类型：{@code agent} / {@code assistant}</li>
 *   <li>{@code msgId} — 消息 ID</li>
 *   <li>{@code stepId} — 步骤 ID（聊天场景为 {@code null}）</li>
 *   <li>{@code toolName} — 工具名称</li>
 * </ul>
 */
public abstract class AbstractApprovalInterceptor implements ToolCallback {

    protected static final Logger log = LoggerFactory.getLogger(AbstractApprovalInterceptor.class);

    protected final ToolCallback delegate;
    protected final EventRegistry eventRegistry;
    protected final String ownerType;
    protected final Long msgId;
    protected final Long stepId;

    /**
     * 标记当前工具调用是否被用户拒绝，供外层包装器（如 AgentToolCallbackWrapper）检查。
     * 在 {@link #call(String, ToolContext)} 返回提示字符串前设置为 true。
     * <p>
     * 每次 {@link #call(String, ToolContext)} 开始时重置为 false，
     * 防止同一拦截器实例多次调用时残留上次拒绝状态。
     * -- GETTER --
     *  检查当前工具调用是否被用户拒绝

     */
    @Getter
    private volatile boolean rejected = false;

    /**
     * 当前工具调用在本轮对话/步骤中的序号（从 1 开始递增），
     * 用于构建唯一审批 key，避免同一工具被多次调用时 key 冲突。
     * <p>
     * 由外层包装器（如 {@code AgentToolCallbackWrapper}）在每次调用前设置。
     */
    private volatile int toolCallSequence = 0;

    /**
     * @param delegate      原始工具回调
     * @param eventRegistry 事件注册表（用于阻塞等待审批结果）
     * @param ownerType     归属类型（agent / assistant）
     * @param msgId         消息 ID
     * @param stepId        步骤 ID（聊天场景为 null）
     */
    protected AbstractApprovalInterceptor(ToolCallback delegate, EventRegistry eventRegistry,
                                          String ownerType, Long msgId, @Nullable Long stepId) {
        this.delegate = delegate;
        this.eventRegistry = eventRegistry;
        this.ownerType = ownerType;
        this.msgId = msgId;
        this.stepId = stepId;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    /**
     * 设置当前工具调用的序号（由外层包装器在每次调用前设置，用于构建唯一审批 key）
     */
    public void setToolCallSequence(int toolCallSequence) {
        this.toolCallSequence = toolCallSequence;
    }

    /**
     * 获取当前工具调用序号（供子类在 pending_approval SSE 负载中传递到前端）
     */
    public int getToolCallSequence() {
        return toolCallSequence;
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        // 每次调用前重置拒绝状态&序列号，防止同一拦截器实例多次调用时残留上次状态
        this.rejected = false;
        // toolCallSequence 由外层包装器在调用前设置，此处不重置它

        String toolName = delegate.getToolDefinition().name();

        // 1. 发射 pending_approval SSE 事件（子类实现）
        emitPendingApproval(toolName, toolInput);

        // 2. 等待审批结果（同时监听停止信号）
        // 审批 key 使用工具调用序号而非工具名，确保同一工具多次调用时 key 唯一
        String approvalKey = EventRegistry.key("tool_approval",
                ownerType + ":" + msgId + ":" + stepId + ":" + toolCallSequence);
        String stopKey = getStopEventKey();
        Object result = stopKey != null
                ? eventRegistry.waitFor(approvalKey, stopKey)
                : eventRegistry.waitFor(approvalKey);

        // 3. 中断（断连/停止）
        if (result == null) {
            log.info("工具审批被中断, toolName={}, msgId={}", toolName, msgId);
            throw new ToolApprovalException("审批被中断");
        }

        // 4. 拒绝 → 返回提示字符串作为正常工具结果，让模型自主决策
        if (!"approved".equals(result)) {
            log.info("用户拒绝了工具调用, toolName={}, msgId={}", toolName, msgId);
            this.rejected = true;
            return "提示：用户拒绝了此工具调用，请根据用户意图决定是询问用户还是换种方式继续回答。";
        }

        // 5. 批准 → 执行原始工具
        log.info("用户批准了工具调用, toolName={}, msgId={}", toolName, msgId);
        return delegate.call(toolInput, toolContext);
    }

    /**
     * 发射 pending_approval 事件到前端
     *
     * @param toolName  工具名称
     * @param toolInput 工具调用参数（JSON 字符串）
     */
    protected abstract void emitPendingApproval(String toolName, String toolInput);

    /**
     * 获取停止事件的 key，用于在等待审批时同时监听停止信号
     * <p>
     * 子类根据所属模块返回对应的停止事件 key 格式：
     * <ul>
     *   <li>智能体：{@code stop_msg:{msgId}}</li>
     *   <li>助手聊天：{@code STOP:{msgId}}</li>
     * </ul>
     *
     * @return 停止事件 key，null 表示不监听停止信号
     */
    @Nullable
    protected abstract String getStopEventKey();
}
