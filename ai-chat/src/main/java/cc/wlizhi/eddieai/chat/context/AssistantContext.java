package cc.wlizhi.eddieai.chat.context;

import cc.wlizhi.eddieai.chat.dao.AssistantDao;
import cc.wlizhi.eddieai.chat.entity.AssistantEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 助手列表上下文，全应用生命周期缓存
 * <p>
 * 以 {@code assistantId} 为键快速查找助手信息，包括系统提示词、模型参数等。
 */
@Component
public class AssistantContext {

    private volatile Map<Long, AssistantEntity> assistantMap;

    @Resource
    private AssistantDao assistantDao;

    @PostConstruct
    void init() {
        refresh();
    }

    /**
     * 根据 assistantId 精确获取助手信息
     */
    public AssistantEntity getAssistantById(Long assistantId) {
        AssistantEntity entity = assistantMap.get(assistantId);
        if (entity != null) {
            return entity;
        }
        refresh();
        return assistantMap.get(assistantId);
    }

    /**
     * 获取全部缓存的助手列表（仅启用的）
     */
    public List<AssistantEntity> getAll() {
        return List.copyOf(assistantMap.values());
    }

    public void refresh() {
        List<AssistantEntity> all = assistantDao.findAll(true);
        Map<Long, AssistantEntity> map = new HashMap<>();
        for (AssistantEntity entity : all) {
            map.put(entity.getId(), entity);
        }
        this.assistantMap = map;
    }
}
