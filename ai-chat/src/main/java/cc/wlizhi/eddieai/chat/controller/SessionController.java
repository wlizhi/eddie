package cc.wlizhi.eddieai.chat.controller;

import cc.wlizhi.eddieai.chat.entity.request.SessionCreateRequest;
import cc.wlizhi.eddieai.chat.entity.request.TitleRenameRequest;
import cc.wlizhi.eddieai.chat.entity.response.MessageVO;
import cc.wlizhi.eddieai.chat.entity.response.SessionVO;
import cc.wlizhi.eddieai.chat.service.SessionService;
import cc.wlizhi.eddieai.common.dto.ApiResult;
import cc.wlizhi.eddieai.common.dto.PageResult;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话管理接口
 */
@RestController
@RequestMapping("/api/session")
public class SessionController {

    @Resource
    private SessionService sessionService;

    /**
     * 创建会话
     */
    @PostMapping
    public ApiResult<SessionVO> create(@Valid @RequestBody SessionCreateRequest request) {
        return ApiResult.success(sessionService.create(request.getAssistantId()));
    }

    /**
     * 分页查询会话列表（某助手下），支持 title 模糊搜索
     */
    @GetMapping("/list")
    public ApiResult<PageResult<SessionVO>> list(
            @RequestParam("assistantId") Long assistantId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "50") int pageSize) {
        return ApiResult.success(sessionService.list(assistantId, title, pageNum, pageSize));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") Long id) {
        sessionService.delete(id);
        return ApiResult.success();
    }

    /**
     * 手动重命名
     */
    @PutMapping("/{id}/title")
    public ApiResult<SessionVO> renameTitle(@PathVariable("id") Long id,
                                            @Valid @RequestBody TitleRenameRequest request) {
        return ApiResult.success(sessionService.renameTitle(id, request.getTitle()));
    }

    /**
     * AI 生成标题（无需请求体，模型从全局配置/助手配置自动降级读取）
     */
    @PostMapping("/{id}/generate-title")
    public ApiResult<String> generateTitle(@PathVariable("id") Long id) {
        return ApiResult.success(sessionService.generateTitle(id));
    }

    /**
     * 置顶
     */
    @PutMapping("/{id}/pin")
    public ApiResult<Void> pin(@PathVariable("id") Long id) {
        sessionService.pin(id);
        return ApiResult.success();
    }

    /**
     * 取消置顶
     */
    @PutMapping("/{id}/unpin")
    public ApiResult<Void> unpin(@PathVariable("id") Long id) {
        sessionService.unpin(id);
        return ApiResult.success();
    }

    /**
     * 获取会话消息（游标分页，倒序）
     */
    @GetMapping("/{id}/messages")
    public ApiResult<List<MessageVO>> getMessages(@PathVariable("id") Long id,
                                                  @RequestParam(value = "beforeId", required = false) Long beforeId) {
        return ApiResult.success(sessionService.getMessages(id, beforeId));
    }
}
