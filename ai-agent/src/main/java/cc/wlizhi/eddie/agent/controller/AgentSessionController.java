/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.agent.controller;

import cc.wlizhi.eddie.agent.entity.response.AgentMessageVO;
import cc.wlizhi.eddie.agent.entity.response.AgentSessionVO;
import cc.wlizhi.eddie.agent.service.AgentSessionService;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.dto.PageResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能体会话管理接口
 * <p>
 * 提供智能体会话的 CRUD 操作及消息查询。
 */
@RestController
@RequestMapping("/api/agent/session")
public class AgentSessionController {

    @Resource
    private AgentSessionService agentSessionService;

    /**
     * 创建会话
     *
     * @param agentId 智能体 ID
     * @return 新创建的会话
     */
    @PostMapping
    public ApiResult<AgentSessionVO> create(@RequestParam(name = "agentId") Long agentId) {
        return ApiResult.success(agentSessionService.create(agentId));
    }

    /**
     * 分页查询会话列表
     *
     * @param agentId  智能体 ID
     * @param title    标题模糊搜索（可选）
     * @param pageNum  页码（默认 1）
     * @param pageSize 每页大小（默认 50）
     * @return 分页结果
     */
    @RequestMapping("/list")
    public ApiResult<PageResult<AgentSessionVO>> list(
            @RequestParam(name = "agentId") Long agentId,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "50") int pageSize) {
        return ApiResult.success(agentSessionService.list(agentId, title, pageNum, pageSize));
    }

    /**
     * 删除会话
     *
     * @param id 会话 ID
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable(name = "id") Long id) {
        agentSessionService.delete(id);
        return ApiResult.success();
    }

    /**
     * 手动重命名
     *
     * @param id    会话 ID
     * @param title 新标题
     * @return 更新后的会话
     */
    @PutMapping("/{id}/title")
    public ApiResult<AgentSessionVO> renameTitle(@PathVariable(name = "id") Long id,
                                                 @RequestParam(name = "title") String title) {
        return ApiResult.success(agentSessionService.renameTitle(id, title));
    }

    /**
     * AI 生成标题（无需请求体，模型从全局配置自动降级读取）
     *
     * @param id 会话 ID
     * @return 生成/截取的标题
     */
    @PostMapping("/{id}/generate-title")
    public ApiResult<String> generateTitle(@PathVariable(name = "id") Long id) {
        return ApiResult.success(agentSessionService.generateTitle(id));
    }

    /**
     * 置顶
     *
     * @param id 会话 ID
     */
    @PutMapping("/{id}/pin")
    public ApiResult<Void> pin(@PathVariable(name = "id") Long id) {
        agentSessionService.pin(id);
        return ApiResult.success();
    }

    /**
     * 取消置顶
     *
     * @param id 会话 ID
     */
    @PutMapping("/{id}/unpin")
    public ApiResult<Void> unpin(@PathVariable(name = "id") Long id) {
        agentSessionService.unpin(id);
        return ApiResult.success();
    }

    /**
     * 查询会话消息列表（游标分页）
     * <p>
     * 返回按 id 正序排列的消息列表，供前端顺序渲染。
     *
     * @param sessionId 会话 ID
     * @param beforeId  游标 ID，返回比此 ID 更早的消息（可选，不传则返回最新消息）
     * @param limit     每页数量（默认 20）
     * @return 消息列表
     */
    @GetMapping("/{sessionId}/messages")
    public ApiResult<List<AgentMessageVO>> messages(
            @PathVariable(name = "sessionId") Long sessionId,
            @RequestParam(name = "beforeId", required = false) Long beforeId,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ApiResult.success(agentSessionService.findMessagesBySessionId(sessionId, beforeId, limit));
    }
}
