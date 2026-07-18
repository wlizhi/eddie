/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手窗口管理 — 工具栏窗口（常驻）+ 弹窗窗口（按需创建）
 */

const {BrowserWindow, screen, nativeTheme, app} = require('electron');
const path = require('path');
const {getTheme} = require('../services/theme-persist');
const {IS_MAC} = require('../utils/platform');
const {calcToolbarHeight, calcToolbarWidth} = require('./toolbar/toolbar-dimensions');
const {getToolbarHtml} = require('./toolbar/toolbar-template');
const {withFocusGuard} = require('./utils/focus-guard');
const {loadPopupSize, savePopupSize} = require('./popups/popup-size-persist');

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

        this.toolbarWindow.webContents.on('crashed', () => {
            console.error('[Toolbar] WebContents crashed!');
        });

        this.toolbarWindow.webContents.on('did-fail-load', (_e, code, desc) => {
            console.error(`[Toolbar] Failed to load: ${code} ${desc}`);
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
        const ff = fontFamily || "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans SC', sans-serif";
        const style = toolbarStyle || 'default';
        const toolbarHeight = calcToolbarHeight(fs, style);
        const toolbarWidth = calcToolbarWidth(items, fs, style);

        // 先用 calcToolbarWidth 计算初始宽度（与最终值接近，减少闪烁），
        // 加载后通过测量实际渲染宽度做最终修正
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
                // 注入 font-family CSS（data: URL 中 CSS font-family 不生效，需用 insertCSS 注入）
                const fontCSS = `body, .action-btn { font-family: ${ff} !important; }`;
                win.webContents.insertCSS(fontCSS).catch(() => {});

                // 注入事件处理器 + 拖拽 + 测量实际内容宽度并修正
                win.webContents.executeJavaScript(`
                    document.getElementById('closeBtn').addEventListener('click', () => {
                        window.selectionAPI.hideToolbar();
                    });
                    document.querySelectorAll('.action-btn').forEach(btn => {
                        btn.addEventListener('click', () => {
                            window.selectionAPI.sendAction(btn.dataset.id);
                        });
                    });
                    // 窗口拖拽（替代 -webkit-app-region）
                    (function() {
                        var toolbar = document.querySelector('.toolbar');
                        var dragging = false, startX, startY;
                        toolbar.addEventListener('mousedown', function(e) {
                            if (e.target.closest('.action-btn') || e.target.closest('.close-btn')) return;
                            dragging = true;
                            startX = e.screenX;
                            startY = e.screenY;
                            e.preventDefault();
                        });
                        document.addEventListener('mousemove', function(e) {
                            if (!dragging) return;
                            var dx = e.screenX - startX;
                            var dy = e.screenY - startY;
                            if (dx !== 0 || dy !== 0) {
                                window.selectionAPI.dragMove(dx, dy);
                                startX = e.screenX;
                                startY = e.screenY;
                            }
                        });
                        document.addEventListener('mouseup', function() {
                            dragging = false;
                        });
                    })();
                    var tb = document.querySelector('.toolbar');
                    var s = getComputedStyle(tb);
                    var pl = parseFloat(s.paddingLeft) || 0;
                    var pr = parseFloat(s.paddingRight) || 0;
                    // columnGap 可能返回 "2px"，gap 可能返回 "0px 2px"(row+column)，取最后一项
                    var gv = (s.columnGap || s.gap || '0px').trim().split(/\s+/);
                    var cg = parseFloat(gv[gv.length - 1]) || 0;
                    // 累加所有直接子元素的 offsetWidth + 子元素之间的 gap
                    // +1 补偿子像素取整误差（offsetWidth 取整后累加可能比实际少 1px）
                    var sum = 0, children = tb.children;
                    for (var i = 0; i < children.length; i++) {
                        sum += children[i].offsetWidth;
                        if (i < children.length - 1) sum += cg;
                    }
                    pl + sum + pr + 1;
                `).then(actualWidth => {
                    if (win.isDestroyed() || typeof actualWidth !== 'number') return;
                    var newWidth = Math.ceil(actualWidth);
                    // 偏差超过 1px 才修正，避免无意义抖动
                    if (Math.abs(newWidth - toolbarWidth) > 1) {
                        var newX = Math.round(Math.max(workArea.x, Math.min(
                            position.x - newWidth / 2,
                            workArea.x + workArea.width - newWidth
                        )));
                        win.setBounds({width: newWidth, height: toolbarHeight, x: newX, y});
                    }
                    win.showInactive();
                }).catch(function() {
                    win.showInactive();
                });
            }
        });
        win.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(
            getToolbarHtml(theme, text, items, fs, ff, style)
        )}`);
    }

    /**
     * 隐藏工具栏
     * macOS：使用 focus-guard 防止隐藏工具栏时主窗口被前置
     * 注意：弹窗窗口不参与 focusGuard，避免触发 blur → autoClose 导致弹窗意外关闭
     */
    hideToolbar() {
        withFocusGuard(this.popupWindows, () => {
            if (this.toolbarWindow && !this.toolbarWindow.isDestroyed()) {
                this.toolbarWindow.hide();
            }
        });
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
     * 是否有弹窗处于可见状态
     * 用于抑制工具栏重复弹出：弹窗可见时，鼠标点击弹窗内部不应唤出工具栏
     */
    isAnyPopupVisible() {
        for (const win of this.popupWindows) {
            if (!win.isDestroyed() && win.isVisible()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建弹窗窗口
     * @param {object} windowConfig - 窗口配置
     * @returns {BrowserWindow}
     */
    _createPopupWindow(windowConfig) {
        const winWidth = windowConfig?.width || 500;
        const winHeight = windowConfig?.height || 400;

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

        // 窗口关闭时自动从列表中移除
        win.once('closed', () => {
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
            win.showInactive();

            // 通知渲染进程初始置顶状态（更新图标）
            win.webContents.send('selection:pin-changed', win._isPinned);

            // 弹窗已可见时才回调（如：隐藏工具栏），避免中间出现无窗口空白期
            if (typeof onShown === 'function') {
                onShown();
            }
        });

        win.loadURL(getPopupUrl());
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

        withFocusGuard(this.popupWindows, () => {
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
        });
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
    // 弹窗尺寸持久化（委托给 popup-size-persist 工具）
    // ============================================================

    _loadPopupSize() {
        return loadPopupSize(this._popupSizePath);
    }

    _savePopupSize(size) {
        savePopupSize(this._popupSizePath, size);
    }
}

module.exports = {SelectionWindows};

