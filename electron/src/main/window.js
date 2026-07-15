/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 窗口管理：仅负责窗口创建、导航、错误页。
 * IPC 处理器已拆分至 ipc/ 目录，主题持久化拆分至 services/theme-persist.js。
 */

const {BrowserWindow} = require('electron');
const path = require('path');
const {getTheme, getMode} = require('./services/theme-persist');
const {IS_MAC, IS_WIN, IS_STANDALONE} = require('./utils/platform');

let mainWindow = null;

// ============================================================
// 启动加载页 — 使用主题颜色（从 theme-persist 内存缓存读取）
// ============================================================
function getLoadingHtml(theme) {
    const {bgPrimary: bg, textPrimary: text, accent, textTertiary, barTrack} = theme;

    return `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"><title>Eddie</title>
<style>*{margin:0;padding:0;box-sizing:border-box}
body{display:flex;flex-direction:column;align-items:center;justify-content:center;
height:100vh;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;
background:${bg};color:${text};user-select:none;overflow:hidden}
.brand{text-align:center;margin-bottom:48px}
.brand h1{font-size:36px;font-weight:600;letter-spacing:4px}
.bar-wrap{width:120px;height:2px;background:${barTrack};border-radius:1px;overflow:hidden}
.bar-inner{width:40%;height:100%;background:${accent};border-radius:1px;
animation:slide 1.4s ease-in-out infinite}
@keyframes slide{0%{transform:translateX(-100%)}50%{transform:translateX(150%)}100%{transform:translateX(250%)}}
.status{position:fixed;bottom:28px;font-size:12px;color:${textTertiary};letter-spacing:1px}</style></head><body>
<div class="brand"><h1>Eddie</h1></div>
<div class="bar-wrap"><div class="bar-inner"></div></div>
<div class="status">INITIALIZING</div></body></html>`;
}

// ============================================================
// 错误提示页
// ============================================================
function showErrorPage(errorMessage) {
    if (mainWindow && !mainWindow.isDestroyed()) return;

    const html = `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"><title>Eddie - 启动失败</title>
<style>*{margin:0;padding:0;box-sizing:border-box}
body{display:flex;flex-direction:column;align-items:center;justify-content:center;
height:100vh;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;
background:#1a1a1a;color:#e0e0e0;padding:2rem;text-align:center}
.icon{font-size:64px;margin-bottom:1rem}
h2{margin-bottom:.75rem;font-size:20px}
.detail{color:#aaa;max-width:400px;line-height:1.6;font-size:14px}
.hint{color:#666;margin-top:1.5rem;font-size:12px}</style></head><body>
<div class="icon">⚠️</div><h2>后端服务启动失败</h2>
<p class="detail">${errorMessage}</p><p class="hint">请查看日志文件获取详细信息</p>
</body></html>`;

    mainWindow = new BrowserWindow({
        width: 480, height: 320, resizable: false,
        title: 'Eddie - 启动失败', show: false,
        backgroundColor: '#1a1a1a',
        webPreferences: {
            preload: path.join(__dirname, '..', 'preload', 'index.js'),
            nodeIntegration: false,
            contextIsolation: true,
        },
    });
    mainWindow.once('ready-to-show', () => mainWindow.show());
    mainWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`);
}

// ============================================================
// 创建主窗口
// ============================================================
function createMainWindow() {
    const startupTheme = getTheme(getMode() === 'dark');

    const browserOptions = {
        width: 1200, height: 800, minWidth: 800, minHeight: 600,
        title: 'Eddie',
        backgroundColor: '#18181b',
        show: false,
        backgroundThrottling: false,
        webPreferences: {
            preload: path.join(__dirname, '..', 'preload', 'index.js'),
            nodeIntegration: false,
            contextIsolation: true,
        },
    };

    if (IS_MAC) {
        browserOptions.titleBarStyle = 'hiddenInset';
        browserOptions.titleBarOverlay = {color: '#18181b', symbolColor: '#a1a1aa'};
    } else if (IS_WIN) {
        browserOptions.titleBarStyle = 'hidden';
        browserOptions.titleBarOverlay = {color: '#18181b', symbolColor: '#a1a1aa'};
    }

    mainWindow = new BrowserWindow(browserOptions);

    mainWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(getLoadingHtml(startupTheme))}`);
    mainWindow.once('ready-to-show', () => mainWindow.show());

    // ============================================================
    // 拦截关闭事件，非退出时隐藏到托盘
    // ============================================================
    mainWindow.on('close', (event) => {
        if (!global.isQuitting) {
            event.preventDefault();
            mainWindow.hide();
        }
    });

    mainWindow.on('closed', () => {
        mainWindow = null;
    });

    // ============================================================
    // 拦截 will-navigate：防止普通 <a href> 点击导致窗口离开应用
    // ============================================================
    mainWindow.webContents.on('will-navigate', (event, url) => {
        try {
            const parsedUrl = new URL(url);
            if (parsedUrl.hostname === 'localhost') return;
        } catch {
            return;
        }
        event.preventDefault();
        require('electron').shell.openExternal(url);
    });

    mainWindow.webContents.setWindowOpenHandler(({url}) => {
        if (url.startsWith('http://localhost')) return {action: 'allow'};
        require('electron').shell.openExternal(url);
        return {action: 'deny'};
    });

    // ============================================================
    // 全屏状态变化（macOS 原生全屏时隐藏 TitleBar）
    // ============================================================
    mainWindow.on('enter-full-screen', () => {
        mainWindow?.webContents.send('fullscreen-changed', true);
    });
    mainWindow.on('leave-full-screen', () => {
        mainWindow?.webContents.send('fullscreen-changed', false);
    });

    // ============================================================
    // 窗口最大化变化通知
    // ============================================================
    mainWindow.on('maximize', () => mainWindow?.webContents.send('window-maximized-changed', true));
    mainWindow.on('unmaximize', () => mainWindow?.webContents.send('window-maximized-changed', false));
}

// ============================================================
// 导航到应用页面
// ============================================================
function navigateToApp() {
    if (!mainWindow || mainWindow.isDestroyed()) return;
    const {app} = require('electron');
    const url = IS_STANDALONE || app.isPackaged
        ? 'http://localhost:11520'
        : 'http://localhost:5173';
    mainWindow.loadURL(url);
    if (!app.isPackaged && !IS_STANDALONE) mainWindow.webContents.openDevTools();
}

// ============================================================
// 获取主窗口引用
// ============================================================
function getMainWindow() {
    return mainWindow;
}

module.exports = {createMainWindow, navigateToApp, showErrorPage, getMainWindow};
