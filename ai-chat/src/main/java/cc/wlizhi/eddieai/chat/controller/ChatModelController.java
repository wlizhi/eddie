package cc.wlizhi.eddieai.chat.controller;

import cc.wlizhi.eddieai.chat.entity.response.ChatModelSelectorVO;
import cc.wlizhi.eddieai.chat.service.ChatModelService;
import cc.wlizhi.eddieai.common.dto.ApiResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天模型选择器
 */
@RestController
@RequestMapping("/api/chat-model")
public class ChatModelController {

    @Resource
    private ChatModelService chatModelService;

    /**
     * 获取聊天模型选择器列表（按供应商分组，仅含启用的模型）
     */
    @GetMapping("/list")
    public ApiResult<List<ChatModelSelectorVO>> listChatModels() {
        return ApiResult.success(chatModelService.listChatModels());
    }
}
