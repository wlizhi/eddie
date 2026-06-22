package cc.wlizhi.eddieai.chat.service;

import cc.wlizhi.eddieai.chat.entity.response.MessageVO;
import cc.wlizhi.eddieai.chat.entity.response.SessionVO;
import cc.wlizhi.eddieai.common.dto.PageResult;

import java.util.List;

/**
 * 会话管理业务接口
 */
public interface SessionService {

    /**
     * 创建会话
     *
     * @param assistantId 归属助手 ID
     * @return 新会话
     */
    SessionVO create(Long assistantId);

    /**
     * 分页查询某助手下的会话列表（置顶 → 更新时间倒序），支持 title 模糊搜索
     *
     * @param assistantId 归属助手 ID
     * @param title       标题模糊搜索关键字（传 null 或空字符串则不过滤）
     * @param pageNum     页码（从 1 开始）
     * @param pageSize    每页大小
     * @return 分页结果
     */
    PageResult<SessionVO> list(Long assistantId, String title, int pageNum, int pageSize);

    /**
     * 删除会话（级联删除所有消息）
     */
    void delete(Long id);

    /**
     * 手动重命名
     */
    SessionVO renameTitle(Long id, String title);

    /**
     * 置顶
     */
    void pin(Long id);

    /**
     * 取消置顶
     */
    void unpin(Long id);

    /**
     * AI 生成标题（取首轮对话，调模型生成）
     *
     * @param sessionId  会话 ID
     * @param providerId 模型服务商 ID
     * @param modelCode  模型 code
     * @return 生成的标题
     */
    String generateTitle(Long sessionId, Long providerId, String modelCode);

    /**
     * 游标分页获取会话消息（倒序，最新在前）
     *
     * @param sessionId 会话 ID
     * @param beforeId  游标，首次传 null
     * @return 消息列表（倒序）
     */
    List<MessageVO> getMessages(Long sessionId, Long beforeId);
}
