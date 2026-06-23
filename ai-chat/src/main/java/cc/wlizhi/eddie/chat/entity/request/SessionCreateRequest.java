package cc.wlizhi.eddie.chat.entity.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建会话请求
 */
@Getter
@Setter
public class SessionCreateRequest {

    @NotNull(message = "assistantId 不能为空")
    private Long assistantId;
}
