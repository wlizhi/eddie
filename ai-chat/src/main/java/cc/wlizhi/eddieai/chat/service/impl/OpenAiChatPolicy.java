package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatClientGetDTO;
import cc.wlizhi.eddieai.chat.service.ChatPolicy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Primary
@Service
public class OpenAiChatPolicy implements ChatPolicy {
    @Override
    public boolean support(String providerCode) {
        return Objects.equals(providerCode, "openai");
    }

    @Override
    public ChatClient getChatClient(ChatClientGetDTO chatClientGetDTO) {
        return null;
    }

    @Override
    public ChatClient.ChatClientRequestSpec getChatClientRequestSpec(ChatClientGetDTO chatClientGetDTO) {
        return null;
    }
}
