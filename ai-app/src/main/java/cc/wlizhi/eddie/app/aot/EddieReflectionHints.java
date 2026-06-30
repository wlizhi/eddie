/**
 * @author Eddie
 * {@code @date} 2026-06-29
 */

package cc.wlizhi.eddie.app.aot;

import cc.wlizhi.eddie.chat.entity.dto.MetadataInfo;
import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.chat.service.impl.DeepseekChatClientFactory;
import cc.wlizhi.eddie.chat.service.impl.OpenAiChatClientFactory;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatModel;
import cc.wlizhi.eddie.common.dao.ChatModelProviderDao;
import cc.wlizhi.eddie.common.entity.GlobalConfigEntity;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
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
    }
}
