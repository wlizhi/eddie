package cc.wlizhi.eddieai.chat.service;

import cc.wlizhi.eddieai.chat.entity.request.AssistantCreateRequest;
import cc.wlizhi.eddieai.chat.entity.request.AssistantUpdateRequest;
import cc.wlizhi.eddieai.chat.entity.response.AssistantDetailVO;
import cc.wlizhi.eddieai.chat.entity.response.AssistantVO;

import java.util.List;

/**
 * 助手列表业务接口
 */
public interface AssistantService {

    /**
     * 查询助手列表
     *
     * @param showAll true=查询全部, false=仅查询启用的
     */
    List<AssistantVO> list(boolean showAll);

    /**
     * 获取助手详情（配置回显）
     */
    AssistantDetailVO getDetail(Long id);

    /**
     * 新建助手
     */
    AssistantVO create(AssistantCreateRequest request);

    /**
     * 更新助手设置
     */
    AssistantVO update(Long id, AssistantUpdateRequest request);

    /**
     * 删除助手
     */
    void delete(Long id);
}
