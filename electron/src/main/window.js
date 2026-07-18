/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 窗口管理：仅负责窗口创建、导航、错误页。
 * IPC 处理器已拆分至 ipc/ 目录，主题持久化拆分至 services/theme-persist.js。
 */

const {BrowserWindow} = require('electron');
const path = require('path');
const {getTheme, getMode, getDisplaySettings} = require('./services/theme-persist');
const {IS_MAC, IS_WIN} = require('./utils/platform');

let mainWindow = null;

// ============================================================
// 启动加载页 — 使用主题颜色（从 theme-persist 内存缓存读取）
// ============================================================
function getLoadingHtml(theme, fontFamily) {
    const {bgPrimary: bg, textPrimary: text, accent, textTertiary} = theme;
    const ff = fontFamily || "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Noto Sans SC', sans-serif";

    return `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"><title>Eddie</title>
<style>*{margin:0;padding:0;box-sizing:border-box}
body{display:flex;flex-direction:column;align-items:center;justify-content:center;
height:100vh;font-family:${ff};
background:${bg};color:${text};user-select:none;overflow:hidden}

/* Background soft glow */
body::before{content:'';position:absolute;width:520px;height:520px;border-radius:50%;
background:radial-gradient(circle,${accent}10 0%,transparent 70%);
top:50%;left:50%;transform:translate(-50%,-50%);
animation:glowPulse 4s ease-in-out infinite}
@keyframes glowPulse{0%,100%{opacity:0.35;transform:translate(-50%,-50%) scale(1)}50%{opacity:1;transform:translate(-50%,-50%) scale(1.2)}}

/* Logo wrapper - entrance */
.logo-wrap{text-align:center;margin-bottom:32px;
animation:logoEntrance .8s cubic-bezier(.34,1.56,.64,1) forwards;
opacity:0;transform:scale(.85)}
@keyframes logoEntrance{to{opacity:1;transform:scale(1)}}

/* ===== Eddie letter animations ===== */
.brand-text{display:inline-flex;gap:2px;position:relative;padding:0 4px}
.letter{display:inline-block;font-size:52px;font-weight:700;
font-family:${ff};
animation:letterWave 2.8s ease-in-out infinite}
.l1{animation-delay:0s}.l2{animation-delay:.18s}.l3{animation-delay:.36s}
.l4{animation-delay:.54s}.l5{animation-delay:.72s}
@keyframes letterWave{
0%,100%{transform:translateY(0) scale(1)}
25%{transform:translateY(-8px) scale(1.04)}
50%{transform:translateY(0) scale(1)}
75%{transform:translateY(-4px) scale(1.02)}
}

/* Electric spark dots */
.sparks{display:flex;gap:3px;justify-content:center;margin-top:6px;height:4px}
.spark{width:3px;height:3px;border-radius:50%;background:${accent};opacity:0;
animation:sparkle 2.8s ease-in-out infinite}
.s1{animation-delay:.1s}.s2{animation-delay:.3s}.s3{animation-delay:.5s}
.s4{animation-delay:.7s}.s5{animation-delay:.9s}
@keyframes sparkle{
0%,100%{opacity:0;transform:scale(0)}
20%{opacity:.8;transform:scale(1.2)}
40%{opacity:0;transform:scale(0)}
}

/* Status */
.status{position:fixed;bottom:28px;font-size:11px;color:${textTertiary};letter-spacing:2px;
animation:statusPulse 2s ease-in-out infinite}
@keyframes statusPulse{0%,100%{opacity:.5}50%{opacity:1}}</style></head><body>

<div class="logo-wrap">
<div class="brand-text">
<span class="letter l1" style="color:${accent}">E</span>
<span class="letter l2" style="color:${accent}">d</span>
<span class="letter l3" style="color:${accent}">d</span>
<span class="letter l4" style="color:${accent}">i</span>
<span class="letter l5" style="color:${accent}">e</span>
</div>
<div class="sparks"><span class="spark s1"></span><span class="spark s2"></span><span class="spark s3"></span><span class="spark s4"></span><span class="spark s5"></span></div>
</div>

<div class="status">INITIALIZING</div>

</body></html>`;
}

// ============================================================
// 错误提示页
// ============================================================
function showErrorPage(errorMessage) {
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

    if (mainWindow && !mainWindow.isDestroyed()) {
        // 窗口已存在时，在现有窗口中加载错误页
        mainWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`);
        return;
    }

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
    const {fontFamily} = getDisplaySettings();

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

    mainWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(getLoadingHtml(startupTheme, fontFamily))}`);
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
// 导航到应用页面（port: 本地 HTTP 服务端口）
// ============================================================
function navigateToApp(port) {
    if (!mainWindow || mainWindow.isDestroyed()) return;
    const {app} = require('electron');
    mainWindow.loadURL(`http://localhost:${port}`);
    if (!app.isPackaged) mainWindow.webContents.openDevTools();
}

// ============================================================
// 获取主窗口引用
// ============================================================
function getMainWindow() {
    return mainWindow;
}

module.exports = {createMainWindow, navigateToApp, showErrorPage, getMainWindow};
