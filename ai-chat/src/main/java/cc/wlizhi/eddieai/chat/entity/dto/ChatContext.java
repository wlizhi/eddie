/**
 * ChatContext — 聊天请求上下文
 * <p>
 * 贯穿整个聊天调用链路的上下文对象，从请求预处理到响应后处理，
 * 每个阶段都可以往 Context 中写入数据，后续阶段可直接读取。
 * <p>
 * 所有扩展点（PreProcessor、ThinkingHandler、MetadataHandler 等）都通过此对象
 * 交换数据，避免方法参数膨胀和 ThreadLocal 的线程安全问题。
 */
package cc.wlizhi.eddieai.chat.entity.dto;

import cc.wlizhi.eddieai.chat.entity.AssistantEntity;
import cc.wlizhi.eddieai.chat.entity.SessionEntity;
import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ChatContext {

    // ==================== 阶段一：原始请求 ====================

    /** 用户原始请求 */
    private ChatRequest originalRequest;

    // ==================== 阶段二：预处理 & Provider ====================

    /**
     * 供应商实体
     */
    private ModelProviderEntity provider;

    /** 供应商代码 (openai / deepseek / anthropic ...) */
    private String providerCode;

    /** 最终请求的模型 ID（经过 ModelNameMapper 映射后） */
    private String resolvedModelId;

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

    // ==================== 阶段四：执行 & 响应 ====================

    /** 请求开始时间戳（毫秒） */
    private long startTime;

    /**
     * 最后一次 ChatResponse（用于提取 token 用量等元数据）
     */
    private ChatResponse lastResponse;

    /**
     * 完整思考内容（StringBuilder，流式拼接）
     */
    private StringBuilder fullThinking;

    /**
     * 完整回答内容（StringBuilder，流式拼接）
     */
    private StringBuilder fullAnswer;

    // ==================== 扩展属性 ====================

    /** 扩展属性 Map，供任意阶段写入临时数据 */
    private Map<String, Object> attributes = new HashMap<>();
}
