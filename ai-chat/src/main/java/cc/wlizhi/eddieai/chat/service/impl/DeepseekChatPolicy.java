package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.dto.ChatClientGetDTO;
import cc.wlizhi.eddieai.chat.service.ChatPolicy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DeepseekChatPolicy implements ChatPolicy {

    @Override
    public boolean support(String providerCode) {
        return Objects.equals(providerCode, "deepseek");
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
