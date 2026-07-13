/**
 * @author Eddie
 * {@code @date} 2026-07-13
 */

package cc.wlizhi.eddie.agent.handler.processor;

import cc.wlizhi.eddie.agent.entity.dto.AgentChatContext;
import cc.wlizhi.eddie.agent.handler.AgentChatPreProcessor;
import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import cc.wlizhi.eddie.common.util.ConfigUtil;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 工具结果截断长度配置预处理器
 * <p>
 * 在构建 ChatClient 之前，从 GlobalConfigContext 中读取工具结果在各层级的截断长度，
 * 并写入 {@link cc.wlizhi.eddie.agent.entity.dto.AgentOutputContext}，
 * 供后续流式处理器／工具拦截器使用。
 * <p>
 * 覆盖范围：
 * <ul>
 *   <li>toolResultModelMaxLength — 提交给 LLM 模型的数据截断</li>
 *   <li>toolCallMaxLength — SSE 推送前端的渲染截断</li>
 *   <li>toolCallStoreMaxLength — 持久化到数据库的存储截断（= SSE 截断的一半）</li>
 * </ul>
 */
@Slf4j
@Order(250)
@Component
public class ToolLengthConfigPreProcessor implements AgentChatPreProcessor {

    @Resource
    private GlobalConfigContext globalConfigContext;

    @Override
    public void process(AgentChatContext ctx) {
        // 1. 模型上下文截断（提交给 LLM 的数据）
        String modelMaxLenStr = globalConfigContext.getConfig(GlobalConfigKey.TOOL_RESULT_MODEL_MAX_LENGTH);
        int modelMaxLen = ConfigUtil.resolveIntConfig(100000, modelMaxLenStr, 0, 100000);
        ctx.getOutput().setToolResultModelMaxLength(modelMaxLen);

        // 2. SSE 渲染截断（推送前端展示）
        String sseMaxLenStr = globalConfigContext.getConfig(GlobalConfigKey.TOOL_CALL_MAX_LENGTH);
        int sseMaxLen = ConfigUtil.resolveIntConfig(5000, sseMaxLenStr, 100, 8000);
        ctx.getOutput().setToolCallMaxLength(sseMaxLen);

        // 3. 存储截断（持久化到数据库，固定为 SSE 截断的一半）
        ctx.getOutput().setToolCallStoreMaxLength(sseMaxLen >> 1);

        log.debug("工具结果截断配置: modelMaxLen={}, sseMaxLen={}, storeMaxLen={}",
                modelMaxLen, sseMaxLen, sseMaxLen >> 1);
    }
}
