/**
 * @author Eddie
 * {@code @date} 2026-06-29
 */

package cc.wlizhi.eddie.app.aot;

import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import cc.wlizhi.eddie.agent.entity.dto.AgentModelInfo;
import cc.wlizhi.eddie.agent.entity.response.AgentSessionVO;
import cc.wlizhi.eddie.chat.entity.dto.MetadataInfo;
import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.chat.service.impl.DeepseekChatClientFactory;
import cc.wlizhi.eddie.chat.service.impl.OpenAiChatClientFactory;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatModel;
import cc.wlizhi.eddie.common.ai.openai.ModelParams;
import cc.wlizhi.eddie.common.dao.ChatModelProviderDao;
import cc.wlizhi.eddie.common.entity.GlobalConfigEntity;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.entity.dto.GeneralSettings;
import cc.wlizhi.eddie.common.entity.dto.ModelJsonItem;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import cc.wlizhi.eddie.tools.service.McpClientHolder;
import cc.wlizhi.eddie.tools.service.McpToolCallback;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class EddieReflectionHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        var members = new MemberCategory[]{
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS
        };
        ReflectionHints reflection = hints.reflection();
        reflection.registerType(GlobalConfigEntity.class, members);
        reflection.registerType(ModelProviderEntity.class, members);
        reflection.registerType(ChatModelProviderDao.EnabledProviderModel.class, members);
        reflection.registerType(McpServerEntity.class, members);
        reflection.registerType(ToolDefinitionEntity.class, members);
        reflection.registerType(MetadataInfo.class, members);
        reflection.registerType(ToolExecutionEvent.class, members);
        reflection.registerType(DeepseekChatClientFactory.class, members);
        reflection.registerType(OpenAiChatClientFactory.class, members);
        reflection.registerType(McpClientHolder.class, members);
        reflection.registerType(McpToolCallback.class, members);
        reflection.registerType(EddieOpenAiChatModel.class, members);
        reflection.registerType(OwnerToolBindingContext.McpServerWithTools.class, members);
        reflection.registerType(ModelParams.class, members);
        reflection.registerType(AgentModelInfo.class, members);
        reflection.registerType(AgentEntity.class, members);
        reflection.registerType(AgentSessionEntity.class, members);
        reflection.registerType(AgentSessionVO.class, members);
        reflection.registerType(cc.wlizhi.eddie.agent.entity.response.AgentMessageVO.class, members);
        reflection.registerType(GeneralSettings.class, members);
        reflection.registerType(ModelJsonItem.class, members);
    }
}
