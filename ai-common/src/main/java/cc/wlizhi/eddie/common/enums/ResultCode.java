/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.common.enums;

/**
 * 返回状态码接口
 */
public interface ResultCode {

    /** 状态码 */
    int getCode();

    /** 状态描述 */
    String getMessage();
}
