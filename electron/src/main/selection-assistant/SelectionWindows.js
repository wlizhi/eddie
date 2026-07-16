/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手窗口管理 — 工具栏窗口（常驻）+ 弹窗窗口（按需创建）
 */

const {BrowserWindow, screen, nativeTheme, app} = require('electron');
const path = require('path');
const fs = require('fs');
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
function getCloseMargin(fontSize) { return Math.max(2, Math.round(fontSize * 0.21)); }
function getToolbarGap(fontSize) { return Math.max(2, Math.round(fontSize * 0.14)); }

// 全局默认 CSS font-family（系统默认字体）
const DEFAULT_FONT_FAMILY = "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans SC', sans-serif";

// 度量文字宽度
function measureText(s, fontSize) {
    const cjkW = Math.round(fontSize * 0.86); // 中文字宽 ≈ 0.86em
    const asciiW = Math.round(fontSize * 0.5); // ASCII 字宽 ≈ 0.5em
    let w = 0;
    for (const ch of s) w += ch.charCodeAt(0) > 127 ? cjkW : asciiW;
    return w;
}

// 根据功能项动态计算工具栏宽度
function calcToolbarWidth(items, fontSize, toolbarStyle) {
    const enabled = items.filter(i => i.enabled).sort((a, b) => a.order - b.order);
    if (!enabled.length) return 120;

    const padH = getContainerPadH(fontSize);
    const btnPad = getBtnPad(fontSize);
    const gap = getToolbarGap(fontSize);
    const iconW = 14;
    const gapIconText = Math.max(2, Math.round(fontSize * 0.15));
    const dividerW = 1;
    const closeBtn = getCloseBtnSize(fontSize);
    const closeMargin = getCloseMargin(fontSize);
    const showLabel = toolbarStyle !== 'compact';

    let actionsW = 0;
    enabled.forEach((item, idx) => {
        const textW = showLabel ? measureText(item.label || '', fontSize) : 0;
        const gapW = showLabel ? gapIconText : 0;
        actionsW += btnPad + iconW + gapW + textW + btnPad;
        if (idx < enabled.length - 1) actionsW += dividerW + gap;
    });

    return padH + actionsW + gap + closeBtn + closeMargin + padH;
}

// ============================================================
// 工具栏 HTML
// ============================================================
function getToolbarHtml(theme, text, items, fontSize, fontFamily, toolbarStyle) {
    const textEscaped = text
        .replace(/&/g, '&')
        .replace(/</g, '<')
        .replace(/>/g, '>')
        .replace(/"/g, '"');

    const showLabel = toolbarStyle !== 'compact';
    const itemsHtml = items
        .filter(i => i.enabled)
        .sort((a, b) => a.order - b.order)
        .map((i, idx, arr) => {
            const label = i.label || '';
            const btnContent = showLabel ? `${i.icon || ''} ${label}` : `${i.icon || ''}`;
            const btn = `<button class="action-btn" data-id="${i.id}">${btnContent}</button>`;
            return idx < arr.length - 1 ? btn + '<div class="btn-divider"></div>' : btn;
        })
        .join('');

    const fontFamilyCSS = fontFamily || DEFAULT_FONT_FAMILY;

    const toolbarH = calcToolbarHeight(fontSize);
    const btnH = getBtnHeight(fontSize);
    const btnPad = getBtnPad(fontSize);
    const btnFs = getBtnFontSize(fontSize);
    const dividerH = getDividerH(fontSize);
    const closeSz = getCloseBtnSize(fontSize);
    const closeMargin = getCloseMargin(fontSize);
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
    font-family:${fontFamilyCSS};
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
    margin-right:${closeMargin}px;
    transition:background .12s;
}
.close-btn:hover{background:${theme.hover};color:${theme.textPrimary}}
</style>
</head>
<body>
<div class="toolbar">
    <div class="actions">${itemsHtml}</div>
    <button class="close-btn" id="closeBtn"><svg width="${Math.round(fontSize * 0.86)}" height="${Math.round(fontSize * 0.86)}" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><line x1="4" y1="4" x2="10" y2="10"/><line x1="10" y1="4" x2="4" y2="10"/></svg></button>
</div>
</body>
</html>`;
}

// ============================================================
// 弹窗 URL 生成（Vue 构建的 popup.html，由后端 static/ 提供服务）
// ============================================================

/**
 * 获取弹窗页面 URL
 * 开发环境使用 Vite dev server，生产环境使用后端静态资源
 */
function getPopupUrl() {
    if (process.env.NODE_ENV === 'development') {
        return 'http://localhost:5173/popup.html';
    }
    return 'http://localhost:11520/popup.html';
}

// ============================================================
// 窗口管理类
// ============================================================
class SelectionWindows {
    constructor() {
        this.toolbarWindow = null;
        /** 所有弹窗窗口列表（允许多弹窗共存） */
        this.popupWindows = [];
        // 持久化弹窗尺寸的文件路径
        this._popupSizePath = path.join(app.getPath('userData'), 'popup-size.json');
    }

    /**
     * 获取当前主题色（从 theme-persist 内存缓存读取，无磁盘 IO）
     */
    getCurrentTheme() {
        return getTheme(nativeTheme.shouldUseDarkColors);
    }

    /**
     * 从列表中移除已关闭的窗口
     */
    _removeWindow(win) {
        const idx = this.popupWindows.indexOf(win);
        if (idx > -1) this.popupWindows.splice(idx, 1);
        // 清理 blur/hide 监听
        if (win._blurHandler) {
            win.removeListener('blur', win._blurHandler);
        }
        if (win._hideHandler) {
            win.removeListener('hide', win._hideHandler);
        }
    }

    /**
     * 向所有弹窗发送 IPC 消息
     */
    sendToAllPopups(channel, ...args) {
        for (const win of this.popupWindows) {
            if (!win.isDestroyed()) {
                try {
                    win.webContents.send(channel, ...args);
                } catch { /* ignore */ }
            }
        }
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
     * @param {string} [fontFamily='system'] - 字体类型 ID
     */
    showToolbar(text, items, position, fontSize, fontFamily, toolbarStyle) {
        const win = this.ensureToolbar();
        const theme = this.getCurrentTheme();

        const fs = fontSize || 14;
        const ff = fontFamily || DEFAULT_FONT_FAMILY;
        const style = toolbarStyle || 'default';
        const toolbarHeight = calcToolbarHeight(fs);
        const toolbarWidth = calcToolbarWidth(items, fs, style);

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
                // 注入 font-family CSS（data: URL 中 CSS font-family 不生效，需用 insertCSS 注入）
                const fontCSS = `body, .action-btn { font-family: ${ff} !important; }`;
                win.webContents.insertCSS(fontCSS).catch(err => console.error('[Toolbar] insertCSS failed:', err));

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
            getToolbarHtml(theme, text, items, fs, ff, style)
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
     * 创建弹窗窗口
     * @param {object} windowConfig - 窗口配置
     * @returns {BrowserWindow}
     */
    _createPopupWindow(windowConfig) {
        const winWidth = windowConfig?.width || 500;
        const winHeight = windowConfig?.height || 400;

        console.log(`[showPopup] Creating BrowserWindow ${winWidth}x${winHeight}`);

        const win = new BrowserWindow({
            width: winWidth,
            height: winHeight,
            frame: false,
            type: IS_MAC ? 'panel' : undefined,
            resizable: true,
            alwaysOnTop: windowConfig?.alwaysOnTop ?? false,
            skipTaskbar: true,
            show: false,
            autoHideMenuBar: true,
            opacity: (windowConfig?.opacity ?? 100) / 100,
            webPreferences: {
                preload: path.join(__dirname, '..', '..', 'preload', 'selection-assistant.js'),
                nodeIntegration: false,
                contextIsolation: true,
            },
        });

        // 转发渲染进程日志到主进程控制台，用于调试
        win.webContents.on('console-message', (_e, level, message) => {
            console.log(`[Popup:renderer] ${message}`);
        });

        // 窗口关闭时自动从列表中移除
        win.once('closed', () => {
            console.log('[showPopup] Popup closed');
            this._removeWindow(win);
        });

        return win;
    }

    /**
     * 创建并显示弹窗
     *
     * 每次创建新窗口（不销毁已有弹窗），允许多弹窗共存。
     * 每个窗口的置顶状态、blur 监听等状态直接绑定到 win 实例上。
     *
     * 置顶行为说明：
     *   - 全局配置 `window.alwaysOnTop` 仅控制新建弹窗时的默认值
     *   - 用户点击置顶按钮切换的是当前窗口的运行时状态，不影响全局配置
     *   - 置顶状态下，自动关闭（autoClose）失效（失焦不关闭）
     *
     * @param {string} text - 选中文本
     * @param {object} windowConfig - 窗口配置
     * @param {string} actionId - 功能项 ID
     * @param {number} [fontSize=14] - 基准字体大小 px
     * @param {function} [onShown] - 弹窗显示后的回调（用于延迟隐藏工具栏）
     */
    showPopup(text, windowConfig, actionId, fontSize, onShown) {
        console.log(`[showPopup] actionId=${actionId}, textLength=${text.length}, windowConfig=`, JSON.stringify(windowConfig));

        // 创建新窗口（不销毁旧窗口）
        const win = this._createPopupWindow(windowConfig);
        const theme = this.getCurrentTheme();
        const winWidth = windowConfig?.width || 500;
        const winHeight = windowConfig?.height || 400;

        // 追踪窗口
        this.popupWindows.push(win);

        // 初始化窗口级别状态
        win._isPinned = windowConfig?.alwaysOnTop ?? false;
        win._autoCloseConfig = windowConfig?.autoClose ?? false;
        win._blurHandler = null;
        win._hideHandler = null;

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

        win.setPosition(posX, posY, false);
        win.setBounds({width: winWidth, height: winHeight, x: posX, y: posY});

        // ============================================================
        // 应用窗口配置（alwaysOnTop、透明度、失焦关闭、记住大小）
        // ============================================================

        // alwaysOnTop：使用初始化后的置顶状态
        win.setAlwaysOnTop(win._isPinned);

        // 透明度
        win.setOpacity((windowConfig?.opacity ?? 100) / 100);

        // 失焦自动关闭：仅当 autoClose 启用且当前未置顶时才注册
        // macOS：panel 窗口在 app 失活时被直接隐藏，不触发 blur，用 hide 兜底
        if (windowConfig?.autoClose && !win._isPinned) {
            const autoCloseFn = () => {
                setTimeout(() => {
                    if (!win.isDestroyed() && !win._isPinned) {
                        this.closePopup(win);
                    }
                }, 200);
            };
            win._blurHandler = autoCloseFn;
            win._hideHandler = autoCloseFn;
            win.on('blur', autoCloseFn);
            win.on('hide', autoCloseFn);
        }

        // 记住窗口大小（仅记录最新窗口的大小）
        if (windowConfig?.rememberSize) {
            const saved = this._loadPopupSize();
            if (saved) {
                win.setSize(saved.width, saved.height);
            }
            win.on('resize', () => {
                if (!win.isDestroyed()) {
                    this._savePopupSize(win.getSize());
                }
            });
        }

        // 每次新建窗口，ready-to-show 正常触发
        win.once('ready-to-show', () => {
            console.log('[showPopup] ready-to-show fired, showing popup');
            win.showInactive();

            // 通知渲染进程初始置顶状态（更新图标）
            win.webContents.send('selection:pin-changed', win._isPinned);

            // 弹窗已可见时才回调（如：隐藏工具栏），避免中间出现无窗口空白期
            if (typeof onShown === 'function') {
                onShown();
            }
        });

        console.log('[showPopup] Loading URL:', getPopupUrl());
        win.loadURL(getPopupUrl());
        console.log('[showPopup] loadURL done');
    }

    /**
     * 关闭指定弹窗（销毁窗口释放内存）
     * macOS：使用 focus-guard 技巧阻止窗口销毁时应用被激活
     *   - 所有可见窗口设为不可聚焦 → destroy → 50ms 后恢复可聚焦
     *   参考 CherryStudio: quirks.ts beginMacFocusGuard / endMacFocusGuard
     *
     * @param {BrowserWindow} [win] - 要关闭的弹窗，不传则关闭所有弹窗
     */
    closePopup(win) {
        // 不传参 → 关闭所有弹窗
        if (!win) {
            for (const w of [...this.popupWindows]) {
                this.closePopup(w);
            }
            return;
        }

        if (win.isDestroyed()) return;

        // macOS focus-guard
        let focusGuard = null;
        if (IS_MAC) {
            focusGuard = [];
            for (const w of require('electron').BrowserWindow.getAllWindows()) {
                if (!w.isDestroyed() && w.isVisible() && w.isFocusable()) {
                    focusGuard.push(w);
                    w.setFocusable(false);
                }
            }
        }

        // 移除 blur/hide 监听
        if (win._blurHandler) {
            win.removeListener('blur', win._blurHandler);
            win._blurHandler = null;
        }
        if (win._hideHandler) {
            win.removeListener('hide', win._hideHandler);
            win._hideHandler = null;
        }

        win.destroy();

        // 50ms 后恢复焦点能力
        if (IS_MAC && focusGuard) {
            setTimeout(() => {
                for (const w of focusGuard) {
                    if (!w.isDestroyed()) w.setFocusable(true);
                }
            }, 50);
        }
    }

    /**
     * 切换指定弹窗的置顶状态
     *
     * 仅改变当前窗口的运行时状态，不影响全局配置（`window.alwaysOnTop` 默认值）。
     * 置顶后自动关闭（autoClose）失效，点击右上角关闭按钮仍可正常关闭。
     * 通知渲染进程更新置顶图标。
     *
     * @param {BrowserWindow} win
     */
    togglePopupPin(win) {
        if (!win || win.isDestroyed()) return;

        const newState = !win.isAlwaysOnTop();
        win.setAlwaysOnTop(newState);
        win._isPinned = newState;

        console.log(`[Selection] Popup pin toggled: ${newState}`);

        if (newState) {
            // 置顶 → 移除 blur/hide 监听，autoClose 失效
            if (win._blurHandler) {
                win.removeListener('blur', win._blurHandler);
                win._blurHandler = null;
            }
            if (win._hideHandler) {
                win.removeListener('hide', win._hideHandler);
                win._hideHandler = null;
            }
            console.log('[Selection] autoClose disabled (pinned)');
        } else {
            // 取消置顶 → 如果配置了 autoClose，恢复 blur/hide 监听
            if (win._autoCloseConfig) {
                const autoCloseFn = () => {
                    setTimeout(() => {
                        if (!win.isDestroyed() && !win._isPinned) {
                            this.closePopup(win);
                        }
                    }, 200);
                };
                win._blurHandler = autoCloseFn;
                win._hideHandler = autoCloseFn;
                win.on('blur', autoCloseFn);
                win.on('hide', autoCloseFn);
                console.log('[Selection] autoClose re-enabled (unpinned)');
            }
        }

        // 通知渲染进程更新置顶图标
        win.webContents.send('selection:pin-changed', newState);
    }

    /**
     * 销毁所有窗口
     */
    destroy() {
        if (this.toolbarWindow && !this.toolbarWindow.isDestroyed()) {
            this.toolbarWindow.destroy();
        }
        for (const win of [...this.popupWindows]) {
            if (!win.isDestroyed()) win.destroy();
        }
        this.toolbarWindow = null;
        this.popupWindows = [];
    }

    // ============================================================
    // 弹窗尺寸持久化
    // ============================================================

    _loadPopupSize() {
        try {
            if (fs.existsSync(this._popupSizePath)) {
                const raw = fs.readFileSync(this._popupSizePath, 'utf-8');
                return JSON.parse(raw);
            }
        } catch { /* ignore */ }
        return null;
    }

    _savePopupSize(size) {
        try {
            const dir = path.dirname(this._popupSizePath);
            if (!fs.existsSync(dir)) {
                fs.mkdirSync(dir, {recursive: true});
            }
            fs.writeFileSync(this._popupSizePath, JSON.stringify({width: size[0], height: size[1]}), 'utf-8');
        } catch { /* ignore */ }
    }
}

module.exports = {SelectionWindows};
module.exports = {SelectionWindows};

