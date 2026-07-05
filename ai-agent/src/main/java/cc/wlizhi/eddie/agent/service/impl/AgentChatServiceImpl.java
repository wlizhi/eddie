/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.agent.handler.AgentPromptsResolver;
import cc.wlizhi.eddie.agent.service.AgentChatService;
import cc.wlizhi.eddie.chat.advisor.ModelThrottleAdvisor;
import cc.wlizhi.eddie.common.agent.enums.AgentEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.enums.RoleType;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import cc.wlizhi.eddie.tools.service.ToolCallbackResolver;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class AgentChatServiceImpl implements AgentChatService {

    private static final Logger log = LoggerFactory.getLogger(AgentChatServiceImpl.class);

    @Resource
    private EventRegistry eventRegistry;
    @Resource
    private List<AgentChatPreProcessor> preProcessors;
    @Resource
    private ModelThrottleAdvisor modelThrottleAdvisor;
    @Resource
    private AgentPromptsResolver promptsResolver;
    @Resource
    private ToolCallbackResolver toolCallbackResolver;

    @Override
    public Flux<ServerSentEvent<String>> chat(AgentChatRequest request) {
        AgentChatContext ctx = new AgentChatContext();
        ctx.setStartTime(System.currentTimeMillis());
        ctx.setOriginalRequest(request);
        ctx.setAgentMode(AgentMode.CHAT);

        // 按 @Order 顺序执行所有预处理器，填充 AgentChatContext 字段
        preProcessors(ctx);

        return Flux.create(sink -> {
            ctx.setSink(sink);
            Thread agentThread = Thread.ofVirtual().name("demo-agent").start(() -> {
                doChat(ctx);
            });
            // 客户端断连时中断虚拟线程
            sink.onDispose(agentThread::interrupt);
        });
    }

    private void doChat(AgentChatContext ctx) {
        int maxIterations = ctx.getAgent().getMaxIterations();
        maxIterations = Math.clamp(maxIterations, 1, 1000);
        int currentRound = 0;
        FluxSink<ServerSentEvent<String>> sink = ctx.getSink();
        while (currentRound++ < maxIterations) {
            ChatClient client = resolveChatClient(ctx);
            // TODO 先写死提示词测试一下
            Stream<ChatResponse> stream = client.prompt()
                    .system(promptsResolver.resolvePrompts(ctx))
                    .user(ctx.getOriginalRequest().getMessage())
                    .advisors(advisor -> advisor
                            .param("chat_memory_conversation_id", ctx.getOriginalRequest().getSessionId())
                            .param("providerId", ctx.getModelProvider().getId())
                            .param("modelCode", ctx.getOriginalRequest().getModelId()))
                    .stream()
                    .chatResponse().toStream();

            stream.forEach(res -> {

            });

            // 如果需要则切换模式
            switchModeIfNecessary(ctx);
            // 会话结束还处于聊天模式，则退出循环
            if (ctx.getAgentMode() == AgentMode.CHAT) {
                break;
            }
        }


        AgentChatRequest chatRequest = ctx.getOriginalRequest();
        String toolMode = chatRequest.getToolSelectionMode();
        if (toolMode == null) {
            toolMode = ctx.getAgent().getToolSelectionMode();
        }


    }

    private void switchModeIfNecessary(AgentChatContext ctx) {
        Long reqMsgId = ctx.getOriginalRequest().getMsgId();
        Long agentMsgId = ctx.getAgentMsg().getId();
        String key = EventRegistry.key(AgentEvent.class.getSimpleName(), reqMsgId != null ? reqMsgId.toString() : agentMsgId.toString());
        Object mode = eventRegistry.get(key);
        if (mode instanceof AgentMode agentMode && agentMode == AgentMode.PLAN) {
            ctx.setAgentMode(AgentMode.PLAN);
        }
    }

    private ChatClient resolveChatClient(AgentChatContext ctx) {
        ToolCallback[] toolCallbacks = toolCallbackResolver.resolve(
                RoleType.ASSISTANT.name(), ctx.getAgent().getId()
                , ctx.getOriginalRequest().getToolSelectionMode()
                , ctx.getOriginalRequest().getToolNames());
        // TODO 刚发起聊天时需要使用记忆窗口，根据配置设定窗口大小。（模型限流已在 preProcessor 中设置，这里不必设置）
        return ctx.getChatClient().mutate()
                .defaultTools((Object) toolCallbacks)
                .build();
    }

    private void preProcessors(AgentChatContext ctx) {
        for (AgentChatPreProcessor processor : preProcessors) {
            processor.process(ctx);
        }
        ChatClient newClient = ctx.getChatClient().mutate().defaultAdvisors(modelThrottleAdvisor).build();
        ctx.setChatClient(newClient);
    }

    @Override
    public void stop(Long messageId, String mode) {
        if (!Objects.equals(AgentEvent.STOP_MSG.name(), mode) && !Objects.equals(AgentEvent.FORCE_STOP_MSG.name(), mode)) {
            throw new BadRequestException("事件类型不正确，支持的事件名称：[" + AgentEvent.STOP_MSG.name()
                    + ", " + AgentEvent.FORCE_STOP_MSG.name() + "]");
        }
        log.info("用户发送聊天中止指令：{}, Agent 任务: messageId={}", mode, messageId);
        eventRegistry.register(AgentEvent.class.getSimpleName(), messageId.toString(), null);
    }
}
