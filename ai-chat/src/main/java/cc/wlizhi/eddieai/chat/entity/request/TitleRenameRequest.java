package cc.wlizhi.eddieai.chat.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 手动重命名会话标题请求
 */
@Getter
@Setter
public class TitleRenameRequest {

    @NotBlank(message = "title 不能为空")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;
}
