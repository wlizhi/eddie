/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手 IPC 处理器
 *
 * 注意：SSE 流式请求已由浏览器直连后端 API (fetch) 处理，
 * 因此移除了 selection:stream-start / processSseEvent 等代理代码。
 * 弹窗 Vue 组件通过同源 fetch('/api/selection-assistant/stream') 直接消费 SSE 流。
 */

const {ipcMain, clipboard, nativeTheme} = require('electron');
const {getSelectionService} = require('../selection-assistant');
const {getTheme, getDisplaySettings} = require('../services/theme-persist');

function register() {
    const service = getSelectionService();

    // 工具栏操作：点击功能项
    ipcMain.on('selection:action', (_e, actionId) => {
        service.handleAction(actionId);
    });

    // 工具栏操作：隐藏工具栏
    ipcMain.on('selection:hide-toolbar', () => {
        service.windows.hideToolbar();
    });

    // 弹窗操作：关闭弹窗（由指定窗口的关闭按钮触发）
    ipcMain.on('selection:close-popup', (event) => {
        const win = require('electron').BrowserWindow.fromWebContents(event.sender);
        service.windows.closePopup(win);
    });

    // 弹窗操作：切换置顶状态（由指定窗口的置顶按钮触发）
    ipcMain.on('selection:toggle-pin', (event) => {
        const win = require('electron').BrowserWindow.fromWebContents(event.sender);
        service.windows.togglePopupPin(win);
    });

    // ============================================================
    // 弹窗数据获取（Vue 弹窗组件 onMounted 时调用）
    // ============================================================

    /**
     * Vue 弹窗组件打开后调用此 handler 获取选中文本、动作、主题色等数据。
     * 数据在 handleAction 时已缓存到 service._popupData
     */
    ipcMain.handle('selection:get-popup-data', () => {
        return service.getPopupData();
    });

    // ============================================================
    // 剪贴板（copy 功能使用）
    // ============================================================

    ipcMain.on('selection:copy-text', (_e, text) => {
        if (text) {
            clipboard.writeText(text);
            console.log(`[Selection] Copied to clipboard: "${text.substring(0, 40)}..."`);
        }
    });

    // ============================================================
    // 配置管理（供后端调用）
    // ============================================================

    ipcMain.on('selection:update-config', (_e, config) => {
        service.updateConfig(config);
    });

    ipcMain.handle('selection:get-config', () => {
        return service.getConfig();
    });

    // 获取当前主题颜色数据（从 theme-persist 内存缓存读取）
    ipcMain.handle('selection:get-theme', () => {
        return getTheme(nativeTheme.shouldUseDarkColors);
    });

    // 获取当前显示设置标识符（themeId / themeMode / colorScheme / fontSize）
    ipcMain.handle('selection:get-display-settings', () => {
        return getDisplaySettings();
    });

    ipcMain.handle('selection:is-activated', () => {
        return service.isActivated();
    });

    // ============================================================
    // 运行时配置刷新（由前端通知触发）
    // ============================================================

    ipcMain.on('selection:config-changed', () => {
        service.refreshConfig();
    });
}

module.exports = {register};
