package cc.wlizhi.eddieai.chat.service;

import cc.wlizhi.eddieai.chat.entity.response.ChatModelSelectorVO;

import java.util.List;

/**
 * 聊天模型选择器业务接口
 */
public interface ChatModelService {

    /**
     * 获取聊天模型选择器列表（按供应商分组，仅含启用的供应商及其模型）
     */
    List<ChatModelSelectorVO> listChatModels();
}
