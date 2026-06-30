/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.entity.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 会话列表响应 VO
 */
@Getter
@Setter
public class SessionVO {

    /**
     * 会话 ID
     */
    private Long id;

    /**
     * 归属助手 ID
     */
    private Long assistantId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 0=普通, 1=置顶
     */
    private Integer pinned;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 累计 token 数
     */
    private Integer totalTokens;

    /**
     * 最后活跃时间
     */
    private String updatedAt;
}
