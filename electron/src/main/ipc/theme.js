/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 主题切换 IPC 处理器：同步暗色/亮色模式到原生标题栏，持久化启动主题
 */

const {ipcMain, nativeTheme} = require('electron');
const {writeThemePrefs} = require('../services/theme-persist');
const {getMainWindow} = require('../window');
const {getSelectionService} = require('../selection-assistant');

function register() {
    // 同步 macOS 暗色/亮色模式（让原生标题栏跟随前端主题）
    ipcMain.on('set-theme-source', (_e, source) => {
        nativeTheme.themeSource = source; // 'dark' | 'light' | 'system'
    });

    // 实时主题更新：前端推送配色 → writeThemePrefs 同时更新内存 + 磁盘
    ipcMain.on('theme:update', (_e, theme) => {
        if (!theme) return;
        writeThemePrefs(theme);
        // 同步字体大小到划词助手（跟随全局设置）
        if (theme.fontSize) {
            try {
                getSelectionService().updateConfig({fontSize: theme.fontSize});
            } catch (err) {
                console.warn('[theme:update] Failed to sync fontSize:', err.message);
            }
        }
        // 同步字体类型到划词助手（跟随全局设置）
        if (theme.fontFamily) {
            try {
                getSelectionService().updateConfig({fontFamily: theme.fontFamily});
            } catch (err) {
                console.warn('[theme:update] Failed to sync fontFamily:', err.message);
            }
        }
        // 通知所有已存活的划词助手弹窗主题已变更（弹窗自行通过 IPC 获取最新主题数据）
        try {
            const svc = getSelectionService();
            svc?.windows?.sendToAllPopups('selection:theme-changed');
            svc?.windows?.sendToAllPopups('selection:settings-changed');
            console.log('[theme:update] Notified all popup windows of theme change');
        } catch (err) {
            console.warn('[theme:update] Failed to notify popup:', err.message);
        }
    });

    // 保留旧通道（兼容，可移除）
    ipcMain.on('save-startup-theme', (_e, theme) => {
        if (theme) writeThemePrefs(theme);
    });

    // 更新标题栏 overlay 颜色（前端主题切换时调用）
    ipcMain.on('update-title-bar-overlay', (_e, {color, symbolColor}) => {
        const win = getMainWindow();
        if (win && !win.isDestroyed()) {
            try {
                win.setTitleBarOverlay?.({color, symbolColor});
            } catch (err) {
                console.warn('[Eddie] setTitleBarOverlay not supported on this platform:', err.message);
            }
        }
    });
}

module.exports = {register};
