package cc.wlizhi.eddie.common.enums;

import lombok.Getter;

/**
 * 消息角色
 */
@Getter
public enum MessageRole {

    USER("用户"),
    ASSISTANT("助手"),
    SYSTEM("系统");

    private final String label;

    MessageRole(String label) {
        this.label = label;
    }
}
