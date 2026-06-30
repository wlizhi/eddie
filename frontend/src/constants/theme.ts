/**
 * @author Eddie
 * @date 2026-06-21
 */

/**
 * 共享主题配置
 *
 * 来源：AssistantDialog.vue tipTheme
 * 已迁移至全局 useNaiveThemeOverrides，此文件保留仅为兼容导入，
 * 具体颜色值由三层体系自动推导，此处仅保留非颜色样式。
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const TIP_THEME_OVERRIDES: any = {
    peers: {
        popover: {
            padding: '5px 9px',
            fontSize: '12px',
            borderRadius: '5px',
        },
    },
}
