/**
 * @author Eddie
 * {@code @date} 2026-06-30
 *
 * Electron 主进程入口 — 仅做模块编排
 */

const {app} = require('electron');

// ============================================================
// 启动时间戳（仅用于控制台调试输出）
// ============================================================
const PROCESS_START = Date.now();

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
const {IS_STANDALONE} = require('./utils/platform');
const {initSelectionAssistant, getSelectionService} = require('./selection-assistant');

// ============================================================
// 点击应用图标后，最早可执行的业务代码：异步启动后端
// 与 Electron/Chromium 初始化并行运行
// ============================================================
console.log('[TIMING] module loaded at', Date.now() - PROCESS_START, 'ms');
if (!IS_STANDALONE) {
    console.log('[TIMING] spawning backend at', Date.now() - PROCESS_START, 'ms');
    startBackend(null);
}

const {createMainWindow, navigateToApp, showErrorPage, getMainWindow} = require('./window');
const {setupIpc} = require('./ipc');
const {createTray} = require('./tray');
const {startLocalServer, PORT: LOCAL_PORT} = require('./server');

// ============================================================
// 全局标志：用于跨模块传递"是否正在退出"状态
// ============================================================
global.isQuitting = false;

// ============================================================
// App 生命周期
// ============================================================
app.whenReady().then(async () => {
    console.log('[TIMING] app.whenReady done at', Date.now() - PROCESS_START, 'ms');

    // 1. 启动本地 HTTP 服务（提供前端静态资源 + API 代理）
    startLocalServer();

    // 2. 立即创建窗口，显示启动动画（不依赖后端）
    createMainWindow();

    if (!IS_STANDALONE) {
        // 3. 等待后端就绪（加载动画持续播放）
        const t0 = Date.now();
        try {
            await waitForBackend(11520);
        } catch (err) {
            console.error(`[Eddie] ${err.message}`);
            showErrorPage(err.message);
            return;
        }
        console.log('[TIMING] backend health check passed at', Date.now() - PROCESS_START, 'ms, wait duration:', Date.now() - t0, 'ms');

        // 4. 后端就绪后：导航到本地 SPA
        navigateToApp(LOCAL_PORT);
        console.log('[TIMING] navigateToApp called at', Date.now() - PROCESS_START, 'ms');

        // 5. 注册托盘、IPC、划词助手（不阻塞导航）
        createTray(getMainWindow());
        setupIpc();
        initSelectionAssistant();
        // 6. 拉取划词助手用户配置
        getSelectionService().refreshConfig();
    } else {
        console.log('[Eddie] Standalone mode: backend is managed externally');
        navigateToApp(11520);
        setupIpc();
        initSelectionAssistant();
        createTray(getMainWindow());
    }
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


