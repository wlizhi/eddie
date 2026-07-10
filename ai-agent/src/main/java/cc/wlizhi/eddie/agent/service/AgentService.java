/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.service;

import cc.wlizhi.eddie.agent.entity.request.AgentCreateRequest;
import cc.wlizhi.eddie.agent.entity.request.AgentUpdateRequest;
import cc.wlizhi.eddie.agent.entity.response.AgentDetailVO;
import cc.wlizhi.eddie.agent.entity.response.AgentVO;
import cc.wlizhi.eddie.chat.entity.response.ToolSourceVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 智能体管理业务接口
 */
public interface AgentService {

    /**
     * 查询智能体列表
     *
     * @param showAll true=查询全部, false=仅查询启用的
     */
    List<AgentVO> list(boolean showAll);

    /**
     * 获取智能体详情（配置回显）
     */
    AgentDetailVO getDetail(Long id);

    /**
     * 新建智能体
     */
    AgentVO create(AgentCreateRequest request);

    /**
     * 更新智能体设置
     */
    AgentVO update(Long id, AgentUpdateRequest request);

    /**
     * 更新智能体头像（支持文字、emoji、图片上传）
     *
     * @param id         智能体 ID
     * @param avatarText 文字或 emoji（可选）
     * @param file       图片文件（可选）
     */
    AgentVO updateAvatar(Long id, String avatarText, MultipartFile file);

    /**
     * 删除智能体（级联删除关联会话和消息）
     */
    void delete(Long id);

    /**
     * 批量排序：按 ID 数组顺序重新赋 sort_order（1,2,3...）
     */
    void batchSort(List<Long> ids);

    /**
     * 获取智能体已绑定的 MCP 工具列表（二层结构：MCP → tools）
     * <p>
     * 仅返回当前智能体已绑定的 MCP Server 及其下辖工具。
     * 按 MCP sort_order 排序，供输入框手动模式选择 MCP 使用。
     *
     * @param agentId 智能体 ID
     * @return 已绑定的 MCP + 工具列表，无绑定返回空列表
     */
    List<ToolSourceVO> getBoundMcpTools(Long agentId);
}
