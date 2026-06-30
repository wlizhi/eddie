/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.common.enums;

import lombok.Getter;

/**
 * 角色类型：助手 / 智能体
 * <p>
 * 枚举 name() 直接作为数据库中 owner_type 的存储值，
 * 如 {@code RoleType.ASSISTANT.name()} → {@code "ASSISTANT"}。
 */
@Getter
public enum RoleType {

    ASSISTANT("助手"),
    AGENT("智能体");

    private final String label;

    RoleType(String label) {
        this.label = label;
    }

}
