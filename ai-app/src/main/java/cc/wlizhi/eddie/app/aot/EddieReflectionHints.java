/**
 * @author Eddie
 * {@code @date} 2026-06-29
 */

package cc.wlizhi.eddie.app.aot;

import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.AgentMsgStepEntity;
import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import cc.wlizhi.eddie.agent.entity.request.AgentMcpServerBinding;
import cc.wlizhi.eddie.agent.entity.request.AgentToolBinding;
import cc.wlizhi.eddie.agent.entity.dto.AgentModelInfo;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskPlan;
import cc.wlizhi.eddie.agent.entity.dto.AgentTaskStep;
import cc.wlizhi.eddie.agent.entity.dto.AgentTokenStatists;
import cc.wlizhi.eddie.agent.entity.event.payload.*;
import cc.wlizhi.eddie.agent.entity.response.AgentSessionVO;
import cc.wlizhi.eddie.chat.controller.ChatToolApprovalController;
import cc.wlizhi.eddie.chat.entity.dto.MetadataInfo;
import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.chat.service.impl.DeepseekChatClientFactory;
import cc.wlizhi.eddie.chat.service.impl.OpenAiChatClientFactory;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.agent.enums.StepStatus;
import cc.wlizhi.eddie.common.agent.enums.TaskPlanStatus;
import cc.wlizhi.eddie.common.enums.ToolExecutionStatus;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatModel;
import cc.wlizhi.eddie.common.ai.openai.ModelParams;
import cc.wlizhi.eddie.common.dao.ChatModelProviderDao;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.dto.ConfigFieldDescriptor;
import cc.wlizhi.eddie.common.dto.ConfigSchema;
import cc.wlizhi.eddie.common.dto.NewlineStringToListDeserializer;
import cc.wlizhi.eddie.common.dto.ShellToolConfig;
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
        reflection.registerType(ChatToolApprovalController.ChatToolApprovalRequest.class, members);
        reflection.registerType(ToolExecutionStatus.class, members);
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
        reflection.registerType(AgentMode.class, members);
        reflection.registerType(TaskPlanStatus.class, members);
        reflection.registerType(StepStatus.class, members);
        reflection.registerType(AgentTaskPlan.class, members);
        reflection.registerType(ApiResult.class, members);
        reflection.registerType(ShellToolConfig.class, members);
        reflection.registerType(NewlineStringToListDeserializer.class, members);
        reflection.registerType(ConfigSchema.class, members);
        reflection.registerType(ConfigFieldDescriptor.class, members);
        reflection.registerType(cc.wlizhi.eddie.settings.entity.response.McpToolItemVO.class, members);
        reflection.registerType(AgentTaskStep.class, members);
        reflection.registerType(AgentMsgStepEntity.class, members);
        // ==================== OpenAI SDK 内部类（Jackson 反序列化需要） ====================
        reflection.registerType(
                com.openai.models.completions.CompletionUsage.CompletionTokensDetails.class,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);
        reflection.registerType(
                com.openai.models.completions.CompletionUsage.PromptTokensDetails.class,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);
        // ==================== SSE 事件 Payload ====================
        reflection.registerType(ThinkingPayload.class, members);
        reflection.registerType(AnswerPayload.class, members);
        reflection.registerType(ToolExecutionPayload.class, members);
        reflection.registerType(MessageCreatedPayload.class, members);
        reflection.registerType(RoundPayload.class, members);
        reflection.registerType(MetadataPayload.class, members);
        reflection.registerType(CancelledPayload.class, members);
        reflection.registerType(AgentTokenStatists.class, members);
    }
}
