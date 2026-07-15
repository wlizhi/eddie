/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手弹窗窗口的全局类型声明
 * selectionAPI 由 preload/selection-assistant.js 通过 contextBridge 注入
 */

declare global {
    /** 划词助手功能项 */
    interface FeatureItem {
        id: string
        label: string
        enabled: boolean
        order: number
    }

    /** 划词助手完整配置（对应后端 SELECTION_ASSISTANT_CONFIG） */
    interface SelectionAssistantConfig {
        enabled: boolean
        toolbar: {
            style: 'default' | 'compact'
        }
        window: {
            rememberSize: boolean
            autoClose: boolean
            alwaysOnTop: boolean
            opacity: number
        }
        features: FeatureItem[]
    }

    interface SelectionAPI {
    /** 发送：点击功能项 */
    sendAction: (actionId: string) => void
    /** 发送：隐藏工具栏 */
    hideToolbar: () => void
    /** 发送：关闭弹窗 */
    closePopup: () => void
    /** 发送：切换置顶 */
    togglePin: () => void

    /**
     * 监听置顶状态变更通知
     * @param callback - 回调接收置顶状态 (pinned: boolean)
     * @returns 取消监听的函数
     */
    onPinChanged: (callback: (pinned: boolean) => void) => () => void

    /** 获取弹窗启动数据 */
    getPopupData: () => Promise<{
        action: string
        text: string
        fontSize: number
        theme: {
            bgPrimary: string
            bgSecondary: string
            bgCard: string
            textPrimary: string
            textSecondary: string
            textTertiary: string
            accent: string
            border: string
            hover: string
        }
        targetLang?: string
    }>

    /** 获取当前主题颜色数据 */
    getTheme: () => Promise<Record<string, string>>

    /** 获取当前显示设置标识符 */
    getDisplaySettings: () => Promise<{
        themeId: string
        themeMode: string
        colorScheme: string
        fontSize: number
        fontFamily: string
    }>

    /** 监听主题变更通知 */
    onThemeChanged: (callback: () => void) => () => void

    /** 复制文本到剪贴板 */
    copyToClipboard: (text: string) => void

    /** 获取划词助手完整配置 */
    getSelectionConfig: () => Promise<SelectionAssistantConfig>

    /** 监听配置/主题变更通知（主题或划词配置修改时触发） */
    onSettingsChanged: (callback: () => void) => () => void
}

    interface Window {
        selectionAPI: SelectionAPI | undefined
    }
}

export {}
