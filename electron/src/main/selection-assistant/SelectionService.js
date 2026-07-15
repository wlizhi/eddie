/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手核心服务 — 管理 selection-hook 生命周期、事件处理、启用/禁用控制
 */

const {app, screen, systemPreferences} = require('electron');
const {IS_MAC, IS_WIN, IS_LINUX} = require('../utils/platform');
const {SELECTION_ASSISTANT_DEFAULTS} = require('./config');
const {SelectionWindows} = require('./SelectionWindows');

class SelectionService {
    constructor() {
        this.selectionHook = null;
        this.initStatus = false;

        // 当前配置（可由后端 IPC 覆盖）
        this.config = {...SELECTION_ASSISTANT_DEFAULTS};

        // 窗口管理器
        this.windows = new SelectionWindows();

        // 是否已激活
        this._activated = false;
    }

    // ============================================================
    // 生命周期
    // ============================================================

    /**
     * 初始化划词助手（应由主进程在 app.whenReady 后调用）
     * 注意：此时不激活 hook，等待后端下发配置后再激活
     */
    init() {
        console.log('[Selection] Initializing selection assistant...');
        // 仅初始化窗口管理器（延迟加载 hook，等后端配置后才加载）
        this.windows.ensureToolbar();
        console.log('[Selection] Selection assistant initialized (awaiting backend config)');
    }

    /**
     * 激活划词助手：加载 selection-hook 原生模块并启动
     */
    activate() {
        if (this._activated) return;
        console.log('[Selection] Activating...');

        try {
            // 1. 延迟加载 selection-hook 原生模块
            if (!this.initStatus) {
                this._loadNativeModule();
            }

            // 2. macOS 辅助功能权限检查
            if (IS_MAC) {
                const trusted = systemPreferences.isTrustedAccessibilityClient(false);
                if (!trusted) {
                    console.warn('[Selection] macOS accessibility permission not granted, requesting...');
                    systemPreferences.isTrustedAccessibilityClient(true);
                    // 即使未授权也继续，用户授权后再次选中即可生效
                }
            }

            // 3. 注册 text-selection 事件监听（标记选中信号 + 缓存坐标）
            this.selectionHook.on('text-selection', this._onTextSelection);

            // 4. 注册全局鼠标释放事件监听（选中完成后触发显示工具栏）
            this.selectionHook.on('mouse-up', this._onGlobalMouseUp);

            // 5. 注册全局鼠标按下事件监听（用于点击工具栏外部时隐藏）
            this.selectionHook.on('mouse-down', this._onGlobalMouseDown);

            // 6. 注册错误监听
            this.selectionHook.on('error', (error) => {
                console.error('[Selection] Hook error:', error);
            });

            // 8. 启动 hook
            if (!this.selectionHook.start({debug: false})) {
                console.error('[Selection] Failed to start selection hook');
                return;
            }

            this._activated = true;
            console.log('[Selection] Activated successfully');
        } catch (err) {
            console.error('[Selection] Failed to activate:', err.message);
        }
    }

    /**
     * 停用划词助手：停止 hook，清理监听器
     */
    deactivate() {
        if (!this._activated) return;
        console.log('[Selection] Deactivating...');

        try {
            if (this.selectionHook) {
                this.selectionHook.stop();
                this.selectionHook.removeAllListeners();
            }
            this.windows.hideToolbar();
            this._activated = false;
            console.log('[Selection] Deactivated');
        } catch (err) {
            console.error('[Selection] Failed to deactivate:', err.message);
        }
    }

    /**
     * 销毁：释放所有资源
     */
    destroy() {
        console.log('[Selection] Destroying...');
        this.deactivate();
        if (this.selectionHook) {
            try {
                this.selectionHook.cleanup();
            } catch (err) {
                console.error('[Selection] Cleanup error:', err.message);
            }
            this.selectionHook = null;
            this.initStatus = false;
        }
        this.windows.destroy();
        console.log('[Selection] Destroyed');
    }

    // ============================================================
    // 配置管理
    // ============================================================

    /**
     * 从后端下发配置更新
     * @param {object} config - 完整配置或部分配置
     */
    updateConfig(config) {
        if (!config) return;

        // 合并配置
        this._mergeConfig(this.config, config);

        console.log('[Selection] Config updated:', JSON.stringify(this.config, null, 2));

        // 根据 enabled 控制启用/禁用
        if (this.config.enabled) {
            this.activate();
        } else {
            this.deactivate();
        }
    }

    /**
     * 获取当前配置
     */
    getConfig() {
        return {...this.config};
    }

    /**
     * 当前是否已激活
     */
    isActivated() {
        return this._activated;
    }

    // ============================================================
    // 内部方法
    // ============================================================

    /**
     * 加载 selection-hook 原生模块
     */
    _loadNativeModule() {
        try {
            const SelectionHook = require('selection-hook');
            this.selectionHook = new SelectionHook();
            this.initStatus = true;
            console.log('[Selection] Native module loaded');
        } catch (err) {
            console.error('[Selection] Failed to load selection-hook:', err.message);
            // 加载失败则禁用
            this.config.enabled = false;
        }
    }

    /**
     * 处理文本选中事件
     *
     * text-selection 和 mouse-up 都能触发工具栏显示，互作兜底：
     *   - text-selection 优先：立即显示工具栏（文本来自事件 data）
     *   - mouse-up 兜底：getCurrentSelection() 获取最终选中文本
     */
    _onTextSelection = (data) => {
        if (!data) return;

        // 更新信号（供 mouse-up 兜底使用）和缓存坐标
        this._lastSelectionTime = Date.now();
        this._lastSelectionData = data;

        const text = (data.text || '').trim();

        if (this.windows.isToolbarVisible()) {
            // 工具栏已显示：检测文本是否变更并更新预览
            if (text && text !== this._cachedText) {
                this._cachedText = text;
                console.log(`[Selection] Text updated: "${text.substring(0, 40)}..."`);
                this.windows.updateToolbarText(text);
            }
            return;
        }

        if (!text) return;

        this._cachedText = text;

        // 工具栏正在加载中：缓存数据，加载完成后会自动显示
        if (this._toolbarLoading) {
            this._pendingText = text;
            this._pendingData = data;
            return;
        }

        this._showToolbar(text, data);
    };

    /**
     * 显示工具栏
     */
    _showToolbar(text, data) {
        console.log(`[Selection] _showToolbar called, text="${text.substring(0, 30)}", loading=${this._toolbarLoading}, visible=${this.windows.isToolbarVisible()}`);
        this._toolbarLoading = true;

        let refPoint = this._getReferencePoint(data);
        if (IS_WIN || IS_LINUX) {
            refPoint = screen.screenToDipPoint(refPoint);
        }
        refPoint = {x: Math.round(refPoint.x), y: Math.round(refPoint.y)};

        this.windows.showToolbar(text, this.config.toolbar.items, refPoint, this.config.fontSize);

        // 加载完成后清除加载状态，如果有待处理数据则再次调用
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

    /**
     * 处理全局鼠标释放事件
     *
     * 事件时序（从原生模块源码确认）：
     *   1. text-selection → 通过 tsfn 异步排队
     *   2. mouse-up       → 同步触发（在 text-selection 之前到达 JS 层）
     *
     * 因此 mouse-up 到达时 text-selection 尚未消费，但 getCurrentSelection()
     * 直接调用原生模块的 AXAPI 读取系统当前选中文本，返回的是**鼠标释放时**
     * 的最终选中内容（而非 text-selection 缓存的中间态文本）。
     *
     * 处理策略：
     *   - 兜底路径（工具栏不可见）：用 getCurrentSelection() 获取最新文本
     *   - 工具栏已可见场景（如双击→三击过程中的 mouse-up 覆盖）：同上
     */
    _onGlobalMouseUp = (event) => {
        if (!event || event.button !== 0) return;

        // 工具栏已显示：强制用 getCurrentSelection() 覆盖最新文本
        // 处理三击场景：第 3 击 mouse-up 时，getCurrentSelection() 返回段落文本
        if (this.windows.isToolbarVisible()) {
            const current = this.selectionHook.getCurrentSelection();
            const text = (current && current.text) ? current.text.trim() : '';
            if (text && text !== this._cachedText) {
                this._cachedText = text;
                console.log(`[Selection] Text updated via mouse-up: "${text.substring(0, 40)}..."`);
                this.windows.updateToolbarText(text);
            }
            return;
        }

        // 工具栏未显示：需要最近有 text-selection 信号才处理
        if (!this._lastSelectionTime || Date.now() - this._lastSelectionTime > 1000) {
            return;
        }

        // 用 getCurrentSelection() 获取最终文本（比缓存文本更可靠）
        const current = this.selectionHook.getCurrentSelection();
        const text = (current && current.text) ? current.text.trim() : '';
        const finalText = text || this._cachedText || '';
        if (!finalText) return;

        // 更新缓存确保一致性
        this._cachedText = finalText;

        if (this._toolbarLoading) {
            this._pendingText = finalText;
            this._pendingData = this._lastSelectionData;
            return;
        }

        this._showToolbar(finalText, this._lastSelectionData);
    };

    /**
     * 处理全局鼠标按下事件
     *
     * 职责：
     *   1. 三击场景预隐藏：用户双击后工具栏可见，第三击 mouse-down 时主动隐藏，
     *      等待 mouse-up/text-selection 获取段落文本重新显示。
     *   2. 点击工具栏外部时隐藏（受 hideOnClickOutside 配置控制）
     *
     * 注意：点击工具栏自身不处理，让按钮事件正常响应。
     */
    _onGlobalMouseDown = (event) => {
        if (!event || event.button !== 0) return;

        // 工具栏不可见 → 不处理
        if (!this.windows.isToolbarVisible()) return;

        // 使用 screen.getCursorScreenPoint() 获取屏幕坐标，与 getBounds() 坐标系一致
        // ⚠ 不能用 event.x/event.y：原生 hook 使用 macOS Quartz 坐标（Y轴左下原点），
        //   而 getBounds() 使用 Electron 屏幕坐标（Y轴左上原点），坐标系不一致会导致 isPointInToolbar 永远返回 false
        const cursor = screen.getCursorScreenPoint();
        const point = {x: Math.round(cursor.x), y: Math.round(cursor.y)};

        // 点击在工具栏自身范围内 → 不处理，让按钮事件正常响应
        if (this.windows.isPointInToolbar(point)) {
            console.log('[Selection] mousedown INSIDE toolbar, allowing click through');
            return;
        }

        console.log('[Selection] mousedown OUTSIDE toolbar, hiding toolbar', point, 'bounds:', this.windows.toolbarWindow?.getBounds());

        // 场景 ④ 三击预隐藏：点击在工具栏外部（即文本区域）→ 隐藏工具栏
        // 后续 mouse-up 或 text-selection 会用 getCurrentSelection() 获取最新文本重新显示
        this.windows.hideToolbar();

        // 根据配置决定是否清除选中信号
        if (this.config.toolbar.hideOnClickOutside) {
            this._lastSelectionTime = 0;
        }
    };

    /**
     * 获取工具栏参考坐标
     *
     * 四种场景触发时，鼠标都在最终选中位置：
     *   ① 滑动选中 → 鼠标在释放位置
     *   ② 拖出文本区 → 鼠标在释放位置（文本区外）
     *   ③ 双击 → 鼠标在单词上
     *   ④ 三击 → 鼠标在段落上
     *
     * 因此始终读取鼠标当前屏幕位置，不需要从 data 中计算。
     */
    _getReferencePoint(_data) {
        const cursor = screen.getCursorScreenPoint();
        return {x: Math.round(cursor.x), y: Math.round(cursor.y + 16)};
    }

    /**
     * 递归合并配置对象
     */
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

    /**
     * 处理工具栏功能项点击
     * @param {string} actionId - 功能项 ID
     */
    handleAction(actionId) {
        console.log(`[Selection] Action: ${actionId}`);

        // 工具栏不可见时，仅警告不阻断（IPC 来自工具栏点击，不应失效）
        if (!this.windows.isToolbarVisible()) {
            console.warn('[Selection] handleAction called but toolbar not visible, proceeding anyway');
        }

        // 先打开弹窗（创建窗口+设置 visibleOnAllWorkspaces），再隐藏工具栏
        // 顺序很重要：先 showPopup 确保新窗口已在当前空间注册，避免 hideToolbar 触发空间切换
        this.windows.showPopup(
            this._cachedText || '',
            this.config.window,
            actionId,
            this.config.fontSize
        );

        // 弹窗创建后再隐藏工具栏
        this.windows.hideToolbar();
    }
}

// 单例导出
const selectionService = new SelectionService();

module.exports = {selectionService};
