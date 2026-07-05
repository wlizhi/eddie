package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.agent.service.AgentChatClientFactory;
import cc.wlizhi.eddie.agent.service.AgentChatClientFactoryRouter;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(300)
@Slf4j
@Component
public class AgentChatClientPreProcessor implements AgentChatPreProcessor {
    @Resource
    private AgentChatClientFactoryRouter factoryRouter;


    @Override
    public void process(AgentChatContext ctx) {
        ModelProviderEntity modelProvider = ctx.getModelProvider();
        AgentChatClientFactory factory = factoryRouter.resolve(modelProvider.getCode());
        ctx.setChatClient(factory.getChatClient(ctx));
    }
}
