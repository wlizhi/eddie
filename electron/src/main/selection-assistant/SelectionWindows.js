/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手窗口管理 — 工具栏窗口（常驻）+ 弹窗窗口（按需创建）
 */

const {BrowserWindow, screen, nativeTheme} = require('electron');
const path = require('path');
const {getTheme} = require('../services/theme-persist');
const {IS_MAC} = require('../utils/platform');

// ============================================================
// 工具栏尺寸计算（基于 fontSize，跟随全局字体设置）
// ============================================================

// 基准：fontSize 为 14px 时，各尺寸
//   btnHeight = ceil(14 * 1.5) = 21
//   btnPad    = ceil(14 * 0.28) = 4
//   padV      = ceil(14 * 0.28) = 4
//   dividerH  = ceil(14 * 0.7) = 10
//   containerPadV = ceil(14 * 0.28) = 4
//   toolbarHeight = btnHeight + containerPadV * 2 = 29

function calcToolbarHeight(fontSize) {
    const btnH = Math.ceil(fontSize * 1.5);
    const padV = Math.ceil(fontSize * 0.28);
    return btnH + padV * 2;
}

function getBtnHeight(fontSize) { return Math.ceil(fontSize * 1.5); }
function getBtnPad(fontSize) { return Math.ceil(fontSize * 0.28); }
function getContainerPadV(fontSize) { return Math.ceil(fontSize * 0.28); }
function getContainerPadH(fontSize) { return Math.ceil(fontSize * 0.28); }
function getBtnFontSize(fontSize) { return Math.max(11, Math.round(fontSize * 0.82)); }
function getDividerH(fontSize) { return Math.ceil(fontSize * 0.7); }
function getCloseBtnSize(fontSize) { return Math.ceil(fontSize * 1.14); }
function getToolbarGap(fontSize) { return Math.max(2, Math.round(fontSize * 0.14)); }

// 度量文字宽度
function measureText(s, fontSize) {
    const cjkW = Math.round(fontSize * 0.86); // 中文字宽 ≈ 0.86em
    const asciiW = Math.round(fontSize * 0.5); // ASCII 字宽 ≈ 0.5em
    let w = 0;
    for (const ch of s) w += ch.charCodeAt(0) > 127 ? cjkW : asciiW;
    return w;
}

// 根据功能项动态计算工具栏宽度
function calcToolbarWidth(items, fontSize) {
    const enabled = items.filter(i => i.enabled).sort((a, b) => a.order - b.order);
    if (!enabled.length) return 120;

    const padH = getContainerPadH(fontSize);
    const btnPad = getBtnPad(fontSize);
    const gap = getToolbarGap(fontSize);
    const iconW = 14;
    const gapIconText = Math.max(2, Math.round(fontSize * 0.15));
    const dividerW = 1;
    const closeBtn = getCloseBtnSize(fontSize);

    let actionsW = 0;
    enabled.forEach((item, idx) => {
        const textW = measureText(item.label || '', fontSize);
        actionsW += btnPad + iconW + gapIconText + textW + btnPad;
        if (idx < enabled.length - 1) actionsW += dividerW + gap;
    });

    return padH + actionsW + gap + closeBtn + padH;
}

// ============================================================
// 工具栏 HTML
// ============================================================
function getToolbarHtml(theme, text, items, fontSize) {
    const textEscaped = text
        .replace(/&/g, '&')
        .replace(/</g, '<')
        .replace(/>/g, '>')
        .replace(/"/g, '"');

    const itemsHtml = items
        .filter(i => i.enabled)
        .sort((a, b) => a.order - b.order)
        .map((i, idx, arr) => {
            const label = i.label || '';
            const btn = `<button class="action-btn" data-id="${i.id}">${i.icon || ''} ${label}</button>`;
            return idx < arr.length - 1 ? btn + '<div class="btn-divider"></div>' : btn;
        })
        .join('');

    const toolbarH = calcToolbarHeight(fontSize);
    const btnH = getBtnHeight(fontSize);
    const btnPad = getBtnPad(fontSize);
    const btnFs = getBtnFontSize(fontSize);
    const dividerH = getDividerH(fontSize);
    const closeSz = getCloseBtnSize(fontSize);
    const padV = getContainerPadV(fontSize);
    const padH = getContainerPadH(fontSize);
    const gap = getToolbarGap(fontSize);

    return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<style>
*{margin:0;padding:0;box-sizing:border-box}
html,body{height:100%;overflow:hidden}
body{
    font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;
    font-size:${fontSize}px;background:transparent;color:${theme.textPrimary};
    user-select:none;
}
.toolbar{
    -webkit-app-region:drag;
    display:flex;align-items:center;height:${toolbarH}px;
    padding:0 ${padH}px;gap:${gap}px;
    background:${theme.bgSecondary};border-radius:8px;
    border:1px solid ${theme.border};
    box-shadow:0 4px 10px rgba(0,0,0,.3);
}
.actions{display:flex;align-items:center;gap:${gap}px;-webkit-app-region:no-drag}
.action-btn{
    -webkit-app-region:no-drag;
    display:inline-flex;align-items:center;gap:${Math.max(2, Math.round(fontSize * 0.15))}px;
    height:${btnH}px;padding:0 ${btnPad}px;border:none;border-radius:5px;
    font-size:${btnFs}px;cursor:pointer;white-space:nowrap;
    background:transparent;color:${theme.textPrimary};
    transition:background .12s;
}
.action-btn:hover{background:${theme.hover}}
.action-btn:active{background:${theme.border}}
.btn-divider{
    width:1px;height:${dividerH}px;align-self:center;
    background:${theme.border};opacity:.3;flex-shrink:0;
}
.close-btn{
    display:inline-flex;align-items:center;justify-content:center;
    width:${closeSz}px;height:${closeSz}px;border:none;border-radius:4px;
    font-size:${Math.round(fontSize * 0.86)}px;cursor:pointer;background:transparent;
    color:${theme.textTertiary};-webkit-app-region:no-drag;
    transition:background .12s;
}
.close-btn:hover{background:${theme.hover};color:${theme.textPrimary}}
</style>
</head>
<body>
<div class="toolbar">
    <div class="actions">${itemsHtml}</div>
    <button class="close-btn" id="closeBtn"><svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><line x1="4" y1="4" x2="10" y2="10"/><line x1="10" y1="4" x2="4" y2="10"/></svg></button>
</div>
</body>
</html>`;
}

// ============================================================
// 弹窗 HTML — 委托给 popups/ 各模块
// ============================================================
function getPopupHtml(theme, text, actionId, fontSize) {
    const textEscaped = text
        .replace(/&/g, '&')
        .replace(/</g, '<')
        .replace(/>/g, '>')
        .replace(/"/g, '"');
    return require('./popups').getPopupHtml(actionId || 'open', theme, text, textEscaped, fontSize);
}

// ============================================================
// 窗口管理类
// ============================================================
class SelectionWindows {
    constructor() {
        this.toolbarWindow = null;
        this.popupWindow = null;
    }

    /**
     * 获取当前主题色（从 theme-persist 内存缓存读取，无磁盘 IO）
     */
    getCurrentTheme() {
        return getTheme(nativeTheme.shouldUseDarkColors);
    }

    /**
     * 创建工具栏窗口（单例，常驻）
     */
    ensureToolbar() {
        if (this.toolbarWindow && !this.toolbarWindow.isDestroyed()) {
            return this.toolbarWindow;
        }

        const theme = this.getCurrentTheme();

        this.toolbarWindow = new BrowserWindow({
            width: 1,     // 初始占位，showToolbar 时会用 calcToolbarWidth 动态计算
            height: 1,    // 同上，由 calcToolbarHeight 动态计算
            frame: false,
            transparent: true,
            resizable: false,
            alwaysOnTop: true,
            skipTaskbar: true,
            show: false,
            type: 'panel',
            acceptsFirstMouse: true, // macOS: 非活跃窗口的首次点击直接传递到 web contents
            webPreferences: {
                preload: path.join(__dirname, '..', '..', 'preload', 'selection-assistant.js'),
                sandbox: false,
                nodeIntegration: false,
                contextIsolation: true,
            },
        });

        // 转发渲染进程日志到主进程控制台，用于调试
        this.toolbarWindow.webContents.on('console-message', (_e, level, message) => {
            console.log(`[Toolbar:renderer] ${message}`);
        });

        this.toolbarWindow.webContents.on('crashed', () => {
            console.error('[Toolbar] WebContents crashed!');
        });

        this.toolbarWindow.webContents.on('did-fail-load', (_e, code, desc) => {
            console.error(`[Toolbar] Failed to load: ${code} ${desc}`);
        });

        this.toolbarWindow.webContents.on('did-stop-loading', () => {
            console.log('[Toolbar] did-stop-loading fired');
        });

        this.toolbarWindow.on('closed', () => {
            this.toolbarWindow = null;
        });

        return this.toolbarWindow;
    }

    /**
     * 显示工具栏
     * @param {string} text - 选中文本
     * @param {Array} items - 功能项配置
     * @param {{x:number, y:number}} position - 屏幕坐标（逻辑像素）
     * @param {number} fontSize - 基准字体大小 px
     */
    showToolbar(text, items, position, fontSize) {
        const win = this.ensureToolbar();
        const theme = this.getCurrentTheme();

        const fs = fontSize || 14;
        const toolbarHeight = calcToolbarHeight(fs);
        const toolbarWidth = calcToolbarWidth(items, fs);

        // 计算位置：确保不超出屏幕边界
        const display = screen.getDisplayNearestPoint(position);
        const workArea = display.workArea;

        let x = Math.round(Math.max(workArea.x, Math.min(
            position.x - toolbarWidth / 2,
            workArea.x + workArea.width - toolbarWidth
        )));
        let y = Math.round(Math.max(workArea.y, Math.min(
            position.y + 12,
            workArea.y + workArea.height - toolbarHeight
        )));

        // 如果下方空间不足，显示在选中文本上方
        if (y + toolbarHeight > workArea.y + workArea.height) {
            y = Math.round(position.y - toolbarHeight - 6);
        }

        win.setPosition(x, y, false);
        win.setBounds({width: toolbarWidth, height: toolbarHeight, x, y});

        // 等新 HTML 加载完成后再显示，避免残留旧内容闪现
        win.webContents.once('did-finish-load', () => {
            if (!win.isDestroyed()) {
                console.log('[Toolbar] did-finish-load, injecting event handlers');
                // 通过 executeJavaScript 注入事件处理器（运行在主 world，可访问 contextBridge 暴露的 selectionAPI）
                win.webContents.executeJavaScript(`
                    console.log('[Toolbar] Event handlers injected, selectionAPI:', typeof window.selectionAPI);

                    document.getElementById('closeBtn').addEventListener('click', () => {
                        console.log('[Toolbar] Close button clicked');
                        window.selectionAPI.hideToolbar();
                    });

                    document.querySelectorAll('.action-btn').forEach(btn => {
                        btn.addEventListener('click', () => {
                            console.log('[Toolbar] Button clicked:', btn.dataset.id);
                            window.selectionAPI.sendAction(btn.dataset.id);
                        });
                    });
                `).catch(err => console.error('[Toolbar] executeJavaScript failed:', err));

                console.log('[Toolbar] did-finish-load, showing window');
                win.showInactive();
            }
        });
        console.log('[Toolbar] loadURL called');
        win.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(
            getToolbarHtml(theme, text, items, fs)
        )}`);
    }

    /**
     * 隐藏工具栏
     */
    hideToolbar() {
        if (this.toolbarWindow && !this.toolbarWindow.isDestroyed()) {
            this.toolbarWindow.hide();
        }
    }

    /**
     * 更新工具栏显示的选中文本（无需重新加载 HTML）
     */
    updateToolbarText(text) {
        if (!this.isToolbarVisible()) return;
        this.toolbarWindow.webContents.executeJavaScript(
            `document.querySelector('.text-preview span').textContent = ${JSON.stringify(text)};`
        ).catch(() => {});
    }

    /**
     * 工具栏是否可见
     */
    isToolbarVisible() {
        return !!(this.toolbarWindow && !this.toolbarWindow.isDestroyed() && this.toolbarWindow.isVisible());
    }

    /**
     * 判断指定屏幕坐标是否在工具栏窗口范围内
     */
    isPointInToolbar(point) {
        if (!this.isToolbarVisible()) return false;
        const bounds = this.toolbarWindow.getBounds();
        return point.x >= bounds.x && point.x <= bounds.x + bounds.width &&
               point.y >= bounds.y && point.y <= bounds.y + bounds.height;
    }

    /**
     * 创建并显示弹窗（按需创建，用完即毁）
     * @param {string} text - 选中文本
     * @param {object} windowConfig - 窗口配置
     * @param {string} actionId - 功能项 ID
     * @param {number} [fontSize=14] - 基准字体大小 px
     */
    showPopup(text, windowConfig, actionId, fontSize) {
        console.log(`[showPopup] actionId=${actionId}, textLength=${text.length}, windowConfig=`, JSON.stringify(windowConfig));

        if (this.popupWindow && !this.popupWindow.isDestroyed()) {
            console.log('[showPopup] Destroying existing popup');
            this.popupWindow.destroy();
            this.popupWindow = null;
        }

        const theme = this.getCurrentTheme();
        const winWidth = windowConfig?.width || 500;
        const winHeight = windowConfig?.height || 400;

        console.log(`[showPopup] Creating BrowserWindow ${winWidth}x${winHeight}`);

        this.popupWindow = new BrowserWindow({
            width: winWidth,
            height: winHeight,
            frame: false,
            type: IS_MAC ? 'panel' : undefined,
            resizable: true,
            alwaysOnTop: true,
            skipTaskbar: true,
            show: false,
            autoHideMenuBar: true,
            webPreferences: {
                preload: path.join(__dirname, '..', '..', 'preload', 'selection-assistant.js'),
                nodeIntegration: false,
                contextIsolation: true,
            },
        });

        let posX, posY;
        if (this.toolbarWindow && !this.toolbarWindow.isDestroyed() &&
            windowConfig?.position === 'follow-toolbar') {
            const tbBounds = this.toolbarWindow.getBounds();
            console.log('[showPopup] Follow toolbar position:', tbBounds);
            const display = screen.getDisplayNearestPoint({x: tbBounds.x, y: tbBounds.y});
            const workArea = display.workArea;
            posX = Math.round(tbBounds.x + (tbBounds.width - winWidth) / 2);
            posY = Math.round(tbBounds.y + tbBounds.height + 6);
            if (posX + winWidth > workArea.x + workArea.width) {
                posX = workArea.x + workArea.width - winWidth - 6;
            } else if (posX < workArea.x) {
                posX = workArea.x + 6;
            }
            if (posY + winHeight > workArea.y + workArea.height) {
                posY = workArea.y + workArea.height - winHeight - 6;
            } else if (posY < workArea.y) {
                posY = workArea.y + 6;
            }
        } else {
            const cursorPoint = screen.getCursorScreenPoint();
            const display = screen.getDisplayNearestPoint(cursorPoint);
            const workArea = display.workArea;
            posX = Math.round(workArea.x + (workArea.width - winWidth) / 2);
            posY = Math.round(workArea.y + (workArea.height - winHeight) / 2);
        }

        console.log(`[showPopup] Position: (${posX}, ${posY})`);

        this.popupWindow.setPosition(posX, posY, false);
        this.popupWindow.setBounds({width: winWidth, height: winHeight, x: posX, y: posY});

        // ⚠ 必须先绑定 ready-to-show 再 loadURL，否则 data: URL 同步加载完成后事件可能已错过
        this.popupWindow.once('ready-to-show', () => {
            console.log('[showPopup] ready-to-show, injecting event handlers');
            // 通过 executeJavaScript 注入弹窗事件处理器（主 world，可访问 contextBridge 暴露的 selectionAPI）
            this.popupWindow.webContents.executeJavaScript(`
                document.getElementById('closeBtn').addEventListener('click', () => {
                    window.selectionAPI.closePopup();
                });
                document.getElementById('pinBtn').addEventListener('click', () => {
                    window.selectionAPI.togglePin();
                    const btn = document.getElementById('pinBtn');
                    btn.style.opacity = btn.style.opacity === '1' ? '.5' : '1';
                });
            `).catch(err => console.error('[showPopup] executeJavaScript failed:', err));

            console.log('[showPopup] ready-to-show fired, showing popup');
            this.popupWindow.showInactive();
            console.log('[showPopup] showInactive() called');
        });

        console.log('[showPopup] Loading URL...');
        this.popupWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(
            getPopupHtml(theme, text, actionId, fontSize)
        )}`);
        console.log('[showPopup] loadURL done');

        this.popupWindow.on('closed', () => {
            console.log('[showPopup] Popup closed');
            this.popupWindow = null;
        });
    }

    /**
     * 销毁所有窗口
     */
    destroy() {
        if (this.toolbarWindow && !this.toolbarWindow.isDestroyed()) {
            this.toolbarWindow.destroy();
        }
        if (this.popupWindow && !this.popupWindow.isDestroyed()) {
            this.popupWindow.destroy();
        }
        this.toolbarWindow = null;
        this.popupWindow = null;
    }
}

module.exports = {SelectionWindows};
