/**
 * @author Eddie
 * {@code @date} 2026-06-30
 *
 * Electron 主进程入口 — 仅做模块编排
 */

const {app} = require('electron');
const {startBackend, killBackend, waitForBackend} = require('./backend');
const {createMainWindow, navigateToApp, setupIpc, showErrorPage, getMainWindow} = require('./window');
const {createTray} = require('./tray');

// ============================================================
// 全局标志：用于跨模块传递"是否正在退出"状态
// ============================================================
global.isQuitting = false;

/**
 * 判断是否为 STANDALONE 模式（后端由外部如 IDEA 启动）
 */
function isStandalone() {
    return process.env.ELECTRON_STANDALONE === 'true';
}

// ============================================================
// App 生命周期
// ============================================================
app.whenReady().then(async () => {
    // 1. 创建主窗口（含 close 事件拦截 → 隐藏到托盘）
    createMainWindow();
    setupIpc();

    // 2. 创建系统托盘
    createTray(getMainWindow());

    if (isStandalone()) {
        console.log('[Eddie] Standalone mode: connecting to existing backend at http://localhost:11520');
        navigateToApp();
        return;
    }

    // 3. 标准模式：启动后端并等待就绪
    startBackend(getMainWindow());

    try {
        await waitForBackend(11520);
        navigateToApp();
    } catch (err) {
        console.error(`[Eddie] ${err.message}`);
        showErrorPage(err.message);
        // 错误时不自动退出，用户可通过托盘菜单操作
    }
});

app.on('before-quit', () => {
    // 真正退出时（托盘"退出"或 Cmd+Q）才杀后端
    global.isQuitting = true;
    killBackend();
});

app.on('window-all-closed', () => {
    // 不退出应用——窗口已由 close 事件拦截到托盘隐藏
    // macOS 下此事件由关闭最后一个窗口触发，忽略即可
    // Win/Linux 下窗口隐藏后不会触发此事件（因为窗口未销毁）
});

app.on('activate', () => {
    // macOS 下点击程序坞图标时恢复窗口
    const mainWindow = getMainWindow();
    if (mainWindow === null) {
        createMainWindow();
        setupIpc();
    } else if (!mainWindow.isDestroyed()) {
        mainWindow.show();
    }
});
