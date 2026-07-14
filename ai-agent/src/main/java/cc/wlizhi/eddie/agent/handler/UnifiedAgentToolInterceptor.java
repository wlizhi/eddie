/**
 * @author Eddie
 * {@code @date} 2026-07-10
 */

package cc.wlizhi.eddie.agent.handler;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentStepStreamContext;
import cc.wlizhi.eddie.agent.entity.event.payload.AgentToolExecutionPayload;
import cc.wlizhi.eddie.chat.entity.dto.ChatToolExecutionEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.exception.ToolApprovalException;
import cc.wlizhi.eddie.common.exception.UserStopException;
import cc.wlizhi.eddie.common.tool.ToolBehavior;
import cc.wlizhi.eddie.common.tool.ToolBehavior.SecurityLevel;
import cc.wlizhi.eddie.common.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * 统一的智能体工具拦截器。<p>
 * 合并了原 {@code AgentApprovalInterceptor}（审批）和 {@code AgentToolCallbackWrapper}（事件发射+持久化），
 * 采用管道模式将工具调用拆分为清晰的步骤序列，消除所有 {@code instanceof} 判断。
 */
@Slf4j
public class UnifiedAgentToolInterceptor implements ToolCallback {

    private final ToolCallback delegate;
    private final ToolDefinitionEntity toolDef;
    private final List<ToolBehavior> behaviors;
    private final McpServerEntity mcpServer;
    private final AgentChatContext ctx;
    private final ObjectMapper objectMapper;

    /**
     * 当前调用的序号
     */
    private volatile int currentSeq = 0;
    /**
     * 是否被用户拒绝
     */
    private volatile boolean rejected = false;

    public UnifiedAgentToolInterceptor(
            ToolCallback delegate,
            ToolDefinitionEntity toolDef,
            List<ToolBehavior> behaviors,
            McpServerEntity mcpServer,
            AgentChatContext ctx) {
        this.delegate = delegate;
        this.toolDef = toolDef;
        this.behaviors = behaviors;
        this.mcpServer = mcpServer;
        this.ctx = ctx;
        this.objectMapper = ctx.getEvent().getObjectMapper() != null ? ctx.getEvent().getObjectMapper() : new ObjectMapper();
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

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String toolName = delegate.getToolDefinition().name();
        this.rejected = false;

        // ═══ 步骤 0：解析序号 ═══
        this.currentSeq = resolveSequence();

        // ═══ 步骤 1：发射 START 事件（最先通知前端"模型正在调用工具"） ═══
        emitSse(toolName, toolInput, "start", null, false);

        // ═══ 步骤 2：三层安全决策 ═══
        SecurityLevel security = resolveSecurity(toolInput);

        // ═══ 步骤 3：根据安全级别分流 ═══
        if (security == SecurityLevel.DENY) {
            log.info("[UnifiedAgentInterceptor] 行为被配置为 DENY: tool={}", toolName);
            emitSse(toolName, toolInput, "complete", "该操作已被管理员禁止执行。", true);
            persistToolCall(toolName, toolInput, "该操作已被管理员禁止执行。", true);
            return "该操作已被管理员禁止执行。";
        }

        if (security == SecurityLevel.APPROVAL) {
            // 3a. 发射 pending_approval 事件
            emitSse(toolName, toolInput, "pending_approval", null, false);

            // 3b. 阻塞等待审批
            String approvalKey = EventRegistry.key("tool_approval",
                    "agent:" + ctx.getAgentMsg().getId() + ":" + resolveStepRecordId() + ":" + currentSeq);
            String stopKey = getStopEventKey();
            Object approvalResult = stopKey != null
                    ? ctx.getEvent().getEventRegistry().waitFor(approvalKey, stopKey)
                    : ctx.getEvent().getEventRegistry().waitFor(approvalKey);

            if (approvalResult == null) {
                log.info("[UnifiedAgentInterceptor] 工具审批被中断: tool={}", toolName);
                throw new ToolApprovalException("审批被中断");
            }

            // 拒绝 → 发射 REJECTED 事件（终态）
            if (!"approved".equals(approvalResult)) {
                log.info("[UnifiedAgentInterceptor] 用户拒绝了工具调用: tool={}", toolName);
                String result = "提示：用户拒绝了此工具调用，请根据用户意图决定是询问用户还是换种方式继续回答。";
                emitSse(toolName, toolInput, "rejected", result, false);
                persistToolCall(toolName, toolInput, "用户拒绝了此工具调用", true);
                return result;
            }
            // 批准 → 继续执行
        }

        // ═══ 步骤 3.5：检查停止信号（执行前） ═══
        if (isStopRequested()) {
            log.info("[UnifiedAgentInterceptor] 用户已停止，跳过工具执行: {}", toolName);
            throw new UserStopException();
        }

        // ═══ 步骤 4：执行真实工具 ═══
        String result;
        try {
            result = delegate.call(toolInput, toolContext);
            result = JsonUtil.unwrapJsonString(result);
        } catch (UserStopException | ToolApprovalException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[UnifiedAgentInterceptor] 工具执行失败: {}", toolName, e);
            String errorMsg = "错误: " + e.getMessage();
            emitSse(toolName, toolInput, "complete", errorMsg, true);
            persistToolCall(toolName, toolInput, errorMsg, true);
            return "";
        }

        // ═══ 步骤 4.5：检查停止信号（执行后） ═══
        if (isStopRequested()) {
            log.info("[UnifiedAgentInterceptor] 用户已停止，丢弃工具结果: {}", toolName);
            throw new UserStopException();
        }

        // ═══ 步骤 5：三层截断 ═══
        String modelResult = truncateModel(result);
        String sseResult = truncateSse(modelResult);
        String storeResult = truncateStore(modelResult);

        // ═══ 步骤 6：发射 COMPLETE 事件（终态） ═══
        emitSse(toolName, toolInput, "complete", sseResult, false);

        // ═══ 步骤 7：持久化到上下文 ═══
        persistToolCall(toolName, toolInput, storeResult, false);

        return modelResult;
    }

    // ═══════════════════════════════════════════════
    //  三层安全决策（同 UnifiedChatToolInterceptor）
    // ═══════════════════════════════════════════════

    private SecurityLevel resolveSecurity(String toolInput) {
        if (toolDef != null && toolDef.getEnabled() != null && toolDef.getEnabled() == 2) {
            return SecurityLevel.APPROVAL;
        }
        if (behaviors == null || behaviors.isEmpty()) {
            return SecurityLevel.AUTO;
        }
        ToolBehavior matched = matchBehavior(toolInput, behaviors);
        if (matched == null) return SecurityLevel.AUTO;

        SecurityLevel userOverride = readUserOverride(matched);
        if (userOverride != null) return userOverride;

        SecurityLevel defaultLevel = matched.getDefaultSecurity();
        return defaultLevel != null ? defaultLevel : SecurityLevel.AUTO;
    }

    private ToolBehavior matchBehavior(String toolInput, List<ToolBehavior> behaviors) {
        if (toolInput == null || toolInput.isBlank()) return null;
        try {
            JsonNode args = objectMapper.readTree(toolInput);
            for (ToolBehavior b : behaviors) {
                String field = b.getDiscriminatorField();
                String value = b.getDiscriminatorValue();
                if (field == null || value == null) continue;
                JsonNode fn = args.get(field);
                if (fn != null && value.equals(fn.asText())) return b;
            }
        } catch (JsonProcessingException e) {
            log.warn("[UnifiedAgentInterceptor] 解析 toolInput 失败: {}", toolInput, e);
        }
        return null;
    }

    @Nullable
    private SecurityLevel readUserOverride(ToolBehavior behavior) {
        if (mcpServer == null) return null;
        String sourceConfig = mcpServer.getSourceConfig();
        if (sourceConfig == null || sourceConfig.isBlank() || "{}".equals(sourceConfig)) return null;
        try {
            JsonNode root = objectMapper.readTree(sourceConfig);
            String toolName = delegate.getToolDefinition().name();
            JsonNode toolNode = root.get(toolName);
            if (toolNode == null || toolNode.isNull()) return null;
            JsonNode behaviorsNode = toolNode.get("behaviors");
            if (behaviorsNode == null || behaviorsNode.isNull()) return null;
            JsonNode bn = behaviorsNode.get(behavior.getName());
            if (bn == null || bn.isNull()) return null;
            return parseSecurityLevel(bn.asText());
        } catch (JsonProcessingException e) {
            log.warn("[UnifiedAgentInterceptor] 解析 source_config 失败: {}", e.getMessage());
            return null;
        }
    }

    private static SecurityLevel parseSecurityLevel(String text) {
        if (text == null) return null;
        return switch (text.toUpperCase()) {
            case "AUTO" -> SecurityLevel.AUTO;
            case "APPROVAL", "PENDING_APPROVAL" -> SecurityLevel.APPROVAL;
            case "DENY" -> SecurityLevel.DENY;
            default -> null;
        };
    }

    // ═══════════════════════════════════════════════
    //  SSE 事件发射
    // ═══════════════════════════════════════════════

    private void emitSse(String toolName, String toolInput, String status, String result, boolean error) {
        try {
            Long msgId = ctx.getAgentMsg() != null ? ctx.getAgentMsg().getId() : null;
            Long stepRecordId = resolveStepRecordId();
            Integer stepNumber = resolveStepNumber();
            AgentToolExecutionPayload payload = new AgentToolExecutionPayload(
                    msgId, stepRecordId, stepNumber,
                    toolName, status, toolInput, result, error, currentSeq);
            ctx.getEvent().getEventPublisher().emit(ctx, AgentEvent.TOOL_EXECUTION, ApiResult.success(payload));
        } catch (Exception e) {
            log.warn("[UnifiedAgentInterceptor] 发射 SSE 事件失败: tool={}", toolName, e);
        }
    }

    // ═══════════════════════════════════════════════
    //  三层截断
    // ═══════════════════════════════════════════════

    private String truncateModel(String result) {
        if (result == null) return "";
        int maxLen = ctx.getOutput().getToolResultModelMaxLength();
        if (maxLen > 0 && result.length() > maxLen) {
            return result.substring(0, maxLen) + "\n\n...（工具结果已截断，更多内容请参考原始数据）";
        }
        return result;
    }

    private String truncateSse(String result) {
        if (result == null) return "";
        int maxLen = ctx.getOutput().getToolCallMaxLength();
        if (maxLen > 0 && result.length() > maxLen) {
            return result.substring(0, maxLen) + "...（已截断）";
        }
        return result;
    }

    private String truncateStore(String result) {
        if (result == null) return "";
        int maxLen = ctx.getOutput().getToolCallStoreMaxLength();
        if (maxLen > 0 && result.length() > maxLen) {
            return result.substring(0, maxLen) + "...（已截断）";
        }
        return result;
    }

    // ═══════════════════════════════════════════════
    //  持久化
    // ═══════════════════════════════════════════════

    private void persistToolCall(String toolName, String toolInput, String result, boolean error) {
        ChatToolExecutionEvent event = new ChatToolExecutionEvent();
        event.setStatus(error && rejected
                ? cc.wlizhi.eddie.common.enums.ToolExecutionStatus.REJECTED
                : cc.wlizhi.eddie.common.enums.ToolExecutionStatus.COMPLETE);
        event.setToolName(toolName);
        event.setArguments(toolInput);
        event.setResult(result);
        event.setError(error);
        event.setSeq(currentSeq);

        if (ctx.getIteratorState().getAgentMode() == AgentMode.EXECUTE) {
            AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
            if (stepCtx != null) stepCtx.getToolCalls().add(event);
        } else {
            ctx.getOutput().getToolCalls().add(event);
        }
    }

    // ═══════════════════════════════════════════════
    //  辅助方法
    // ═══════════════════════════════════════════════

    private int resolveSequence() {
        if (ctx.getStepStreamContext() != null) {
            return ctx.getStepStreamContext().getToolCallSequence().incrementAndGet();
        }
        return ctx.getOutput().getToolCallSequence().incrementAndGet();
    }

    private Long resolveStepRecordId() {
        AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
        return stepCtx != null ? stepCtx.getStepRecordId() : null;
    }

    private Integer resolveStepNumber() {
        AgentStepStreamContext stepCtx = ctx.getStepStreamContext();
        if (stepCtx != null && stepCtx.getStepNumber() != null) {
            return stepCtx.getStepNumber();
        }
        return ctx.getMetrics().getCurrentStepNumber();
    }

    @Nullable
    private String getStopEventKey() {
        if (ctx.getAgentMsg() == null) return null;
        return EventRegistry.key(
                AgentEvent.STOP_MSG.name().toLowerCase(),
                ctx.getAgentMsg().getId().toString());
    }

    private boolean isStopRequested() {
        EventRegistry registry = ctx.getEvent().getEventRegistry();
        if (registry == null || ctx.getAgentMsg() == null) return false;
        String stopKey = getStopEventKey();
        if (stopKey == null) return false;
        String stopVal = registry.get(stopKey);
        return Objects.equals(stopVal, AgentEvent.STOP_MSG.name().toLowerCase());
    }

    public boolean isRejected() {
        return rejected;
    }

    public int getCurrentSeq() {
        return currentSeq;
    }
}
