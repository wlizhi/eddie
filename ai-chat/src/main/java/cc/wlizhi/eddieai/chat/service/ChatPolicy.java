package cc.wlizhi.eddieai.chat.service;

import cc.wlizhi.eddieai.chat.entity.dto.ChatClientGetDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.codec.ServerSentEvent;

/**
 * 多服务商多模型聊天策略
 * 1. 根据用户选择的模型，选择对应的服务商
 * 2. 根据用户选择的模型，选择对应的模型
 * 3. 根据用户选择的模型，选择对应的模型参数
 */
public interface ChatPolicy {

    boolean support(String providerCode);

    ChatClient getChatClient(ChatClientGetDTO chatClientGetDTO);

    default ServerSentEvent<String> getThinkEvent(ChatResponse chatResponse) {
        return null;
    }
}
