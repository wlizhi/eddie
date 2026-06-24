/**
 * Naive UI 主题映射桥接
 *
 * 将三层体系（外观→主题→强调色）的 CSS 变量值自动映射到 Naive UI 组件 token，
 * 使所有 Naive UI 组件自动跟随 displaySettings 变化。
 *
 * 用法：在 App.vue 中绑定到 NConfigProvider 的 theme-overrides prop
 *
 * 注意：仅覆盖项目中实际使用到的组件，避免未知 token 导致 Naive UI 异常。
 * 新增 Naive UI 组件时，在此文件补充对应的 token 映射即可。
 */
import {computed} from 'vue'
import {COLOR_SCHEMES, displaySettings, findTheme} from '@/composables/useDisplaySettings'

/**
 * 从主题定义中读取 CSS 变量值，取不到时返回空字符串
 */
function v(name: string): string {
    const theme = findTheme(displaySettings.themeId)
    const vars = theme?.variables[displaySettings.themeMode]
    return vars?.[name] ?? ''
}

/**
 * 获取当前强调色的对应值
 */
function accent(key: 'accent' | 'hover' | 'lightBg' | 'lightBorder' | 'ring' | 'borderFocus' | 'textAccent'): string {
    const scheme = COLOR_SCHEMES[displaySettings.colorScheme]
    if (!scheme) return ''
    const vars = displaySettings.themeMode === 'dark' ? scheme.dark : scheme.light
    return vars[key]
}

/**
 * 自动生成 Naive UI theme-overrides
 * 依赖 displaySettings 的响应式变化自动重新计算
 */
export const naiveThemeOverrides = computed(() => {
    // 显式依赖响应式属性，确保 themeId/themeMode/colorScheme 变化时重新计算
    void displaySettings.themeId
    void displaySettings.themeMode
    void displaySettings.colorScheme

    const bgPrimary = v('--bg-primary')
    const bgSecondary = v('--bg-secondary')
    const bgTertiary = v('--bg-tertiary')
    const bgHover = v('--bg-hover')
    const bgMask = v('--bg-mask')
    const bgTooltip = v('--bg-tooltip')

    const textPrimary = v('--text-primary')
    const textSecondary = v('--text-secondary')
    const textTertiary = v('--text-tertiary')
    const textInverse = v('--text-inverse')

    const borderDefault = v('--border-default')
    const borderLight = v('--border-light')

    const accentDefault = accent('accent') || v('--accent-default')
    const accentHover = accent('hover') || v('--accent-hover')
    const accentRing = accent('ring') || v('--accent-ring')
    const borderFocus = accent('borderFocus') || v('--border-focus')

    const dividerLight = v('--divider-light')
    const dangerDefault = v('--danger-default')
    const dangerHover = v('--danger-hover')

    return {
        // ===== 通用基础 token —— 影响所有组件 =====
        common: {
            primaryColor: accentDefault,
            primaryColorHover: accentHover,
            primaryColorPressed: accentHover,
            primaryColorSuppl: accentDefault,
            bodyColor: bgPrimary,
            textColor1: textPrimary,
            textColor2: textSecondary,
            textColor3: textTertiary,
            borderColor: borderDefault,
            dividerColor: dividerLight,
            errorColor: dangerDefault,
            hoverColor: bgHover,
            inputColor: bgSecondary,
            popoverColor: bgPrimary,
            placeholderColor: textTertiary,
            closeColor: textTertiary,
            closeColorHover: textPrimary,
            tableColor: bgPrimary,
            actionColor: bgTertiary,
            clearColor: textTertiary,
            borderRadius: '8px',
        },

        // ===== Select 下拉选择器（InputArea、AssistantDialog、DefaultModelPanel 使用）=====
        Select: {
            menuColor: bgPrimary,
            menuBoxShadow: `0 4px 16px ${bgMask}`,
            color: bgSecondary,
            border: `1px solid ${borderLight}`,
            borderFocus: `1px solid ${borderFocus}`,
            boxShadowFocus: `0 0 0 2px ${accentRing}`,
            placeholderColor: textTertiary,
            actionTextColor: accentDefault,
        },

        // ===== Modal 模态框（AssistantDialog 使用）=====
        Modal: {
            color: bgPrimary,
            textColor: textPrimary,
            titleTextColor: textPrimary,
            titleFontWeight: '600',
            boxShadow: `0 20px 60px ${bgMask}`,
        },

        // ===== Tooltip 提示（AssistantDialog 使用）=====
        Tooltip: {
            color: bgTooltip,
            textColor: textInverse,
            boxShadow: `0 2px 8px ${bgMask}`,
            borderRadius: '6px',
        },

        // ===== Card 卡片 =====
        Card: {
            color: bgPrimary,
            colorModal: bgPrimary,
            borderColor: borderDefault,
            textColor: textPrimary,
            titleTextColor: textPrimary,
        },

        // ===== Button 按钮 =====
        Button: {
            color: accentDefault,
            colorHover: accentHover,
            colorPressed: accentHover,
            textColor: textInverse,
            textColorHover: textInverse,
            border: `1px solid ${accentDefault}`,
            borderHover: `1px solid ${accentHover}`,
            borderRadius: '8px',
        },

        // ===== Input 输入框 =====
        Input: {
            color: bgSecondary,
            colorFocus: bgPrimary,
            textColor: textPrimary,
            border: `1px solid ${borderLight}`,
            borderFocus: `1px solid ${borderFocus}`,
            boxShadowFocus: `0 0 0 2px ${accentRing}`,
            placeholderColor: textTertiary,
            caretColor: accentDefault,
        },

        // ===== Message 消息提示 =====
        Message: {
            color: bgPrimary,
            textColor: textPrimary,
            border: `1px solid ${borderDefault}`,
            boxShadow: `0 4px 16px ${bgMask}`,
            borderRadius: '10px',
        },

        // ===== Dialog 对话框 =====
        Dialog: {
            color: bgPrimary,
            textColor: textPrimary,
            titleTextColor: textPrimary,
            boxShadow: `0 20px 60px ${bgMask}`,
        },

        // ===== Switch 开关 =====
        Switch: {
            railColor: v('--icon-muted'),
            railColorActive: accentDefault,
            buttonColor: textInverse,
        },

        // ===== Checkbox 复选框 =====
        Checkbox: {
            color: bgPrimary,
            border: `1px solid ${borderLight}`,
            borderFocus: `1px solid ${borderFocus}`,
            colorChecked: accentDefault,
            colorCheckedHover: accentHover,
            textColor: textPrimary,
        },

        // ===== Tag 标签 =====
        Tag: {
            color: bgTertiary,
            textColor: textSecondary,
        },

        // ===== Dropdown 下拉菜单 =====
        Dropdown: {
            color: bgPrimary,
            textColor: textPrimary,
            optionColorHover: bgHover,
            optionTextColorHover: textPrimary,
            dividerColor: dividerLight,
        },

        // ===== Popover 弹出层 =====
        Popover: {
            color: bgPrimary,
            textColor: textPrimary,
            boxShadow: `0 4px 16px ${bgMask}`,
            borderRadius: '10px',
        },

        // ===== Spin 加载 =====
        Spin: {
            color: accentDefault,
        },

        // ===== Progress 进度条 =====
        Progress: {
            color: accentDefault,
            textColor: textSecondary,
        },

        // ===== Tabs 标签页 =====
        Tabs: {
            tabTextColor: textTertiary,
            tabTextColorActive: accentDefault,
            tabTextColorHover: textPrimary,
            barColor: accentDefault,
            paneTextColor: textPrimary,
        },

        // ===== Badge 徽标 =====
        Badge: {
            color: dangerDefault,
            textColor: textInverse,
        },

        // ===== Alert 警告 =====
        Alert: {
            color: bgSecondary,
            textColor: textPrimary,
            titleTextColor: textPrimary,
        },

        // ===== LoadingBar 加载条 =====
        LoadingBar: {
            color: accentDefault,
        },

        // ===== DataTable 数据表格 =====
        DataTable: {
            color: bgPrimary,
            textColor: textPrimary,
            borderColor: borderDefault,
            thColor: bgSecondary,
            thTextColor: textSecondary,
            tdColor: bgPrimary,
            tdColorHover: bgHover,
        },

        // ===== Form 表单 =====
        Form: {
            labelTextColor: textSecondary,
            feedbackTextColor: dangerDefault,
        },

        // ===== Empty 空状态 =====
        Empty: {
            textColor: textTertiary,
        },

        // ===== Skeleton 骨架屏 =====
        Skeleton: {
            color: bgTertiary,
            colorEnd: bgHover,
        },
    }
})
