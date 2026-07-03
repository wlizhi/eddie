/**
 * @author Eddie
 * {@code @date} 2026-06-21
 */

package cc.wlizhi.eddie.chat.entity.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 助手列表展示 VO
 */
@Getter
@Setter
public class AssistantVO {

    private Long id;
    private String name;
    private String avatar;
    private String description;
    private String systemPrompt;
    private Long providerId;
    private String providerName;
    private String modelId;
    private Integer memoryRounds;
    private Integer enabled;
    private Integer sortOrder;
    private Long createdAt;
    private Long updatedAt;
}
