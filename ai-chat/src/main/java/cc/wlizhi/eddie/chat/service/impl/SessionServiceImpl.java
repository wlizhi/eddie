package cc.wlizhi.eddie.chat.service.impl;

import cc.wlizhi.eddie.chat.context.AssistantContext;
import cc.wlizhi.eddie.chat.entity.dto.ChatContext;
import cc.wlizhi.eddie.chat.entity.dto.ToolExecutionEvent;
import cc.wlizhi.eddie.chat.entity.request.ChatRequest;
import cc.wlizhi.eddie.chat.entity.response.MessageVO;
import cc.wlizhi.eddie.chat.entity.response.SessionVO;
import cc.wlizhi.eddie.chat.service.ChatClientFactory;
import cc.wlizhi.eddie.chat.service.ChatClientFactoryRouter;
import cc.wlizhi.eddie.chat.service.SessionService;
import cc.wlizhi.eddie.common.dao.MessageDao;
import cc.wlizhi.eddie.common.dao.SessionDao;
import cc.wlizhi.eddie.common.dto.PageResult;
import cc.wlizhi.eddie.common.entity.AssistantEntity;
import cc.wlizhi.eddie.common.entity.MessageEntity;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.common.entity.SessionEntity;
import cc.wlizhi.eddie.common.enums.GlobalConfigKey;
import cc.wlizhi.eddie.common.exception.NotFoundException;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话管理业务实现
 */
@Service
public class SessionServiceImpl implements SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int TITLE_MAX_LENGTH = 20;

    @Resource
    private SessionDao sessionDao;

    @Resource
    private MessageDao messageDao;

    @Resource
    private GlobalConfigContext globalConfigContext;

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private AssistantContext assistantContext;

    @Resource
    private ChatClientFactoryRouter chatClientFactoryRouter;

    @Resource
    private ResourceLoader resourceLoader;

    private static final int MESSAGE_PAGE_SIZE = 20;

    @Override
    public SessionVO create(Long assistantId) {
        SessionEntity entity = new SessionEntity();
        entity.setAssistantId(assistantId);
        sessionDao.insert(entity);
        Long id = sessionDao.findLastInsertId();
        SessionEntity saved = sessionDao.findById(id);
        return toVO(saved);
    }

    @Override
    public PageResult<SessionVO> list(Long assistantId, String title, int pageNum, int pageSize) {
        long total = sessionDao.countByAssistantId(assistantId, title);
        int offset = (pageNum - 1) * pageSize;
        List<SessionEntity> entities = sessionDao.findByAssistantIdPaged(assistantId, title, offset, pageSize);

        List<SessionVO> vos = new ArrayList<>();
        for (SessionEntity entity : entities) {
            vos.add(toVO(entity));
        }
        return PageResult.of(pageNum, pageSize, total, vos);
    }

    @Override
    public void delete(Long id) {
        if (!sessionDao.existsById(id)) {
            throw new NotFoundException("会话不存在: " + id);
        }
        messageDao.deleteBySessionId(id);
        sessionDao.deleteById(id);
    }

    @Override
    public SessionVO renameTitle(Long id, String title) {
        if (!sessionDao.existsById(id)) {
            throw new NotFoundException("会话不存在: " + id);
        }
        sessionDao.updateTitle(id, title);
        return toVO(sessionDao.findById(id));
    }

    @Override
    public void pin(Long id) {
        if (!sessionDao.existsById(id)) {
            throw new NotFoundException("会话不存在: " + id);
        }
        sessionDao.updatePinned(id, 1);
    }

    @Override
    public void unpin(Long id) {
        if (!sessionDao.existsById(id)) {
            throw new NotFoundException("会话不存在: " + id);
        }
        sessionDao.updatePinned(id, 0);
    }

    @Override
    public String generateTitle(Long sessionId) {
        SessionEntity session = sessionDao.findById(sessionId);
        if (session == null) {
            throw new NotFoundException("会话不存在: " + sessionId);
        }

        List<MessageEntity> firstRound = messageDao.findFirstRound(sessionId);
        if (firstRound.size() < 2) {
            // 消息不足，返回空字符串
            sessionDao.updateTitle(sessionId, "");
            return "";
        }

        String userMsg = "";
        String assistantMsg = "";
        for (MessageEntity msg : firstRound) {
            if ("user".equals(msg.getRole())) {
                userMsg = msg.getContent();
            } else if ("assistant".equals(msg.getRole())) {
                assistantMsg = msg.getContent();
            }
        }

        // 降级链：FAST_MODEL → DEFAULT_MODEL → 助手绑定模型 → 截取前 20 字
        String title = tryGenerateWithAi(userMsg, assistantMsg, session);
        if (title != null && !title.isBlank()) {
            sessionDao.updateTitle(sessionId, title);
            return title;
        }

        // 最终降级：截取首条消息前 20 字
        title = userMsg.length() > TITLE_MAX_LENGTH ? userMsg.substring(0, TITLE_MAX_LENGTH) : userMsg;
        sessionDao.updateTitle(sessionId, title);
        return title;
    }

    /**
     * 逐级尝试用 AI 模型生成标题
     */
    private String tryGenerateWithAi(String userMsg, String assistantMsg, SessionEntity session) {
        // 降级链 1: FAST_MODEL
        String fastModelJson = globalConfigContext.getConfig(GlobalConfigKey.FAST_MODEL);
        if (fastModelJson != null) {
            try {
                JsonNode node = objectMapper.readTree(fastModelJson);
                String title = callModel(node, userMsg, assistantMsg);
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
                String title = callModel(node, userMsg, assistantMsg);
                if (title != null) return title;
            } catch (Exception e) {
                log.warn("DEFAULT_MODEL 调用失败", e);
            }
        }

        // 降级链 3: 助手绑定模型
        if (session.getAssistantId() != null) {
            AssistantEntity assistant = assistantContext.getAssistantById(session.getAssistantId());
            if (assistant != null && assistant.getProviderId() != null && assistant.getModelId() != null) {
                String title = callModel(assistant.getProviderId(), assistant.getModelId(), userMsg, assistantMsg);
                if (title != null) return title;
            }
        }

        return null;
    }

    /**
     * 从 JSON 节点解析 providerId + modelCode 并调用模型
     */
    private String callModel(JsonNode node, String userMsg, String assistantMsg) {
        Long providerId = node.get("providerId") != null ? node.get("providerId").asLong() : null;
        String modelCode = node.get("modelCode") != null ? node.get("modelCode").asText() : null;
        if (providerId == null || modelCode == null) {
            return null;
        }
        return callModel(providerId, modelCode, userMsg, assistantMsg);
    }

    /**
     * 使用指定 provider + model 调用 AI 生成标题
     */
    private String callModel(Long providerId, String modelCode, String userMsg, String assistantMsg) {
        ModelProviderEntity provider = modelProviderContext.getModelProviderById(providerId);
        if (provider == null) {
            log.warn("生成标题时未找到 provider: {}", providerId);
            return null;
        }

        // 加载 prompt 模板
        String prompt;
        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:prompts/title-generation.md");
            prompt = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("加载标题生成 prompt 文件失败", e);
            return null;
        }

        prompt = prompt.replace("{userMessage}", userMsg)
                .replace("{assistantMessage}", assistantMsg != null ? assistantMsg : "");

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
            String result = chatClient.prompt(prompt).call().content();
            return result != null ? result.trim() : null;
        } catch (Exception e) {
            log.warn("调用模型(providerId={}, modelCode={})生成标题失败: {}", providerId, modelCode, e.getMessage());
            return null;
        }
    }

    @Override
    public List<MessageVO> getMessages(Long sessionId, Long beforeId) {
        if (!sessionDao.existsById(sessionId)) {
            throw new NotFoundException("会话不存在: " + sessionId);
        }
        List<MessageEntity> entities = messageDao.findBySessionId(sessionId, beforeId, MESSAGE_PAGE_SIZE);
        List<MessageVO> result = new ArrayList<>();
        for (MessageEntity entity : entities) {
            result.add(toMessageVO(entity));
        }
        return result;
    }

    // ==================== 内部方法 ====================

    private SessionVO toVO(SessionEntity entity) {
        SessionVO vo = new SessionVO();
        vo.setId(entity.getId());
        vo.setAssistantId(entity.getAssistantId());
        vo.setTitle(entity.getTitle());
        vo.setPinned(entity.getPinned());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setMessageCount(entity.getMessageCount() != null ? entity.getMessageCount() : 0);
        return vo;
    }

    private MessageVO toMessageVO(MessageEntity entity) {
        MessageVO vo = new MessageVO();
        vo.setId(entity.getId());
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
        vo.setCacheReadInputTokens(entity.getCacheReadInputTokens());
        vo.setCacheWriteInputTokens(entity.getCacheWriteInputTokens());
        vo.setCreatedAt(entity.getCreatedAt());
        // 将 tool_calls JSON 字符串反序列化为 List<ToolExecutionEvent>，
        // 避免前端收到 String 类型后需要二次 JSON.parse（双层序列化问题）
        String toolCallsStr = entity.getToolCalls();
        if (toolCallsStr != null && !toolCallsStr.isEmpty() && !"[]".equals(toolCallsStr)) {
            try {
                vo.setToolCalls(objectMapper.readValue(toolCallsStr,
                        new TypeReference<List<ToolExecutionEvent>>() {
                        }));
            } catch (Exception e) {
                log.warn("反序列化 toolCalls 失败, sessionId={}: {}", entity.getSessionId(), e.getMessage());
                vo.setToolCalls(Collections.emptyList());
            }
        } else {
            vo.setToolCalls(Collections.emptyList());
        }
        return vo;
    }
}
