/**
 * @author Eddie
 * {@code @date} 2026-06-30
 *
 * 窗口管理：创建主窗口、主题持久化、IPC 通信
 */

const {BrowserWindow, nativeTheme, ipcMain} = require('electron');
const path = require('path');
const fs = require('fs');

let mainWindow = null;

// ============================================================
// 启动主题持久化路径
// ============================================================
const THEME_PREFS_PATH = (() => {
    const {app} = require('electron');
    return path.join(app.getPath('userData'), 'theme-prefs.json');
})();

function readThemePrefs() {
    try {
        if (fs.existsSync(THEME_PREFS_PATH)) {
            return JSON.parse(fs.readFileSync(THEME_PREFS_PATH, 'utf-8'));
        }
    } catch {
    }
    return null;
}

function writeThemePrefs(theme) {
    try {
        const dir = path.dirname(THEME_PREFS_PATH);
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, {recursive: true});
        }
        fs.writeFileSync(THEME_PREFS_PATH, JSON.stringify(theme, null, 2), 'utf-8');
    } catch (err) {
        console.warn(`[Eddie] Failed to write theme prefs: ${err.message}`);
    }
}

// ============================================================
// 启动加载页 — 使用主题颜色（从 theme-prefs.json 读取）
// ============================================================
function getLoadingHtml(theme) {
    const bg = theme?.bgPrimary || '#18181b';
    const text = theme?.textPrimary || '#e4e4e7';
    const accent = theme?.accent || '#a1a1aa';
    const textTertiary = theme?.textTertiary || '#52525b';
    const barTrack = theme?.barTrack || '#27272a';

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
        webPreferences: {preload: path.join(__dirname, 'preload.js'), nodeIntegration: false, contextIsolation: true},
    });
    mainWindow.once('ready-to-show', () => mainWindow.show());
    mainWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`);
}

// ============================================================
// 创建主窗口 (平台自适应: macOS hiddenInset + overlay, Windows hidden + overlay, Linux 默认)
// ============================================================
function createMainWindow() {
    const startupTheme = readThemePrefs();
    const isMac = process.platform === 'darwin';
    const isWin = process.platform === 'win32';

    const browserOptions = {
        width: 1200, height: 800, minWidth: 800, minHeight: 600,
        title: 'Eddie',
        backgroundColor: '#18181b',       // 与深色主题一致，消除白闪
        show: false,
        backgroundThrottling: false,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            nodeIntegration: false,
            contextIsolation: true,
        },
    };

    if (isMac) {
        // macOS: hiddenInset — 保留交通灯区域，标题栏通过 TitleBar.vue 自定义拖拽区
        browserOptions.titleBarStyle = 'hiddenInset';
        browserOptions.titleBarOverlay = {color: '#18181b', symbolColor: '#a1a1aa'};
    } else if (isWin) {
        // Windows: hidden — 隐藏原生标题栏，保留原生窗口控制按钮覆盖层
        browserOptions.titleBarStyle = 'hidden';
        browserOptions.titleBarOverlay = {color: '#18181b', symbolColor: '#a1a1aa'};
    }
    // Linux: 默认标题栏，不做特殊处理

    mainWindow = new BrowserWindow(browserOptions);

    mainWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(getLoadingHtml(startupTheme))}`);
    mainWindow.once('ready-to-show', () => mainWindow.show());

    // ============================================================
    // ★ 关键改动：拦截关闭事件，非退出时隐藏到托盘
    // ============================================================
    mainWindow.on('close', (event) => {
        if (!global.isQuitting) {
            event.preventDefault();
            mainWindow.hide();
        }
        // isQuitting 为 true 时（托盘"退出"触发的），让窗口正常关闭
    });

    mainWindow.on('closed', () => {
        mainWindow = null;
    });

    mainWindow.webContents.setWindowOpenHandler(({url}) => {
        if (url.startsWith('http://localhost')) return {action: 'allow'};
        require('electron').shell.openExternal(url);
        return {action: 'deny'};
    });
}

function navigateToApp() {
    if (!mainWindow || mainWindow.isDestroyed()) return;
    const {app} = require('electron');
    const isStandalone = process.env.ELECTRON_STANDALONE === 'true';
    // STANDALONE 模式直接连接后端端口（后端已在 IDEA 等外部启动）
    const url = isStandalone || app.isPackaged ? 'http://localhost:11520' : 'http://localhost:5173';
    mainWindow.loadURL(url);
    if (!app.isPackaged && !isStandalone) mainWindow.webContents.openDevTools();
}

// ============================================================
// IPC
// ============================================================
function setupIpc() {
    // 窗口控制（自定义按钮备用）
    ipcMain.on('window-minimize', () => mainWindow?.minimize());
    ipcMain.on('window-maximize', () => {
        mainWindow?.isMaximized() ? mainWindow.unmaximize() : mainWindow.maximize();
    });
    // ★ window-close 行为不变，close 事件拦截已在 createMainWindow 中处理
    ipcMain.on('window-close', () => mainWindow?.close());
    ipcMain.handle('window-is-maximized', () => mainWindow?.isMaximized() ?? false);

    mainWindow?.on('maximize', () => mainWindow?.webContents.send('window-maximized-changed', true));
    mainWindow?.on('unmaximize', () => mainWindow?.webContents.send('window-maximized-changed', false));

    // 更新标题栏 overlay 颜色（前端主题切换时调用）
    ipcMain.on('update-title-bar-overlay', (_e, {color, symbolColor}) => {
        if (mainWindow && !mainWindow.isDestroyed()) {
            try {
                mainWindow.setTitleBarOverlay?.({color, symbolColor});
            } catch (err) {
                // Windows/Linux 未启用 overlay 时调用会抛出 TypeError，静默忽略
                console.warn('[Eddie] setTitleBarOverlay not supported on this platform:', err.message);
            }
        }
    });

    // 同步 macOS 暗色/亮色模式（让原生标题栏跟随前端主题）
    ipcMain.on('set-theme-source', (_e, source) => {
        nativeTheme.themeSource = source; // 'dark' | 'light' | 'system'
    });

    // 持久化启动主题（前端主题切换时写入，下次启动时加载页使用相同配色）
    ipcMain.on('save-startup-theme', (_e, theme) => {
        writeThemePrefs(theme);
    });
}

// ============================================================
// 获取主窗口引用（供其他模块使用）
// ============================================================
function getMainWindow() {
    return mainWindow;
}

module.exports = {createMainWindow, navigateToApp, setupIpc, showErrorPage, getMainWindow};
