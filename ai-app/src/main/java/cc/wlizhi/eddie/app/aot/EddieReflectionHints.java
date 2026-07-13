/**
 * @author Eddie
 * {@code @date} 2026-06-29
 */

package cc.wlizhi.eddie.app.aot;

import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.AgentMsgEntity;
import cc.wlizhi.eddie.agent.entity.AgentMsgStepEntity;
import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import cc.wlizhi.eddie.agent.entity.dto.*;
import cc.wlizhi.eddie.agent.entity.event.payload.*;
import cc.wlizhi.eddie.agent.entity.response.AgentSessionVO;
import cc.wlizhi.eddie.chat.controller.ChatToolApprovalController;
import cc.wlizhi.eddie.chat.entity.dto.CancelledPayload;
import cc.wlizhi.eddie.chat.entity.dto.ChatErrorPayload;
import cc.wlizhi.eddie.chat.entity.dto.ChatToolExecPayload;
import cc.wlizhi.eddie.chat.entity.dto.MessageCreatedPayload;
import cc.wlizhi.eddie.chat.entity.dto.MetadataInfo;
import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.common.agent.enums.AgentMode;
import cc.wlizhi.eddie.common.agent.enums.StepStatus;
import cc.wlizhi.eddie.common.agent.enums.TaskPlanStatus;
import cc.wlizhi.eddie.common.ai.openai.EddieOpenAiChatModel;
import cc.wlizhi.eddie.common.ai.openai.ModelParams;
import cc.wlizhi.eddie.common.dao.ChatModelProviderDao;
import cc.wlizhi.eddie.common.dto.*;
import cc.wlizhi.eddie.common.entity.GlobalConfigEntity;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.ToolDefinitionEntity;
import cc.wlizhi.eddie.common.entity.dto.GeneralSettings;
import cc.wlizhi.eddie.common.entity.dto.ModelJsonItem;
import cc.wlizhi.eddie.common.enums.ToolExecutionStatus;
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
        // ==================== 标准实体/DTO/工具类（INVOKE_PUBLIC_CONSTRUCTORS + INVOKE_PUBLIC_METHODS） ====================
        registerType(hints,
                GlobalConfigEntity.class,
                ModelProviderEntity.class,
                ChatModelProviderDao.EnabledProviderModel.class,
                McpServerEntity.class,
                ToolDefinitionEntity.class,
                MetadataInfo.class,
                ToolExecutionEvent.class,
                ChatToolExecPayload.class,
                MessageCreatedPayload.class,
                ChatErrorPayload.class,
                CancelledPayload.class,
                ChatToolApprovalController.ChatToolApprovalRequest.class,
                ToolExecutionStatus.class,
                McpClientHolder.class,
                McpToolCallback.class,
                EddieOpenAiChatModel.class,
                OwnerToolBindingContext.McpServerWithTools.class,
                ModelParams.class,
                AgentModelInfo.class,
                AgentEntity.class,
                AgentSessionEntity.class,
                AgentSessionVO.class,
                cc.wlizhi.eddie.agent.entity.response.AgentMessageVO.class,
                GeneralSettings.class,
                ModelJsonItem.class,
                AgentMode.class,
                TaskPlanStatus.class,
                StepStatus.class,
                AgentTaskPlan.class,
                ApiResult.class,
                ShellToolConfig.class,
                NewlineStringToListDeserializer.class,
                ConfigSchema.class,
                ConfigFieldDescriptor.class,
                cc.wlizhi.eddie.settings.entity.response.McpToolItemVO.class,
                AgentTaskStep.class,
                AgentMsgStepEntity.class,
                AgentMsgEntity.class,
                AgentIteratorState.class,
                // ==================== SSE 事件 Payload ====================
                ThinkingPayload.class,
                AnswerPayload.class,
                ToolExecutionPayload.class,
                MessageCreatedPayload.class,
                RoundPayload.class,
                MetadataPayload.class,
                CancelledPayload.class,
                AgentTokenStatists.class
        );
        // ==================== OpenAI SDK 内部类（Jackson 反序列化需要，使用 INVOKE_DECLARED_METHODS） ====================
        ReflectionHints reflection = hints.reflection();
        reflection.registerType(
                com.openai.models.completions.CompletionUsage.CompletionTokensDetails.class,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);
        reflection.registerType(
                com.openai.models.completions.CompletionUsage.PromptTokensDetails.class,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);
    }

    private void registerType(RuntimeHints hints, Class<?>... classArr) {
        var members = new MemberCategory[]{
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS
        };
        ReflectionHints reflection = hints.reflection();
        for (Class<?> clazz : classArr) {
            reflection.registerType(clazz, members);
        }
    }
}
