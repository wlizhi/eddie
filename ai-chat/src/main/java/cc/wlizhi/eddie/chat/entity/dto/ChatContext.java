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



@Getter
@Setter
public class ChatContext {

    // ==================== 阶段一：原始请求 ====================

    /** 用户原始请求 */
    private ChatRequest originalRequest;

    // ==================== 阶段二：预处理 & Provider ====================

    /** 供应商实体 */
    private ModelProviderEntity provider;

    /** 完整助手实体（含 systemPrompt、modelId、modelParams、memoryRounds） */
    private AssistantEntity assistant;

    /** 完整会话实体（含 title、assistantId、pinned） */
    private SessionEntity session;

    /** 用户消息上下文（预处理阶段填充 content，持久化后回填 msgId） */
    private final ChatUserMsgContext userMsgContext = new ChatUserMsgContext();

    // ==================== 阶段二·五：模型价格配置（预处理解析） ====================

    /** 模型价格配置（预处理阶段解析，用于后续 token 费用估算） */
    private ModelPricing pricing;

    // ==================== 阶段三：消息持久化 & 响应构建 ====================

    /**
     * Assistant 消息上下文（持久化阶段写入 assistantMsgId，
     * 执行阶段流式构建 thinking/answer，完成后回填 metadata/toolCalls）
     */
    private final ChatAssistantMsgContext assistantMsgContext = new ChatAssistantMsgContext();

    // ==================== 阶段四：构建 & 执行 ====================

    /** 工厂路由 + 注入 Advisor/工具 后构建的 ChatClient */
    private ChatClient chatClient;

    // ==================== 阶段五：运行时流转状态 ====================

    /** 运行时流转状态（startTime、interrupted、lockNanoTime、toolCallSequence、cancelMode） */
    private final ChatFlowState flowState = new ChatFlowState();
}
