/**
 * @author Eddie
 * {@code @date} 2026-07-09
 */

package cc.wlizhi.eddie.chat.entity.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 工具绑定状态
 * <p>
 * 表示单个工具在当前助手下的使用方式：
 * <ul>
 *   <li>status=0 — 禁用</li>
 *   <li>status=1 — 自动批准</li>
 *   <li>status=2 — 人工审批</li>
 * </ul>
 */
@Getter
@Setter
public class ToolBinding {

    /** 工具定义 ID */
    private Long toolId;

    /** 绑定状态：0=禁用, 1=自动批准, 2=人工审批 */
    private Integer status;
}
