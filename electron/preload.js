/**
 * @author Eddie
 * {@code @date} 2026-06-30
 */

const {contextBridge, ipcRenderer} = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
    // 后端日志
    onBackendLog: (callback) => {
        ipcRenderer.on('backend-log', (_event, data) => callback(data));
    },
    removeBackendLogListener: () => {
        ipcRenderer.removeAllListeners('backend-log');
    },

    // ===== 窗口控制（TitleBar 组件使用） =====
    minimize: () => ipcRenderer.send('window-minimize'),
    maximize: () => ipcRenderer.send('window-maximize'),
    close: () => ipcRenderer.send('window-close'),
    isMaximized: () => ipcRenderer.invoke('window-is-maximized'),
    onMaximizedChange: (callback) => {
        ipcRenderer.on('window-maximized-changed', (_event, maximized) => callback(maximized));
    },
    removeMaximizedChangeListener: () => {
        ipcRenderer.removeAllListeners('window-maximized-changed');
    },

    // ===== 标题栏颜色同步 =====
    updateTitleBarOverlay: (options) => {
        ipcRenderer.send('update-title-bar-overlay', options);
    },

    // ===== macOS 暗色/亮色模式 =====
    setThemeSource: (source) => {
        ipcRenderer.send('set-theme-source', source);
    },
});
