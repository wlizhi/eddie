package cc.wlizhi.eddieai.app.config;

import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天模型配置
 * <p>
 * 手动创建 DeepSeek ChatModel（兼容 OpenAI 协议），不依赖 Spring AI Starter。
 * 后续多模型场景可在此扩展为工厂模式，从数据库读取配置动态构建 ChatModel。
 */
@Configuration
public class ChatConfig {

    @Bean
    public OpenAiChatModel openAiChatModel(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.base-url}") String baseUrl,
            @Value("${deepseek.model}") String model,
            @Value("${deepseek.temperature}") Double temperature) {
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .model(model)
                        .temperature(temperature)
                        .build())
                .build();
    }

    @Bean
    public DeepSeekChatModel deepSeekChatModel(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.base-url}") String baseUrl,
            @Value("${deepseek.model}") String model,
            @Value("${deepseek.temperature}") Double temperature) {
        return DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .build())
                .options(DeepSeekChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build())
                .build();
    }

}
