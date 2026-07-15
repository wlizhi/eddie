/**
 * @author Eddie
 * {@code @date} 2026-06-30
 *
 * Electron 主进程入口 — 仅做模块编排
 */

const {app} = require('electron');
const {startBackend, killBackend, waitForBackend} = require('./backend');
const {createMainWindow, navigateToApp, showErrorPage, getMainWindow} = require('./window');
const {setupIpc} = require('./ipc');
const {createTray} = require('./tray');
const {initSelectionAssistant} = require('./selection-assistant');
const {IS_STANDALONE} = require('./utils/platform');

// ============================================================
// 全局标志：用于跨模块传递"是否正在退出"状态
// ============================================================
global.isQuitting = false;

// ============================================================
// App 生命周期
// ============================================================
app.whenReady().then(async () => {
    // 1. 创建主窗口（含 close 事件拦截 → 隐藏到托盘）
    createMainWindow();

    // 2. 注册 IPC 处理器
    setupIpc();

    // 3. 初始化划词助手（不自动激活，等待后端配置）
    initSelectionAssistant();

    // 4. 创建系统托盘
    createTray(getMainWindow());

    if (IS_STANDALONE) {
        console.log('[Eddie] Standalone mode: connecting to existing backend at http://localhost:11520');
        navigateToApp();
        return;
    }

    // 4. 标准模式：启动后端并等待就绪
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
