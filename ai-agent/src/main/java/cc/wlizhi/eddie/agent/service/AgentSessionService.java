/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.service;

import cc.wlizhi.eddie.agent.entity.response.AgentMessageVO;
import cc.wlizhi.eddie.agent.entity.response.AgentSessionVO;
import cc.wlizhi.eddie.common.dto.PageResult;

import java.util.List;

/**
 * 智能体会话管理业务接口
 */
public interface AgentSessionService {

    /**
     * 创建会话
     *
     * @param agentId 智能体 ID
     * @return 新创建的会话
     */
    AgentSessionVO create(Long agentId);

    /**
     * 分页查询会话列表
     *
     * @param agentId  智能体 ID
     * @param title    标题模糊搜索（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<AgentSessionVO> list(Long agentId, String title, int pageNum, int pageSize);

    /**
     * 删除会话（级联删除消息）
     *
     * @param id 会话 ID
     */
    void delete(Long id);

    /**
     * 手动重命名
     *
     * @param id    会话 ID
     * @param title 新标题
     * @return 更新后的会话
     */
    AgentSessionVO renameTitle(Long id, String title);

    /**
     * 置顶
     *
     * @param id 会话 ID
     */
    void pin(Long id);

    /**
     * 取消置顶
     *
     * @param id 会话 ID
     */
    void unpin(Long id);

    /**
     * 分页查询会话消息（游标分页，倒序）
     *
     * @param sessionId 会话 ID
     * @param beforeId  游标 ID，返回比此 ID 更早的消息（可选）
     * @param limit     每页数量（默认 20）
     * @return 消息列表（按 id 正序排列）
     */
    List<AgentMessageVO> findMessagesBySessionId(Long sessionId, Long beforeId, int limit);
}
