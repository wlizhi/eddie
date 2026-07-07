/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.service.impl;

import cc.wlizhi.eddie.agent.dao.AgentDao;
import cc.wlizhi.eddie.agent.dao.AgentMsgDao;
import cc.wlizhi.eddie.agent.dao.AgentSessionDao;
import cc.wlizhi.eddie.agent.entity.AgentEntity;
import cc.wlizhi.eddie.agent.entity.AgentMsgEntity;
import cc.wlizhi.eddie.agent.entity.AgentSessionEntity;
import cc.wlizhi.eddie.agent.entity.response.AgentMessageVO;
import cc.wlizhi.eddie.agent.entity.response.AgentSessionVO;
import cc.wlizhi.eddie.agent.service.AgentSessionService;
import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.request.ChatRequest;
import cc.wlizhi.eddie.chat.service.ChatClientFactory;
import cc.wlizhi.eddie.chat.service.ChatClientFactoryRouter;
import cc.wlizhi.eddie.common.dto.PageResult;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.dto.GeneralSettings;
import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import cc.wlizhi.eddie.memory.context.BuiltInPromptsContext;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能体会话管理业务实现
 */
@Service
public class AgentSessionServiceImpl implements AgentSessionService {

    private static final Logger log = LoggerFactory.getLogger(AgentSessionServiceImpl.class);
    private static final int TITLE_MAX_LENGTH = 20;

    @Resource
    private AgentSessionDao agentSessionDao;

    @Resource
    private AgentMsgDao agentMsgDao;

    @Resource
    private AgentDao agentDao;

    @Resource
    private GlobalConfigContext globalConfigContext;

    @Resource
    private BuiltInPromptsContext builtInPromptsContext;

    @Resource
    private ChatClientFactoryRouter chatClientFactoryRouter;

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public AgentSessionVO create(Long agentId) {
        AgentSessionEntity entity = new AgentSessionEntity();
        entity.setAgentId(agentId);
        agentSessionDao.insert(entity);
        Long id = agentSessionDao.findLastInsertId();
        AgentSessionEntity saved = agentSessionDao.findById(id);
        return toVO(saved);
    }

    @Override
    public PageResult<AgentSessionVO> list(Long agentId, String title, int pageNum, int pageSize) {
        long total = agentSessionDao.countByAgentId(agentId, title);
        if (total == 0) {
            return PageResult.empty();
        }
        int offset = (pageNum - 1) * pageSize;
        List<AgentSessionEntity> entities = agentSessionDao.findByAgentIdPaged(agentId, title, offset, pageSize);

        List<AgentSessionVO> vos = new ArrayList<>();
        for (AgentSessionEntity entity : entities) {
            vos.add(toVO(entity));
        }
        return PageResult.of(pageNum, pageSize, total, vos);
    }

    @Override
    public void delete(Long id) {
        if (!agentSessionDao.existsById(id)) {
            throw new NotFoundException("智能体会话不存在: " + id);
        }
        agentMsgDao.deleteBySessionId(id);
        agentSessionDao.deleteById(id);
    }

    @Override
    public AgentSessionVO renameTitle(Long id, String title) {
        if (!agentSessionDao.existsById(id)) {
            throw new NotFoundException("智能体会话不存在: " + id);
        }
        agentSessionDao.updateTitle(id, title);
        return toVO(agentSessionDao.findById(id));
    }

    @Override
    public String generateTitle(Long sessionId) {
        AgentSessionEntity session = agentSessionDao.findById(sessionId);
        if (session == null) {
            throw new NotFoundException("智能体会话不存在: " + sessionId);
        }

        // 读取配置：生成标题取前几轮对话（默认 1 轮）
        GeneralSettings generalSettings = globalConfigContext.getGeneralSettings();
        int rounds = Math.max(generalSettings.getTitleGenerationRounds(), 1);

        List<AgentMsgEntity> messages = agentMsgDao.findRounds(sessionId, rounds);
        if (messages.size() < 2) {
            // 消息不足，返回空字符串
            agentSessionDao.updateTitle(sessionId, "");
            return "";
        }

        // 按角色提取首轮 user 消息（用于最终降级截取）
        String firstUserMsg = "";
        // 构建多轮对话文本
        StringBuilder conversationBuilder = new StringBuilder();
        int roundNum = 0;
        int userCount = 0;
        for (AgentMsgEntity msg : messages) {
            if ("user".equals(msg.getRole())) {
                userCount++;
                roundNum = userCount;
                if (userCount == 1) {
                    firstUserMsg = msg.getContent();
                }
                conversationBuilder.append("第").append(roundNum).append("轮\n用户：").append(msg.getContent()).append("\n");
            } else if ("assistant".equals(msg.getRole())) {
                conversationBuilder.append("智能体：").append(msg.getContent()).append("\n");
            }
        }

        String conversation = conversationBuilder.toString().trim();

        // 降级链：FAST_MODEL → DEFAULT_MODEL → 智能体绑定模型 → 截取前 20 字
        String title = tryGenerateWithAi(conversation, session);
        if (title != null && !title.isBlank()) {
            agentSessionDao.updateTitle(sessionId, title);
            return title;
        }

        // 最终降级：截取首条消息前 20 字
        title = firstUserMsg.length() > TITLE_MAX_LENGTH ? firstUserMsg.substring(0, TITLE_MAX_LENGTH) : firstUserMsg;
        agentSessionDao.updateTitle(sessionId, title);
        return title;
    }

    /**
     * 逐级尝试用 AI 模型生成标题
     */
    private String tryGenerateWithAi(String conversation, AgentSessionEntity session) {
        // 降级链 1: FAST_MODEL
        String fastModelJson = globalConfigContext.getConfig(GlobalConfigKey.FAST_MODEL);
        if (fastModelJson != null) {
            try {
                JsonNode node = objectMapper.readTree(fastModelJson);
                String title = callModel(node, conversation);
                if (title != null) return title;
            } catch (Exception e) {
                log.warn("FAST_MODEL 调用失败", e);
            }
        }

        // 降级链 2: DEFAULT_MODEL
        String defaultModelJson = globalConfigContext.getConfig(GlobalConfigKey.DEFAULT_MODEL);
        if (defaultModelJson != null) {
            try {
                JsonNode node = objectMapper.readTree(defaultModelJson);
                String title = callModel(node, conversation);
                if (title != null) return title;
            } catch (Exception e) {
                log.warn("DEFAULT_MODEL 调用失败", e);
            }
        }

        // 降级链 3: 智能体绑定模型
        if (session.getAgentId() != null) {
            AgentEntity agent = agentDao.findById(session.getAgentId());
            if (agent != null && agent.getMainProviderId() != null && agent.getMainModelId() != null) {
                String title = callModel(agent.getMainProviderId(), agent.getMainModelId(), conversation);
                if (title != null) return title;
            }
        }

        return null;
    }

    /**
     * 从 JSON 节点解析 providerId + modelCode 并调用模型
     */
    private String callModel(JsonNode node, String conversation) {
        Long providerId = node.get("providerId") != null ? node.get("providerId").asLong() : null;
        String modelId = node.get("modelId") != null ? node.get("modelId").asText() : null;
        if (providerId == null || modelId == null) {
            return null;
        }
        return callModel(providerId, modelId, conversation);
    }

    /**
     * 使用指定 provider + model 调用 AI 生成标题
     */
    private String callModel(Long providerId, String modelCode, String conversation) {
        ModelProviderEntity provider = modelProviderContext.getModelProviderById(providerId);
        if (provider == null) {
            log.warn("生成标题时未找到 provider: {}", providerId);
            return null;
        }

        // 加载并解析 prompt 模板
        String promptTemplate = builtInPromptsContext.getSessionTitlePrompts();
        if (promptTemplate == null) {
            log.warn("标题生成 prompt 未加载，跳过");
            return null;
        }

        String prompt = builtInPromptsContext.resolvePrompt(promptTemplate, Map.of(
                "conversation", conversation != null ? conversation : ""
        ));

        // 构建最小 ChatContext 用于获取 ChatClient
        ChatContext ctx = new ChatContext();
        ctx.setProvider(provider);
        ctx.setProviderCode(provider.getCode());
        ChatRequest request = new ChatRequest();
        request.setModelId(modelCode);
        ctx.setOriginalRequest(request);
        AssistantEntity assistant = new AssistantEntity();
        assistant.setModelParams(null);
        ctx.setAssistant(assistant);

        try {
            ChatClientFactory factory = chatClientFactoryRouter.resolve(provider.getCode());
            ChatClient chatClient = factory.getChatClient(ctx);
            String result = chatClient.prompt(prompt)
                    .advisors(a -> a
                            .param("providerId", providerId)
                            .param("modelCode", modelCode))
                    .call().content();
            return result != null ? result.trim() : null;
        } catch (Exception e) {
            log.warn("调用模型(providerId={}, modelCode={})生成标题失败: {}", providerId, modelCode, e.getMessage());
            return null;
        }
    }

    @Override
    public void pin(Long id) {
        if (!agentSessionDao.existsById(id)) {
            throw new NotFoundException("智能体会话不存在: " + id);
        }
        agentSessionDao.updatePinned(id, 1);
    }

    @Override
    public void unpin(Long id) {
        if (!agentSessionDao.existsById(id)) {
            throw new NotFoundException("智能体会话不存在: " + id);
        }
        agentSessionDao.updatePinned(id, 0);
    }

    @Override
    public List<AgentMessageVO> findMessagesBySessionId(Long sessionId, Long beforeId, int limit) {
        List<AgentMsgEntity> entities = agentMsgDao.findBySessionId(sessionId, beforeId, limit);
        // AgentMsgDao 返回倒序（最新在前），反转为正序
        List<AgentMessageVO> vos = new ArrayList<>(entities.size());
        for (int i = entities.size() - 1; i >= 0; i--) {
            vos.add(toMessageVO(entities.get(i)));
        }
        return vos;
    }

    // ==================== 内部方法 ====================

    private AgentSessionVO toVO(AgentSessionEntity entity) {
        AgentSessionVO vo = new AgentSessionVO();
        vo.setId(entity.getId());
        vo.setAgentId(entity.getAgentId());
        vo.setTitle(entity.getTitle());
        vo.setPinned(entity.getPinned());
        vo.setMessageCount(entity.getMessageCount() != null ? entity.getMessageCount() : 0);
        vo.setTotalTokens(entity.getTotalTokens() != null ? entity.getTotalTokens() : 0);
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AgentMessageVO toMessageVO(AgentMsgEntity entity) {
        AgentMessageVO vo = new AgentMessageVO();
        vo.setId(entity.getId());
        vo.setSessionId(entity.getSessionId());
        vo.setAgentId(entity.getAgentId());
        vo.setRole(entity.getRole());
        vo.setProviderId(entity.getProviderId());
        vo.setModelCode(entity.getModelCode());
        vo.setModelName(entity.getModelName());
        vo.setThinking(entity.getThinking());
        vo.setContent(entity.getContent());
        vo.setPromptTokens(entity.getPromptTokens());
        vo.setCompletionTokens(entity.getCompletionTokens());
        vo.setTotalTokens(entity.getTotalTokens());
        vo.setPriceEstimate(entity.getPriceEstimate());
        vo.setCurrency(entity.getCurrency());
        vo.setToolCalls(entity.getToolCalls());
        vo.setCacheReadInputTokens(entity.getCacheReadInputTokens());
        vo.setCacheWriteInputTokens(entity.getCacheWriteInputTokens());
        vo.setDurationMs(entity.getDurationMs());
        vo.setMsgStatus(entity.getMsgStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
