/**
 * @author Eddie
 * {@code @date} 2026-06-30
 *
 * 后端进程管理：启动、停止、等待就绪
 */

const {spawn} = require('child_process');
const path = require('path');
const http = require('http');
const fs = require('fs');

let backendProcess = null;

// ============================================================
// 后端路径
// ============================================================
function getBackendPath() {
    const {app} = require('electron');
    let basePath;
    if (!app.isPackaged) {
        basePath = path.join(__dirname, '..', 'target', 'eddie-app');
    } else {
        basePath = path.join(process.resourcesPath, 'eddie-app');
    }
    // Windows 下 PE 可执行文件必须带 .exe 后缀，否则 spawn() 会失败
    if (process.platform === 'win32') {
        basePath += '.exe';
    }
    return basePath;
}

// ============================================================
// 日志目录
// ============================================================
function getLogPath() {
    const {app} = require('electron');
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
function startBackend(mainWindow) {
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
        const {app} = require('electron');
        if (!global.isQuitting) app.quit();
    });

    backendProcess.on('error', (err) => {
        const msg = `Failed to start backend: ${err.message}`;
        console.error(`[Eddie] ${msg}`);
        logStream.write(`[FATAL] ${msg}\n`);
        logStream.end();
        const {app} = require('electron');
        if (!global.isQuitting) app.quit();
    });
}

// ============================================================
// 等待后端端口就绪
// ============================================================
function waitForBackend(port, retries = 60) {
    return new Promise((resolve, reject) => {
        const check = (n) => {
            if (global.isQuitting) {
                reject(new Error('App is quitting'));
                return;
            }
            let settled = false;
            const onSettled = () => {
                if (settled) return true;
                settled = true;
                return false;
            };
            const req = http.get(`http://localhost:${port}/api/health`, () => {
                if (onSettled()) return;
                console.log('[Eddie] Backend is ready!');
                resolve();
            });
            req.on('error', () => {
                if (onSettled()) return;
                if (n > 0) {
                    console.log(`[Eddie] Waiting for backend... (${n} retries left)`);
                    setTimeout(() => check(n - 1), 1000);
                } else {
                    reject(new Error('Backend did not start in time'));
                }
            });
            req.setTimeout(2000, () => {
                if (onSettled()) return;
                req.destroy();
                if (n > 0) setTimeout(() => check(n - 1), 1000);
                else reject(new Error('Backend did not start in time'));
            });
        };
        check(retries);
    });
}

// ============================================================
// 停止后端进程
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

module.exports = {startBackend, killBackend, waitForBackend};
