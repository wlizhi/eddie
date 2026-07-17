/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手核心服务 — 管理 selection-hook 生命周期、事件处理、启用/禁用控制
 */

const {app, screen, systemPreferences, nativeTheme} = require('electron');
const http = require('http');
const {IS_MAC, IS_WIN, IS_LINUX} = require('../utils/platform');
const {SELECTION_ASSISTANT_DEFAULTS, FALLBACK_THEMES} = require('./config');
const {SelectionWindows} = require('./SelectionWindows');
const {getTheme} = require('../services/theme-persist');

class SelectionService {
    constructor() {
        this.selectionHook = null;
        this.initStatus = false;

        this.config = {...SELECTION_ASSISTANT_DEFAULTS};

        this.windows = new SelectionWindows();

        this._activated = false;

        this._popupData = null;

        this._clickCount = 0;
        this._clickResetTimer = null;

        this._selectionUpdateTimer = null;

        this._pendingTextUpdate = false;

        this._lastMouseDownTime = 0;

        this._lastSelectionProgram = '';
    }

    getPopupData() {
        return this._popupData;
    }

    // ============================================================
    // 生命周期
    // ============================================================

    init() {
        this.windows.ensureToolbar();
    }

    activate() {
        if (this._activated) return;

        try {
            if (!this.initStatus) {
                this._loadNativeModule();
            }

            if (IS_MAC) {
                const trusted = systemPreferences.isTrustedAccessibilityClient(false);
                if (!trusted) {
                    systemPreferences.isTrustedAccessibilityClient(true);
                }
            }

            this.selectionHook.on('text-selection', this._onTextSelection);
            this.selectionHook.on('mouse-up', this._onGlobalMouseUp);
            this.selectionHook.on('mouse-down', this._onGlobalMouseDown);
            this.selectionHook.on('error', () => {});

            if (!this.selectionHook.start({debug: false})) {
                return;
            }

            this._activated = true;
        } catch (err) {}
    }

    deactivate() {
        if (!this._activated) return;

        try {
            if (this.selectionHook) {
                this.selectionHook.stop();
                this.selectionHook.removeAllListeners();
            }
            this.windows.hideToolbar();
            this._activated = false;
        } catch (err) {}
    }

    destroy() {
        this.deactivate();
        if (this.selectionHook) {
            try {
                this.selectionHook.cleanup();
            } catch (err) {}
            this.selectionHook = null;
            this.initStatus = false;
        }
        this.windows.destroy();
    }

    // ============================================================
    // 配置管理
    // ============================================================

    updateConfig(config) {
        if (!config) return;
        this._mergeConfig(this.config, config);

        if (this.config.enabled) {
            this.activate();
        } else {
            this.deactivate();
        }
    }

    getConfig() {
        return {...this.config};
    }

    isActivated() {
        return this._activated;
    }

    async refreshConfig() {
        const CONFIG_KEY = 'SELECTION_ASSISTANT_CONFIG';
        const BACKEND_URL = 'http://localhost:11520/api/settings/configs';

        try {
            const configs = await new Promise((resolve, reject) => {
                http.get(BACKEND_URL, (res) => {
                    let data = '';
                    res.on('data', chunk => data += chunk);
                    res.on('end', () => {
                        try { resolve(JSON.parse(data)); } catch (e) { reject(e); }
                    });
                }).on('error', reject);
            });

            const raw = configs.data?.[CONFIG_KEY];
            if (raw) {
                const parsed = JSON.parse(raw);
                this._mergeConfig(this.config, parsed);
                this._syncFeaturesToToolbarItems();
            }

            if (this.config.enabled) {
                if (!this._activated) this.activate();
            } else {
                if (this._activated) this.deactivate();
            }

            if (this.windows.isToolbarVisible() && this._cachedText) {
                this._showToolbar(this._cachedText, this._lastSelectionData);
            }
        } catch (err) {}

        this.windows.sendToAllPopups('selection:settings-changed');
    }

    // ============================================================
    // 内部方法
    // ============================================================

    _loadNativeModule() {
        try {
            const SelectionHook = require('selection-hook');
            this.selectionHook = new SelectionHook();
            this.initStatus = true;
        } catch (err) {
            this.config.enabled = false;
        }
    }

    _onTextSelection = (data) => {
        if (!data) return;

        const text = (data.text || '').trim();
        if (!text) return;
        this._lastSelectionData = data;
        this._lastSelectionProgram = data.programName || '';
        this._pendingTextUpdate = true;

        if (this.windows.isToolbarVisible()) {
            if (this._clickCount <= 1) {
                if (text && text !== this._cachedText) {
                    this._cachedText = text;
                    this.windows.updateToolbarText(text);
                }
            } else {
                clearTimeout(this._selectionUpdateTimer);
                this._selectionUpdateTimer = setTimeout(() => {
                    const current = this.selectionHook.getCurrentSelection();
                    const finalText = (current && current.text) ? current.text.trim() : '';
                    if (finalText && finalText !== this._cachedText) {
                        this._cachedText = finalText;
                        this.windows.updateToolbarText(finalText);
                    }
                }, 50);
            }
            return;
        }

        if (this.windows.isAnyPopupVisible() && text === this._cachedText) {
            return;
        }

        if (this._clickCount >= 2) {
            clearTimeout(this._selectionUpdateTimer);
            this._selectionUpdateTimer = setTimeout(() => {
                const current = this.selectionHook.getCurrentSelection();
                const finalText = (current && current.text) ? current.text.trim() : '';
                if (!finalText) return;
                this._cachedText = finalText;
                if (this._toolbarLoading) {
                    this._pendingText = finalText;
                    this._pendingData = data;
                } else {
                    this._showToolbar(finalText, data);
                }
            }, 50);
            return;
        }

        this._cachedText = text;

        if (this._toolbarLoading) {
            this._pendingText = text;
            this._pendingData = data;
            return;
        }

        this._showToolbar(text, data);
    };

    _showToolbar(text, data) {
        this._toolbarLoading = true;

        let refPoint = this._getReferencePoint(data);
        if (IS_WIN || IS_LINUX) {
            refPoint = screen.screenToDipPoint(refPoint);
        }
        refPoint = {x: Math.round(refPoint.x), y: Math.round(refPoint.y)};

        this.windows.showToolbar(text, this.config.toolbar.items, refPoint, this.config.fontSize, this.config.fontFamily || "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans SC', sans-serif", this.config.toolbar.style);

        const win = this.windows.toolbarWindow;
        if (win && !win.isDestroyed()) {
            win.webContents.once('did-finish-load', () => {
                this._toolbarLoading = false;
                if (this._pendingText && this._pendingText !== this._cachedText) {
                    const pd = this._pendingData;
                    this._pendingText = null;
                    this._pendingData = null;
                    this._onTextSelection(pd);
                }
            });
        }
    }

    _onGlobalMouseUp = (event) => {
        if (!event || event.button !== 0) return;

        if (this._pendingTextUpdate) {
            this._pendingTextUpdate = false;

            if (this._clickCount >= 3) {
                setTimeout(() => {
                    if (!this.selectionHook) return;
                    const current = this.selectionHook.getCurrentSelection();
                    const text = (current && current.text) ? current.text.trim() : '';
                    if (!text) return;
                    this._applySelectionText(text);
                }, 80);
                return;
            }

            const current = this.selectionHook.getCurrentSelection();
            const text = (current && current.text) ? current.text.trim() : '';

            if (!text) {
                if (!this._cachedText) return;
                this._applySelectionText(this._cachedText);
                return;
            }

            this._applySelectionText(text);
            return;
        }

        if (this._cachedText) {
            this._applySelectionText(this._cachedText);
            return;
        }

        let lastMouseDownDuration = Date.now() - this._lastMouseDownTime;
        if (this._clickCount >= 2 || lastMouseDownDuration > 300) {
            const current = this.selectionHook.getCurrentSelection();
            if (!current) return;
            const text = (current.text || '').trim();
            const program = current.programName || '';
            if (this._lastSelectionProgram && program !== this._lastSelectionProgram) {
                return;
            }
            if (text) {
                this._applySelectionText(text);
                return;
            }
        }
    };

    _applySelectionText(text) {
        if (!text) return;

        if (this.windows.isAnyPopupVisible() && text === this._cachedText) {
            return;
        }

        this._cachedText = text;

        if (this.windows.isToolbarVisible()) {
            this.windows.updateToolbarText(text);
            return;
        }

        if (this._toolbarLoading) {
            this._pendingText = text;
            this._pendingData = this._lastSelectionData;
            return;
        }

        this._showToolbar(text, this._lastSelectionData);
    }

    _onGlobalMouseDown = (event) => {
        if (!event || event.button !== 0) return;

        this._clickCount = (this._clickCount || 0) + 1;
        clearTimeout(this._clickResetTimer);
        this._clickResetTimer = setTimeout(() => {
            this._clickCount = 0;
        }, 500);

        this._lastMouseDownTime = Date.now();

        if (!this.windows.isToolbarVisible()) return;

        const cursor = screen.getCursorScreenPoint();
        const point = {x: Math.round(cursor.x), y: Math.round(cursor.y)};

        if (this.windows.isPointInToolbar(point)) {
            return;
        }

        this.windows.hideToolbar();
        this._cachedText = '';
    };

    _getReferencePoint(_data) {
        const cursor = screen.getCursorScreenPoint();
        return {x: Math.round(cursor.x), y: Math.round(cursor.y + 16)};
    }

    _syncFeaturesToToolbarItems() {
        const features = this.config.features;
        const items = this.config.toolbar.items;
        if (!features || !features.length || !items) return;

        for (const item of items) {
            const feature = features.find(f => f.id === item.id);
            if (feature) {
                item.enabled = feature.enabled;
                item.order = feature.order;
            }
        }
        items.sort((a, b) => a.order - b.order);
    }

    _mergeConfig(target, source) {
        for (const key of Object.keys(source)) {
            if (source[key] !== null && typeof source[key] === 'object' && !Array.isArray(source[key])) {
                if (!target[key] || typeof target[key] !== 'object') {
                    target[key] = {};
                }
                this._mergeConfig(target[key], source[key]);
            } else {
                target[key] = source[key];
            }
        }
    }

    handleAction(actionId) {
        const theme = getTheme(nativeTheme.shouldUseDarkColors);
        this._popupData = {
            action: actionId,
            text: this._cachedText || '',
            fontSize: this.config.fontSize || 14,
            theme: theme,
            targetLang: this.config.translate?.targetLang || 'zh-CN',
        };

        this.windows.showPopup(
            this._cachedText || '',
            this.config.window,
            actionId,
            this.config.fontSize,
            () => {
                this.windows.hideToolbar();
                this._cachedText = '';
            }
        );
    }
}

const selectionService = new SelectionService();

module.exports = {selectionService};
