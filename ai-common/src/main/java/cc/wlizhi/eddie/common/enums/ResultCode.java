/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.common.enums;

/**
 * 返回状态码接口
 */
public interface ResultCode {

    /** 业务状态码 */
    int getCode();

    /** 描述信息（前端友好提示） */
    String getMessage();
}
