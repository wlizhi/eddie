/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.consts.AgentEvent;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.agent.service.AgentChatService;
import cc.wlizhi.eddie.common.cache.EventRegistry;
import cc.wlizhi.eddie.common.exception.BadRequestException;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

@Service
public class AgentChatServiceImpl implements AgentChatService {

    private static final Logger log = LoggerFactory.getLogger(AgentChatServiceImpl.class);

    @Resource
    private EventRegistry eventRegistry;

    @Resource
    private List<AgentChatPreProcessor> preProcessors;

    @Override
    public Flux<ServerSentEvent<String>> chat(AgentChatRequest request) {
        AgentChatContext ctx = new AgentChatContext();
        ctx.setStartTime(System.currentTimeMillis());
        ctx.setOriginalRequest(request);

        // 按 @Order 顺序执行所有预处理器，填充 AgentChatContext 字段
        for (AgentChatPreProcessor processor : preProcessors) {
            processor.process(ctx);
        }

        return Flux.empty();
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
