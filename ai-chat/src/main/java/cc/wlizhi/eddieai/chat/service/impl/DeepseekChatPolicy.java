package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatClientGetDTO;
import cc.wlizhi.eddieai.chat.service.ChatPolicy;
import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import cc.wlizhi.eddieai.common.exception.BadRequestException;
import cc.wlizhi.eddieai.memory.context.ModelProviderContext;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DeepseekChatPolicy implements ChatPolicy {

    @Resource
    private DeepSeekChatModel deepSeekChatModel;
    @Resource
    private ModelProviderContext modelProviderContext;


    @Override
    public boolean support(String providerCode) {
        return Objects.equals(providerCode, "deepseek");
    }

    @Override
    public ChatClient getChatClient(ChatClientGetDTO chatClientGetDTO) {
        ModelProviderEntity modelProvider = modelProviderContext.getModelProviderById(chatClientGetDTO.getProviderId());
        if (modelProvider == null) {
            throw new BadRequestException("providerId=" + chatClientGetDTO.getProviderId() + " 不存在的模型服务商");
        }
        DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .apiKey(modelProvider.getApiKey())
                        .baseUrl(modelProvider.getBaseUrl())
                        .build())
                .options(DeepSeekChatOptions.builder()
                        .model(chatClientGetDTO.getModelId())
                        .build())
                .build();
        return ChatClient.builder(deepSeekChatModel)
                .build();
    }

    @Override
    public ServerSentEvent<String> getThinkEvent(ChatResponse chatResponse) {
        // 提取思考内容（reasoning_content）
        String reasoning = chatResponse.getResults().stream()
                .map(Generation::getOutput)
                .map(msg -> {
                    if (msg instanceof DeepSeekAssistantMessage deepSeekAssistantMessage) {
                        return deepSeekAssistantMessage.getReasoningContent();
                    }
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.joining());
        return ObjectUtils.isEmpty(reasoning) ? null : ServerSentEvent.<String>builder()
                .event("thinking")
                .data(reasoning)
                .build();
    }
}
