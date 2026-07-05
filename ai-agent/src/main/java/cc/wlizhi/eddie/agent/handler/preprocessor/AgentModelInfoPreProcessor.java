/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.handler.preprocessor;

import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.entity.dto.AgentModelInfo;
import cc.wlizhi.eddie.agent.entity.request.AgentChatRequest;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.agent.mapper.AgentModelInfoMapper;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.dto.ModelJsonItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * 模型信息预处理器 — 填充 {@link AgentChatContext#useModelInfo}
 * <p>
 * 从 {@link ModelProviderEntity#getModels()} JSON 中解析模型列表，
 * 匹配最终使用的模型 ID（请求级覆盖 ＞ Agent 配置），
 * 通过 {@link AgentModelInfoMapper} 将 {@link ModelJsonItem} 映射为 {@link AgentModelInfo}。
 * <p>
 * 未匹配到模型时不阻断流程，使用模型 ID 构建最小 {@link AgentModelInfo}。
 */
@Component
@Order(4)
public class AgentModelInfoPreProcessor implements AgentChatPreProcessor {

    private static final Logger log = LoggerFactory.getLogger(AgentModelInfoPreProcessor.class);

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private AgentModelInfoMapper agentModelInfoMapper;

    @Override
    public void process(AgentChatContext ctx) {
        AgentChatRequest request = ctx.getOriginalRequest();
        AgentEntity agent = ctx.getAgent();
        ModelProviderEntity provider = ctx.getModelProvider();

        // 确定最终模型 ID：请求级覆盖 ＞ Agent 配置
        String modelId = request.getModelId() != null
                ? request.getModelId()
                : agent.getMainModelId();

        if (modelId == null) {
            log.warn("未配置模型 ID，agentId={}, providerId={}", agent.getId(), provider.getId());
            return;
        }

        // 从 provider.models JSON 中匹配模型信息
        String modelsJson = provider.getModels();
        if (ObjectUtils.isEmpty(modelsJson) || "[]".equals(modelsJson)) {
            log.warn("服务商 models 配置为空，providerId={}, modelId={}", provider.getId(), modelId);
            ctx.setUseModelInfo(buildMinimalModelInfo(modelId));
            return;
        }

        try {
            List<ModelJsonItem> items = objectMapper.readValue(
                    modelsJson, new TypeReference<List<ModelJsonItem>>() {
                    });

            items.stream()
                    .filter(item -> modelId.equals(item.getId()))
                    .findFirst()
                    .ifPresentOrElse(
                            item -> ctx.setUseModelInfo(agentModelInfoMapper.toAgentModelInfo(item)),
                            () -> {
                                log.warn("未在 provider.models 中匹配到模型 providerId={}, modelId={}",
                                        provider.getId(), modelId);
                                ctx.setUseModelInfo(buildMinimalModelInfo(modelId));
                            }
                    );
        } catch (Exception e) {
            log.warn("解析 provider.models JSON 失败，providerId={}, modelId={}",
                    provider.getId(), modelId, e);
            ctx.setUseModelInfo(buildMinimalModelInfo(modelId));
        }
    }

    /**
     * 构建最小 {@link AgentModelInfo}，仅包含模型 ID
     */
    private static AgentModelInfo buildMinimalModelInfo(String modelId) {
        AgentModelInfo info = new AgentModelInfo();
        info.setId(modelId);
        info.setName(modelId);
        return info;
    }
}
