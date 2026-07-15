/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手配置定义 — 所有可配置项的默认值
 * 后端可通过 IPC 下发配置覆盖这些默认值
 */

/**
 * 默认工具栏功能项
 * V1 仅实现 'open'（打开弹窗），其他项预留给后续扩展
 */
const SVG_DOCUMENT = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><path d="M3 1.5h5l3.5 3.5v7.5a1 1 0 0 1-1 1h-7.5a1 1 0 0 1-1-1v-10a1 1 0 0 1 1-1z"/><path d="M8 1.5v3.5h3.5"/></svg>';
const SVG_GLOBE = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M2.5 5h9"/><path d="M2.5 9h9"/><path d="M7 1.5a7 7 0 0 1 0 11"/><path d="M7 1.5a7 7 0 0 0 0 11"/></svg>';
const SVG_BOOK = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><path d="M2 2.5v9a1 1 0 0 0 1 1h3.5L7 11l.5 1.5H11a1 1 0 0 0 1-1v-9a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1z"/><path d="M7 11V4"/></svg>';
const SVG_LIST = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="3.5" x2="11.5" y2="3.5"/><line x1="5" y1="7" x2="11.5" y2="7"/><line x1="5" y1="10.5" x2="11.5" y2="10.5"/><circle cx="2.5" cy="3.5" r=".8"/><circle cx="2.5" cy="7" r=".8"/><circle cx="2.5" cy="10.5" r=".8"/></svg>';
const SVG_COPY = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><rect x="4.5" y="4.5" width="7" height="7" rx=".8"/><path d="M2.5 10.5v-7a1 1 0 0 1 1-1h7"/></svg>';

const DEFAULT_TOOLBAR_ITEMS = [
    { id: 'open',       label: '打开',   icon: SVG_DOCUMENT,  enabled: true,  order: 1 },
    { id: 'translate',  label: '翻译',   icon: SVG_GLOBE,     enabled: true,  order: 2 },
    { id: 'explain',    label: '解释',   icon: SVG_BOOK,      enabled: true,  order: 3 },
    { id: 'summarize',  label: '总结',   icon: SVG_LIST,      enabled: true,  order: 4 },
    { id: 'copy',       label: '复制',   icon: SVG_COPY,      enabled: true,  order: 5 },
];

/**
 * 划词助手完整配置
 */
const SELECTION_ASSISTANT_DEFAULTS = {
    // ============================================================
    // 启停控制（由后端配置下发）
    // ============================================================
    enabled: true,   // 测试时设为 true，上线后改回 false 由后端控制

    // ============================================================
    // 字体（由前端全局设置同步）
    // ============================================================
    fontSize: 14,    // 基准字体大小 px，通过 IPC selection:update-config 覆盖

    // ============================================================
    // 工具栏
    // ============================================================
    toolbar: {
        // 'default' — 图标 + 文字
        // 'compact' — 仅图标（预留，V1 暂不实现）
        style: 'default',

        // 功能项列表，数据驱动渲染，支持排序和启用/禁用
        items: DEFAULT_TOOLBAR_ITEMS,

        // 点击工具栏外部区域时自动隐藏
        hideOnClickOutside: true,
    },

    // ============================================================
    // 功能窗口（弹窗）
    // ============================================================
    window: {
        // 窗口位置：'follow-toolbar' | 'center'
        position: 'follow-toolbar',

        // 是否记住窗口大小（预留）
        rememberSize: false,

        // 失焦自动关闭（禁用 — blur 事件不可靠，弹窗生命周期由用户主动关闭控制）
        autoClose: false,

        // 是否默认置顶（预留）
        alwaysOnTop: true,

        // 透明度 0-100（预留）
        opacity: 100,

        // 默认窗口尺寸
        width: 500,
        height: 400,
    },

    // ============================================================
    // 触发方式（预留）
    // ============================================================
    trigger: {
        // 'selection' — 选中文本自动触发
        // 'shortcut'  — 快捷键触发
        mode: 'selection',

        // 全局快捷键（仅 shortcut 模式下有效）
        shortcut: 'CmdOrCtrl+Shift+E',
    },
};

/**
 * 深色/亮色主题默认色值（当无持久化主题配置时的兜底值）
 */
const FALLBACK_THEMES = {
    dark: {
        bgPrimary:      '#18181b',
        bgSecondary:    '#27272a',
        bgCard:         '#1f1f23',
        textPrimary:    '#e4e4e7',
        textSecondary:  '#a1a1aa',
        textTertiary:   '#52525b',
        accent:         '#6366f1',
        border:         '#3f3f46',
        hover:          '#2d2d33',
    },
    light: {
        bgPrimary:      '#ffffff',
        bgSecondary:    '#f4f4f5',
        bgCard:         '#fafafa',
        textPrimary:    '#18181b',
        textSecondary:  '#71717a',
        textTertiary:   '#a1a1aa',
        accent:         '#6366f1',
        border:         '#e4e4e7',
        hover:          '#e4e4e7',
    },
};

module.exports = {
    SELECTION_ASSISTANT_DEFAULTS,
    FALLBACK_THEMES,
    DEFAULT_TOOLBAR_ITEMS,
};
