/**
 * @author Eddie
 * {@code @date} 2026-07-10
 */

package cc.wlizhi.eddie.chat.handler.impl;

import cc.wlizhi.eddie.chat.entity.dto.ChatToolExecutionEvent;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.enums.ToolExecutionStatus;
import cc.wlizhi.eddie.common.exception.ToolApprovalException;
import cc.wlizhi.eddie.common.tool.ToolBehavior;
import cc.wlizhi.eddie.common.tool.ToolBehavior.SecurityLevel;
import cc.wlizhi.eddie.common.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一的助手聊天工具拦截器。<p>
 * 合并了原 {@code ChatApprovalInterceptor}（审批）和 {@code ToolCallbackWrapper}（事件发射），
 * 采用管道模式将工具调用拆分为清晰的步骤序列：
 * <ol>
 *   <li><b>审批检查</b> — 按三层决策逻辑（助手级 enabled → 行为匹配 → 用户配置）决定是否需审批</li>
 *   <li><b>等待审批</b> — 阻塞等待用户确认/拒绝</li>
 *   <li><b>发射 START 事件</b> — 前端实时展示工具调用状态</li>
 *   <li><b>执行工具</b> — 委托给真实 @Tool 方法</li>
 *   <li><b>发射 COMPLETE/REJECTED 事件</b> — 推送执行结果</li>
 * </ol>
 * <p>
 * 所有元数据（行为列表、权限配置、工具定义）在构造时注入，
 * 运行时不再依赖外部上下文，无 {@code instanceof} 判断。
 */
public class UnifiedChatToolInterceptor implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(UnifiedChatToolInterceptor.class);

    private final ToolCallback delegate;
    private final ToolDefinitionEntity toolDef;
    private final List<ToolBehavior> behaviors;
    private final McpServerEntity mcpServer;
    private final EventRegistry eventRegistry;
    private final Long msgId;
    private final Sinks.Many<ChatToolExecutionEvent> sink;
    private final AtomicInteger toolCallCounter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 当前调用的序号（用于审批 key 唯一标识）
     */
    private volatile int currentSeq = 0;

    /**
     * 是否被用户拒绝
     */
    private volatile boolean rejected = false;

    public UnifiedChatToolInterceptor(
            ToolCallback delegate,
            ToolDefinitionEntity toolDef,
            List<ToolBehavior> behaviors,
            McpServerEntity mcpServer,
            EventRegistry eventRegistry,
            Long msgId,
            Sinks.Many<ChatToolExecutionEvent> sink,
            AtomicInteger toolCallCounter) {
        this.delegate = delegate;
        this.toolDef = toolDef;
        this.behaviors = behaviors;
        this.mcpServer = mcpServer;
        this.eventRegistry = eventRegistry;
        this.msgId = msgId;
        this.sink = sink;
        this.toolCallCounter = toolCallCounter;
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

        // 每次调用前重置状态
        this.rejected = false;
        this.currentSeq = toolCallCounter.incrementAndGet();

        // ═══ 步骤 1：发射 START 事件（最先通知前端"模型正在调用工具"） ═══
        ChatToolExecutionEvent startEvent = ChatToolExecutionEvent.start(toolName, toolInput);
        startEvent.setSeq(currentSeq);
        emitSafe(startEvent);

        // ═══ 步骤 2：三层安全决策 ═══
        SecurityLevel security = resolveSecurity(toolInput);

        // ═══ 步骤 3：根据安全级别分流 ═══
        if (security == SecurityLevel.DENY) {
            log.info("[UnifiedChatInterceptor] 行为被配置为 DENY: tool={}, input={}", toolName, toolInput);
            ChatToolExecutionEvent deniedEvent = ChatToolExecutionEvent.complete(
                    toolName, toolInput, "该操作已被管理员禁止执行。", true);
            deniedEvent.setSeq(currentSeq);
            emitSafe(deniedEvent);
            return "该操作已被管理员禁止执行。";
        }

        if (security == SecurityLevel.APPROVAL) {
            // 3a. 发射 pending_approval 事件
            emitPendingApproval(toolName, toolInput);

            // 3b. 阻塞等待审批结果（同时监听停止信号）
            // 审批 key = tool_approval:assistant:{msgId}:{seq}，msgId+seq 足够唯一
            String approvalKey = EventRegistry.key("tool_approval",
                    "assistant:" + msgId + ":" + currentSeq);
            String stopKey = msgId != null
                    ? EventRegistry.key("STOP", msgId.toString())
                    : null;
            Object approvalResult = stopKey != null
                    ? eventRegistry.waitFor(approvalKey, stopKey)
                    : eventRegistry.waitFor(approvalKey);

            // 中断（断连/停止）
            if (approvalResult == null) {
                log.info("[UnifiedChatInterceptor] 工具审批被中断: tool={}, msgId={}", toolName, msgId);
                throw new ToolApprovalException("审批被中断");
            }

            // 拒绝 → 发射 REJECTED 事件（终态）
            if (!"approved".equals(approvalResult)) {
                log.info("[UnifiedChatInterceptor] 用户拒绝了工具调用: tool={}, msgId={}", toolName, msgId);
                this.rejected = true;
                ChatToolExecutionEvent rejectedEvent = ChatToolExecutionEvent.rejected(toolName, toolInput);
                rejectedEvent.setSeq(currentSeq);
                rejectedEvent.setResult("提示：用户拒绝了此工具调用，请根据用户意图决定是询问用户还是换种方式继续回答。");
                emitSafe(rejectedEvent);
                return rejectedEvent.getResult();
            }
            // 批准 → 继续执行
        }

        // ═══ 步骤 4：执行真实工具 ═══
        String result;
        try {
            result = delegate.call(toolInput, toolContext);
            // 只有 "以双引号开头且是合法 JSON 字符串" 才解码
            // 这样不会误伤 Map/List/POJO 的 JSON 对象输出
            result = JsonUtil.unwrapJsonString(result);
        } catch (Exception e) {
            log.error("[UnifiedChatInterceptor] 工具执行失败: {}", toolName, e);
            ChatToolExecutionEvent errorEvent = ChatToolExecutionEvent.complete(
                    toolName, toolInput, "错误: " + e.getMessage(), true);
            errorEvent.setSeq(currentSeq);
            emitSafe(errorEvent);
            throw e;
        }

        // ═══ 步骤 5：发射 COMPLETE 事件（终态） ═══
        ChatToolExecutionEvent completeEvent = ChatToolExecutionEvent.complete(
                toolName, toolInput, result, false);
        completeEvent.setSeq(currentSeq);
        emitSafe(completeEvent);

        return result;
    }

    // ═══════════════════════════════════════════════
    //  三层安全决策
    // ═══════════════════════════════════════════════

    /**
     * 解析本次工具调用的安全级别。
     * <p>
     * 决策顺序：
     * <ol>
     *   <li>助手级 {@code enabled=2} → 整个工具需审批</li>
     *   <li>工具无行为声明 → 自动放行（兼容旧工具）</li>
     *   <li>匹配行为 → 检查用户配置覆盖 → 代码默认值</li>
     * </ol>
     */
    private SecurityLevel resolveSecurity(String toolInput) {
        // 第 1 层：助手级 enabled=2 → 整工具需审批
        if (toolDef.getEnabled() != null && toolDef.getEnabled() == 2) {
            return SecurityLevel.APPROVAL;
        }

        // 第 2 层：工具未声明行为 → 全部放行
        if (behaviors == null || behaviors.isEmpty()) {
            return SecurityLevel.AUTO;
        }

        // 第 3 层：匹配行为
        ToolBehavior matched = matchBehavior(toolInput, behaviors);
        if (matched == null) {
            // 未匹配到任何已知行为 → 安全兜底，放行
            return SecurityLevel.AUTO;
        }

        // 第 4 层：检查用户配置覆盖
        SecurityLevel userOverride = readUserOverride(matched);
        if (userOverride != null) {
            return userOverride;
        }

        // 第 5 层：代码默认值
        SecurityLevel defaultLevel = matched.getDefaultSecurity();
        return defaultLevel != null ? defaultLevel : SecurityLevel.AUTO;
    }

    /**
     * 从 toolInput JSON 中匹配对应的行为声明。
     */
    private ToolBehavior matchBehavior(String toolInput, List<ToolBehavior> behaviors) {
        if (toolInput == null || toolInput.isBlank()) {
            return null;
        }
        try {
            JsonNode args = objectMapper.readTree(toolInput);
            for (ToolBehavior behavior : behaviors) {
                String field = behavior.getDiscriminatorField();
                String value = behavior.getDiscriminatorValue();
                if (field == null || value == null) continue;

                JsonNode fieldNode = args.get(field);
                if (fieldNode != null && value.equals(fieldNode.asText())) {
                    return behavior;
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("[UnifiedChatInterceptor] 解析 toolInput 失败: {}", toolInput, e);
        }
        return null;
    }

    /**
     * 从 MCP Server 的 source_config 中读取用户对此行为的配置覆盖。
     * <p>
     * 配置格式（命名空间模式，参考 ShellTools.loadConfig()）：
     * <pre>{@code
     * {
     *   "built_in_clipboard": {
     *     "behaviors": {
     *       "write": "approval",
     *       "read": "auto"
     *     }
     *   }
     * }
     * }</pre>
     */
    @Nullable
    private SecurityLevel readUserOverride(ToolBehavior behavior) {
        if (mcpServer == null) return null;

        String sourceConfig = mcpServer.getSourceConfig();
        if (sourceConfig == null || sourceConfig.isBlank() || "{}".equals(sourceConfig)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(sourceConfig);
            String toolName = delegate.getToolDefinition().name();
            JsonNode toolNode = root.get(toolName);
            if (toolNode == null || toolNode.isNull()) return null;
            JsonNode behaviorsNode = toolNode.get("behaviors");
            if (behaviorsNode == null || behaviorsNode.isNull()) return null;
            JsonNode behaviorNode = behaviorsNode.get(behavior.getName());
            if (behaviorNode == null || behaviorNode.isNull()) return null;

            return parseSecurityLevel(behaviorNode.asText());
        } catch (JsonProcessingException e) {
            log.warn("[UnifiedChatInterceptor] 解析 source_config 失败: {}", e.getMessage());
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
    //  批准事件发射
    // ═══════════════════════════════════════════════

    private void emitPendingApproval(String toolName, String toolInput) {
        try {
            ChatToolExecutionEvent event = new ChatToolExecutionEvent();
            event.setStatus(ToolExecutionStatus.PENDING_APPROVAL);
            event.setToolName(toolName);
            event.setArguments(toolInput);
            event.setSeq(currentSeq);
            var result = sink.tryEmitNext(event);
            if (result.isFailure()) {
                log.warn("[UnifiedChatInterceptor] 发射 pending_approval 事件失败: tool={}, reason={}",
                        toolName, result);
            }
        } catch (Exception e) {
            log.warn("[UnifiedChatInterceptor] 发射 pending_approval 事件异常: tool={}", toolName, e);
        }
    }

    /**
     * 安全发射事件，失败仅打日志，不影响主流程。
     */
    private void emitSafe(ChatToolExecutionEvent event) {
        try {
            var result = sink.tryEmitNext(event);
            if (result.isFailure()) {
                log.debug("[UnifiedChatInterceptor] 发射工具事件失败: {} (reason={})",
                        event.getToolName(), result);
            }
        } catch (Exception e) {
            log.debug("[UnifiedChatInterceptor] 发射工具事件异常", e);
        }
    }

    // ═══════════════════════════════════════════════
    //  状态查询（供外层使用）
    // ═══════════════════════════════════════════════

    public boolean isRejected() {
        return rejected;
    }

    public int getCurrentSeq() {
        return currentSeq;
    }
}
