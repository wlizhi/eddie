package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatClientGetDTO;
import cc.wlizhi.eddieai.chat.service.ChatPolicy;
import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import cc.wlizhi.eddieai.common.exception.BadRequestException;
import cc.wlizhi.eddieai.memory.context.ModelProviderContext;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Primary
@Service
public class OpenAiChatPolicy implements ChatPolicy {
    @Resource
    private OpenAiChatModel openAiChatModel;
    @Resource
    private ModelProviderContext modelProviderContext;

    @Override
    public boolean support(String providerCode) {
        return Objects.equals(providerCode, "openai");
    }

    @Override
    public ChatClient getChatClient(ChatClientGetDTO chatClientGetDTO) {
        ModelProviderEntity modelProvider = modelProviderContext.getModelProviderById(chatClientGetDTO.getProviderId());
        if (modelProvider == null) {
            throw new BadRequestException("providerId=" + chatClientGetDTO.getProviderId() + " 不存在的模型服务商");
        }
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .apiKey(modelProvider.getApiKey())
                        .baseUrl(modelProvider.getBaseUrl())
                        .model(chatClientGetDTO.getModelId())
                        .build())
                .build();
        return ChatClient.builder(openAiChatModel)
                .build();
    }

}
