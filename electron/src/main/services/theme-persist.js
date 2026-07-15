/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 主题持久化 + 内存缓存 — 全局唯一主题源
 *
 * 磁盘文件路径：
 *   macOS:   ~/Library/Application Support/{app.name}/theme-prefs.json
 *   Windows: %APPDATA%/{app.name}/theme-prefs.json
 *   Linux:   ~/.config/{app.name}/theme-prefs.json
 *
 * 生命周期：
 *   1. 模块加载时（应用启动）→ 从磁盘文件加载到内存缓存 _themeCache
 *   2. 页面修改外观/主题/强调色 → IPC theme:update → 更新内存 + 写磁盘
 *   3. 任何 Electron 原生 UI 读取主题 → 调用 getTheme(isDark)，直接从内存返回完整主题
 *      始终包含全部字段（含兜底默认值），调用方无需做 null 判断
 */

const path = require('path');
const fs = require('fs');
const {app, nativeTheme} = require('electron');

const THEME_PREFS_PATH = (() => {
    return path.join(app.getPath('userData'), 'theme-prefs.json');
})();

// ============================================================
// 默认兜底主题（与 FALLBACK_THEMES 保持一致，独立存放避免循环依赖）
// ============================================================
const DEFAULT_THEME = {
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
        barTrack:       '#27272a',
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
        barTrack:       '#e4e4e7',
    },
};

// ============================================================
// 内存缓存 + 磁盘读写
// ============================================================
let _themeCache = null;

function _init() {
    try {
        if (fs.existsSync(THEME_PREFS_PATH)) {
            _themeCache = JSON.parse(fs.readFileSync(THEME_PREFS_PATH, 'utf-8'));
        }
    } catch {
        // 文件损坏等场景，忽略
    }
}
_init();

/**
 * 获取完整主题对象（始终包含全部 10 个字段 + 兜底默认值）
 * 所有 Electron 原生 UI 应通过此方法获取主题，禁止直接调用 readThemePrefs()
 *
 * @param {boolean} isDark - 是否使用深色兜底（仅在缓存缺失时生效）
 * @returns {{bgPrimary, bgSecondary, bgCard, textPrimary, textSecondary, textTertiary, accent, border, hover, barTrack}}
 */
function getTheme(isDark) {
    const fallback = isDark ? DEFAULT_THEME.dark : DEFAULT_THEME.light;
    if (!_themeCache) return {...fallback};
    return {
        bgPrimary:      _themeCache.bgPrimary      || fallback.bgPrimary,
        bgSecondary:    _themeCache.bgSecondary    || fallback.bgSecondary,
        bgCard:         _themeCache.bgCard         || fallback.bgCard,
        textPrimary:    _themeCache.textPrimary    || fallback.textPrimary,
        textSecondary:  _themeCache.textSecondary  || fallback.textSecondary,
        textTertiary:   _themeCache.textTertiary   || fallback.textTertiary,
        accent:         _themeCache.accent         || fallback.accent,
        border:         _themeCache.border         || fallback.border,
        hover:          _themeCache.hover          || fallback.hover,
        barTrack:       _themeCache.barTrack       || fallback.barTrack,
    };
}

/**
 * 获取缓存的暗色/亮色偏好（用于启动加载页等无需全部变量的场景）
 * 无缓存时根据 nativeTheme 判定
 */
function getMode() {
    if (_themeCache?.mode) return _themeCache.mode;
    return nativeTheme.shouldUseDarkColors ? 'dark' : 'light';
}

/**
 * 获取显示设置标识符（用于划词助手弹窗本地计算主题 CSS 变量）
 * 返回当前缓存的 themeId、themeMode、colorScheme、fontSize
 * 无缓存时使用默认值
 */
function getDisplaySettings() {
    return {
        themeId: _themeCache?.themeId || 'default',
        themeMode: _themeCache?.mode || getMode(),
        colorScheme: _themeCache?.colorScheme || '#6366f1',
        fontSize: _themeCache?.fontSize || 14,
        fontFamily: _themeCache?.fontFamily || "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans SC', sans-serif",
    };
}

/**
 * 更新主题色：同时更新内存缓存 + 持久化到磁盘
 * @param {object|null} theme - 完整主题对象（10 个字段 + mode），null 表示清除
 */
function writeThemePrefs(theme) {
    _themeCache = theme ? {...theme} : null;
    try {
        const dir = path.dirname(THEME_PREFS_PATH);
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, {recursive: true});
        }
        fs.writeFileSync(THEME_PREFS_PATH, JSON.stringify(_themeCache, null, 2), 'utf-8');
    } catch (err) {
        console.warn(`[Eddie] Failed to write theme prefs: ${err.message}`);
    }
}

module.exports = {getTheme, getMode, getDisplaySettings, writeThemePrefs};
