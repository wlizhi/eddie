package cc.wlizhi.eddieai.common.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 会话表映射实体
 */
@Getter
@Setter
public class SessionEntity {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 归属助手 ID
     */
    private Long assistantId;

    /**
     * 会话标题（AI 生成，默认为空字符串）
     */
    private String title;

    /**
     * 0=普通, 1=置顶
     */
    private Integer pinned;

    /**
     * 消息数量（冗余字段，每次发消息时同步更新）
     */
    private Integer messageCount;

    /**
     * 累计 token 数（冗余字段，每次发消息时同步更新）
     */
    private Integer totalTokens;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 最后活跃时间
     */
    private String updatedAt;
}
