/**
 * ChatContext — 聊天请求上下文
 * <p>
 * 贯穿整个聊天调用链路的上下文对象，从请求预处理到响应后处理，
 * 每个阶段都可以往 Context 中写入数据，后续阶段可直接读取。
 * <p>
 * 所有扩展点（PreProcessor、ThinkingHandler、MetadataHandler 等）都通过此对象
 * 交换数据，避免方法参数膨胀和 ThreadLocal 的线程安全问题。
 */

/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.entity.dto;

import cc.wlizhi.eddie.chat.entity.request.ChatRequest;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.ModelPricing;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.SessionEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ChatContext {

    // ==================== 阶段一：原始请求 ====================

    /**
     * 用户原始请求
     */
    private ChatRequest originalRequest;

    // ==================== 阶段二：预处理 & Provider ====================

    /**
     * 供应商实体
     */
    private ModelProviderEntity provider;

    /**
     * 供应商代码 (openai / deepseek / anthropic ...)
     */
    private String providerCode;

    /**
     * 完整的助手实体（含 systemPrompt、modelId、modelParams、memoryRounds 等）
     */
    private AssistantEntity assistant;

    /**
     * 完整的会话实体（含 title、assistantId、pinned 等）
     */
    private SessionEntity session;

    /**
     * 用户消息内容
     */
    private String userMessage;

    // ==================== 阶段三：构建 & 执行 ====================

    /**
     * 构建好的 ChatClient
     */
    private ChatClient chatClient;

    // ==================== 阶段二.五：模型价格配置（预处理解析） ====================

    /**
     * 模型价格配置（含 inputPrice / outputPrice / cacheInputPrice / currency）
     */
    private ModelPricing pricing;

    // ==================== 阶段四：执行 & 响应 ====================

    /**
     * 请求开始时间戳（毫秒）
     */
    private long startTime;

    /**
     * 最后一次 ChatResponse（用于提取 token 用量等元数据）
     */
    private ChatResponse lastResponse;

    /**
     * 响应元数据（由 ChatMetadataHandler 构建，同时供 SSE 和持久化使用）
     */
    private MetadataInfo metadata;

    /**
     * 完整思考内容（StringBuilder，流式拼接）
     */
    private StringBuilder fullThinking;

    /**
     * 完整回答内容（StringBuilder，流式拼接）
     */
    private StringBuilder fullAnswer;

    /**
     * 工具执行记录列表（用于持久化到 ai_session_msg.tool_calls）
     */
    private List<ToolExecutionEvent> toolCalls = new ArrayList<>();

    /**
     * 是否被用户中断（手动取消或网络断开）
     */
    private boolean interrupted;

    /**
     * 用户消息 ID（持久化后获得，用于停止事件关联）
     */
    private Long userMessageId;

    /**
     * 占位 assistant 消息的 ID（流开始前插入，doFinally 中通过此 ID 更新内容）
     */
    private Long placeholderMsgId;

    // ==================== 会话锁 ====================

    /**
     * 会话锁 token（由 {@link cc.wlizhi.eddie.common.cache.SessionLockManager#tryLock} 返回的 nanoTime）
     * <p>
     * 0 表示未持有锁，在 {@code doFinally} 中传递给 {@code unlock} 用以原子比对释放。
     */
    private long lockNanoTime;

    // ==================== 扩展属性 ====================

    /**
     * 扩展属性 Map，供任意阶段写入临时数据
     */
    private Map<String, Object> attributes = new HashMap<>();
}
