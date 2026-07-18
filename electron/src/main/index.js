/**
 * @author Eddie
 * {@code @date} 2026-06-30
 *
 * Electron 主进程入口 — 仅做模块编排
 */

const {app} = require('electron');

// ============================================================
// 过滤 macOS 系统层 Text Input 警告日志（_TIPropertyValueIsValid）
// ============================================================
const stderrWrite = process.stderr.write.bind(process.stderr);
process.stderr.write = (buf, enc, cb) => {
    if (buf && (buf.includes('_TIPropertyValueIsValid') || buf.includes('imkxpc_setApplicationProperty'))) {
        if (typeof cb === 'function') cb();
        return true;
    }
    return stderrWrite(buf, enc, cb);
};

const {startBackend, killBackend, waitForBackend} = require('./backend');
const {createMainWindow, navigateToApp, showErrorPage, getMainWindow} = require('./window');
const {setupIpc} = require('./ipc');
const {createTray} = require('./tray');
const {initSelectionAssistant, getSelectionService} = require('./selection-assistant');
const {IS_STANDALONE} = require('./utils/platform');

// ============================================================
// 全局标志：用于跨模块传递"是否正在退出"状态
// ============================================================
global.isQuitting = false;

// ============================================================
// App 生命周期
// ============================================================
app.whenReady().then(async () => {
    // 1. 立即创建窗口，显示启动动画（不依赖后端）
    createMainWindow();

    if (!IS_STANDALONE) {
        // 2. 启动后端（窗口已存在，日志可通过 IPC 传递到加载页）
        startBackend(null);
        try {
            await waitForBackend(11520);
        } catch (err) {
            console.error(`[Eddie] ${err.message}`);
            showErrorPage(err.message);
            return;
        }
    } else {
        console.log('[Eddie] Standalone mode: backend is managed externally');
    }

    // 3. 后端已就绪，注册 IPC 并初始化其余组件
    setupIpc();

    // 3. 初始化划词助手（此时可 HTTP 请求后端获取用户配置）
    await initSelectionAssistant();

    // 4. 创建系统托盘
    createTray(getMainWindow());

    // 5. 导航到应用
    navigateToApp();
});

app.on('before-quit', () => {
    // 真正退出时（托盘"退出"或 Cmd+Q）才杀后端
    global.isQuitting = true;
    // 先清理划词助手（停止 hook 事件线程 + 关闭窗口），否则进程无法退出
    getSelectionService().destroy();
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
