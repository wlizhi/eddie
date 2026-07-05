package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.service.AgentChatClientFactory;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatModel;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatOptions;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiOptionsHelper;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class EddieOpenAiAgentClientFactory implements AgentChatClientFactory {
    @Resource
    private EddieOpenAiOptionsHelper optionsHelper;

    @Override
    public boolean support(String providerCode) {
        return optionsHelper.supports(providerCode);
    }

    @Override
    public ChatClient getChatClient(AgentChatContext ctx) {
        ModelProviderEntity provider = ctx.getModelProvider();
        AgentEntity agent = ctx.getAgent();

        // OkHttp 默认 read timeout 为 60 秒，长思考/长回答模型可能触发
        // AbstractOpenAiOptions.DEFAULT_TIMEOUT，导致 stream was reset: CANCEL。
        // 设 10 分钟确保完整输出。
        // TODO 这个超时事件后面改为可配置或用户页面设置，后端请求超时应当给出友好提示。
        EddieOpenAiChatOptions.Builder optionsBuilder = EddieOpenAiChatOptions.builder()
                .apiKey(provider.getApiKey())
                .baseUrl(provider.getBaseUrl())
                .model(ctx.getUseModelInfo().getId())
                .timeout(Duration.ofMinutes(30));

        // 应用助手级 modelParams
        optionsHelper.applyModelParams(optionsBuilder, agent.getMainModelParams(), provider.getCode());

        // 请求级 thinkingMode 覆盖助手配置
        String requestThinkingMode = ctx.getOriginalRequest().getThinkingMode();
        if (requestThinkingMode != null && !requestThinkingMode.isBlank()) {
            optionsHelper.applyThinkingMode(optionsBuilder, requestThinkingMode, provider.getCode());
        }

        EddieOpenAiChatModel chatModel = EddieOpenAiChatModel.builder()
                .options(optionsBuilder.build())
                .build();

        return ChatClient.builder(chatModel).build();
    }
}
