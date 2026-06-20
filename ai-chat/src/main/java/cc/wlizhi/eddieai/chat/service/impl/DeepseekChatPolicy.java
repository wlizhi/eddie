package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatClientGetDTO;
import cc.wlizhi.eddieai.chat.service.ChatPolicy;
import cc.wlizhi.eddieai.common.entity.ModelProviderEntity;
import cc.wlizhi.eddieai.common.exception.BadRequestException;
import cc.wlizhi.eddieai.memory.context.ModelProviderContext;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.stereotype.Service;

import java.util.Objects;

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
        ModelProviderEntity modelProvider = modelProviderContext.getModelProvider(chatClientGetDTO.getProviderCode());
        if (modelProvider == null) {
            throw new BadRequestException(chatClientGetDTO.getProviderCode() + "不支持的模型服务商");
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

}
