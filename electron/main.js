/**
 * @author Eddie
 * {@code @date} 2026-06-30
 */

const {app, BrowserWindow, nativeTheme, ipcMain} = require('electron');
const {spawn} = require('child_process');
const path = require('path');
const http = require('http');
const fs = require('fs');

// STANDALONE 模式：Electron 套壳独立运行的后端服务（如 IDEA 启动的 JAR）
// 设置环境变量 ELECTRON_STANDALONE=true 即可启用
const isStandalone = process.env.ELECTRON_STANDALONE === 'true';

let backendProcess = null;
let mainWindow = null;
let isQuitting = false;

// ============================================================
// 启动主题持久化路径
// ============================================================
const THEME_PREFS_PATH = path.join(app.getPath('userData'), 'theme-prefs.json')

function readThemePrefs() {
    try {
        if (fs.existsSync(THEME_PREFS_PATH)) {
            return JSON.parse(fs.readFileSync(THEME_PREFS_PATH, 'utf-8'))
        }
    } catch {}
    return null
}

function writeThemePrefs(theme) {
    try {
        const dir = path.dirname(THEME_PREFS_PATH)
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, {recursive: true})
        }
        fs.writeFileSync(THEME_PREFS_PATH, JSON.stringify(theme, null, 2), 'utf-8')
    } catch (err) {
        console.warn(`[Eddie] Failed to write theme prefs: ${err.message}`)
    }
}

// ============================================================
// 后端路径
// ============================================================
function getBackendPath() {
    if (!app.isPackaged) {
        return path.join(__dirname, '..', 'target', 'eddie-app');
    }
    return path.join(process.resourcesPath, 'eddie-app');
}

// ============================================================
// 日志目录
// ============================================================
function getLogPath() {
    return path.join(app.getPath('userData'), 'logs', 'eddie-app.log');
}

function ensureLogDir() {
    const logDir = path.dirname(getLogPath());
    if (!fs.existsSync(logDir)) {
        fs.mkdirSync(logDir, {recursive: true});
    }
}

// ============================================================
// 启动后端进程
// ============================================================
function startBackend() {
    const backendPath = getBackendPath();
    const logPath = getLogPath();

    console.log(`[Eddie] Starting backend: ${backendPath}`);
    console.log(`[Eddie] Log file: ${logPath}`);

    ensureLogDir();
    const logStream = fs.createWriteStream(logPath, {flags: 'a'});

    // macOS/Linux: 确保二进制文件可执行
    if (process.platform !== 'win32') {
        try {
            fs.chmodSync(backendPath, '755');
        } catch (err) {
            console.warn(`[Eddie] Failed to chmod backend: ${err.message}`);
        }
    }

    backendProcess = spawn(backendPath, [], {
        stdio: ['pipe', 'pipe', 'pipe'],
        windowsHide: true,
        env: {...process.env, EDDIE_ELECTRON: 'true'},
    });

    backendProcess.stdout.on('data', (data) => {
        const msg = data.toString();
        logStream.write(msg);
        if (mainWindow && !mainWindow.isDestroyed()) {
            mainWindow.webContents.send('backend-log', msg);
        }
    });

    backendProcess.stderr.on('data', (data) => {
        const msg = data.toString();
        logStream.write(`[ERR] ${msg}`);
        if (mainWindow && !mainWindow.isDestroyed()) {
            mainWindow.webContents.send('backend-log', `[ERR] ${msg}`);
        }
    });

    backendProcess.on('exit', (code) => {
        const msg = `Backend exited with code ${code}`;
        console.log(`[Eddie] ${msg}`);
        logStream.end();
        if (!isQuitting) app.quit();
    });

    backendProcess.on('error', (err) => {
        const msg = `Failed to start backend: ${err.message}`;
        console.error(`[Eddie] ${msg}`);
        logStream.write(`[FATAL] ${msg}\n`);
        logStream.end();
        if (!isQuitting) app.quit();
    });
}

// ============================================================
// 等待后端端口就绪
// ============================================================
function waitForBackend(port, retries = 60) {
    return new Promise((resolve, reject) => {
        const check = (n) => {
            if (isQuitting) {
                reject(new Error('App is quitting'));
                return;
            }
            const req = http.get(`http://localhost:${port}/api/session/list`, () => {
                console.log('[Eddie] Backend is ready!');
                resolve();
            });
            req.on('error', () => {
                if (n > 0) {
                    console.log(`[Eddie] Waiting for backend... (${n} retries left)`);
                    setTimeout(() => check(n - 1), 1000);
                } else {
                    reject(new Error('Backend did not start in time'));
                }
            });
            req.setTimeout(2000, () => {
                req.destroy();
                if (n > 0) setTimeout(() => check(n - 1), 1000);
                else reject(new Error('Backend did not start in time'));
            });
        };
        check(retries);
    });
}

// ============================================================
// 错误提示页
// ============================================================
function showErrorPage(errorMessage) {
    if (mainWindow && !mainWindow.isDestroyed()) return;

    const html = `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"><title>Eddie AI - 启动失败</title>
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
        title: 'Eddie AI - 启动失败', show: false,
        backgroundColor: '#1a1a1a',
        webPreferences: {preload: path.join(__dirname, 'preload.js'), nodeIntegration: false, contextIsolation: true},
    });
    mainWindow.once('ready-to-show', () => mainWindow.show());
    mainWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`);
}

// ============================================================
// 启动加载页 — 使用主题颜色（从 theme-prefs.json 读取）
// ============================================================
function getLoadingHtml(theme) {
    const bg = theme?.bgPrimary || '#18181b'
    const text = theme?.textPrimary || '#e4e4e7'
    const accent = theme?.accent || '#a1a1aa'
    const textTertiary = theme?.textTertiary || '#52525b'
    const barTrack = theme?.barTrack || '#27272a'

    return `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"><title>Eddie AI</title>
<style>*{margin:0;padding:0;box-sizing:border-box}
body{display:flex;flex-direction:column;align-items:center;justify-content:center;
height:100vh;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;
background:${bg};color:${text};user-select:none;overflow:hidden}
.brand{text-align:center;margin-bottom:48px}
.brand h1{font-size:36px;font-weight:600;letter-spacing:4px}
.brand h1 span{color:${textTertiary};font-weight:400}
.bar-wrap{width:120px;height:2px;background:${barTrack};border-radius:1px;overflow:hidden}
.bar-inner{width:40%;height:100%;background:${accent};border-radius:1px;
animation:slide 1.4s ease-in-out infinite}
@keyframes slide{0%{transform:translateX(-100%)}50%{transform:translateX(150%)}100%{transform:translateX(250%)}}
.status{position:fixed;bottom:28px;font-size:12px;color:${textTertiary};letter-spacing:1px}</style></head><body>
<div class="brand"><h1>Eddie <span>AI</span></h1></div>
<div class="bar-wrap"><div class="bar-inner"></div></div>
<div class="status">INITIALIZING</div></body></html>`;
}

// ============================================================
// 创建主窗口 (hiddenInset + titleBarOverlay + drag 原生双击最大化)
// ============================================================
function createMainWindow() {
    const startupTheme = readThemePrefs()

    mainWindow = new BrowserWindow({
        width: 1200, height: 800, minWidth: 800, minHeight: 600,
        title: 'Eddie AI',
        titleBarStyle: 'hiddenInset',
        titleBarOverlay: {color: '#18181b', symbolColor: '#a1a1aa'},
        backgroundColor: '#18181b',       // 与深色主题一致，消除白闪
        show: false,
        backgroundThrottling: false,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            nodeIntegration: false,
            contextIsolation: true,
        },
    });

    mainWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(getLoadingHtml(startupTheme))}`);
    mainWindow.once('ready-to-show', () => mainWindow.show());

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
    ipcMain.on('window-close', () => mainWindow?.close());
    ipcMain.handle('window-is-maximized', () => mainWindow?.isMaximized() ?? false);

    mainWindow?.on('maximize', () => mainWindow?.webContents.send('window-maximized-changed', true));
    mainWindow?.on('unmaximize', () => mainWindow?.webContents.send('window-maximized-changed', false));

    // 更新标题栏 overlay 颜色（前端主题切换时调用）
    ipcMain.on('update-title-bar-overlay', (_e, {color, symbolColor}) => {
        if (mainWindow && !mainWindow.isDestroyed()) {
            mainWindow.setTitleBarOverlay?.({color, symbolColor});
        }
    });

    // 同步 macOS 暗色/亮色模式（让原生标题栏跟随前端主题）
    ipcMain.on('set-theme-source', (_e, source) => {
        nativeTheme.themeSource = source; // 'dark' | 'light' | 'system'
    });

    // 持久化启动主题（前端主题切换时写入，下次启动时加载页使用相同配色）
    ipcMain.on('save-startup-theme', (_e, theme) => {
        writeThemePrefs(theme)
    })
}

// ============================================================
// 退出
// ============================================================
function killBackend() {
    if (backendProcess && !backendProcess.killed) {
        console.log('[Eddie] Killing backend process...');
        backendProcess.kill('SIGTERM');
        setTimeout(() => {
            if (backendProcess && !backendProcess.killed) backendProcess.kill('SIGKILL');
        }, 5000);
    }
}

// ============================================================
// App 生命周期
// ============================================================
app.whenReady().then(async () => {
    // 1. 立即显示窗口
    createMainWindow();
    setupIpc();

    if (isStandalone) {
        // STANDALONE 模式：后端已在外部（IDEA 等）启动，直连即可
        console.log('[Eddie] Standalone mode: connecting to existing backend at http://localhost:11520');
        navigateToApp();
        return;
    }

    // 2. 标准模式：启动后端并等待就绪
    startBackend();

    try {
        await waitForBackend(11520);
        navigateToApp();
    } catch (err) {
        console.error(`[Eddie] ${err.message}`);
        showErrorPage(err.message);
        setTimeout(() => {
            if (!isQuitting) app.quit();
        }, 15000);
    }
});

app.on('before-quit', () => {
    isQuitting = true;
    killBackend();
});
app.on('window-all-closed', () => {
    killBackend();
    app.quit();
});
app.on('activate', () => {
    if (mainWindow === null) {
        createMainWindow();
        setupIpc();
    }
});
