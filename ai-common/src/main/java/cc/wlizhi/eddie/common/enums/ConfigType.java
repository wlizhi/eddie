package cc.wlizhi.eddie.common.enums;

/**
 * 全局配置类型。<p>
 * <ul>
 *   <li>{@link #FRONTEND} — 前端可见，通过 {@code GET /api/settings/configs} 返回</li>
 *   <li>{@link #BACKEND} — 后端内置，不返回给前端</li>
 * </ul>
 *
 * @author Eddie
 * {@code @date} 2026-07-04
 */
public enum ConfigType {
    FRONTEND,
    BACKEND
}
