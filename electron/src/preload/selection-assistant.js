/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手窗口的预加载脚本
 * 工具栏和弹窗窗口都使用此脚本，暴露必要的 IPC 通道
 *
 * 注意：SSE 流式请求已由浏览器直连后端 API (fetch) 处理，
 * 不再需要通过主进程代理转发。
 */

const {contextBridge, ipcRenderer} = require('electron');

contextBridge.exposeInMainWorld('selectionAPI', {
    // ============================================================
    // 窗口操作
    // ============================================================
    /** 发送：隐藏工具栏 */
    hideToolbar: () => ipcRenderer.send('selection:hide-toolbar'),
    /** 发送：关闭弹窗 */
    closePopup: () => ipcRenderer.send('selection:close-popup'),

    /** 发送：切换置顶 */
    togglePin: () => ipcRenderer.send('selection:toggle-pin'),

    /**
     * 监听置顶状态变更通知（主进程 selection:pin-changed 时推送）
     * @param {function} callback - 回调接收置顶状态 (pinned: boolean)
     * @returns {function} 取消监听的函数
     */
    onPinChanged: (callback) => {
        const handler = (_e, pinned) => callback(pinned);
        ipcRenderer.on('selection:pin-changed', handler);
        return () => ipcRenderer.removeListener('selection:pin-changed', handler);
    },

    // ============================================================
    // 弹窗数据获取（Vue 弹窗组件调用）
    // ============================================================

    /**
     * 获取弹窗启动数据（选中文本、动作、主题色、字号等）
     * 由 Vue 弹窗组件在 onMounted 时调用
     * @returns {Promise<{action:string, text:string, fontSize:number, theme:object, targetLang?:string}>}
     */
    getPopupData: () => ipcRenderer.invoke('selection:get-popup-data'),

    // ============================================================
    // 主题实时同步（Electron 主进程推送通知 → 弹窗自行获取数据）
    // ============================================================

    /**
     * 获取当前主题颜色数据（从 theme-persist 内存缓存读取）
     * @returns {Promise<object>} 包含 bgPrimary/bgSecondary 等 10 个颜色字段的主题对象
     */
    getTheme: () => ipcRenderer.invoke('selection:get-theme'),

    /**
     * 获取当前显示设置标识符（弹窗自行计算主题 CSS 变量）
     * @returns {Promise<{themeId:string, themeMode:string, colorScheme:string, fontSize:number}>}
     */
    getDisplaySettings: () => ipcRenderer.invoke('selection:get-display-settings'),

    /**
     * 监听主题变更通知（主进程 theme:update 时推送，不携带数据）
     * @param {function} callback - 无参回调
     * @returns {function} 取消监听的函数
     */
    onThemeChanged: (callback) => {
        const handler = () => callback();
        ipcRenderer.on('selection:theme-changed', handler);
        return () => ipcRenderer.removeListener('selection:theme-changed', handler);
    },

    // ============================================================
    // 配置管理（弹窗获取完整配置）
    // ============================================================

    /**
     * 获取划词助手完整配置（复用 selection:get-config handler）
     * @returns {Promise<object>} SelectionAssistantConfig
     */
    getSelectionConfig: () => ipcRenderer.invoke('selection:get-config'),

    /**
     * 监听配置变更通知（主题/划词配置修改时推送）
     * @param {function} callback - 无参回调
     * @returns {function} 取消监听的函数
     */
    onSettingsChanged: (callback) => {
        const handler = () => callback();
        ipcRenderer.on('selection:settings-changed', handler);
        return () => ipcRenderer.removeListener('selection:settings-changed', handler);
    },

    // ============================================================
    // 剪贴板
    // ============================================================

    /** 复制文本到剪贴板 */
    copyToClipboard: (text) => ipcRenderer.send('selection:copy-text', text),
});
