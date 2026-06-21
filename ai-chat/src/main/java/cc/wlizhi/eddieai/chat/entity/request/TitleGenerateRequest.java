package cc.wlizhi.eddieai.chat.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * AI 生成标题请求
 */
@Getter
@Setter
public class TitleGenerateRequest {

    @NotNull(message = "providerId 不能为空")
    private Long providerId;

    @NotBlank(message = "modelCode 不能为空")
    private String modelCode;
}
