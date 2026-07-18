/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * macOS focus-guard 工具
 *
 * macOS 上，窗口隐藏/销毁时系统会自动激活其他可见窗口。
 * focus-guard 技巧：先将所有可见窗口设为不可聚焦 → 执行操作 → 50ms 后恢复。
 * 参考 CherryStudio: quirks.ts beginMacFocusGuard / endMacFocusGuard
 */

const {IS_MAC} = require('../../utils/platform');

/**
 * 在 focus-guard 保护下执行操作
 *
 * @param {BrowserWindow[]} excludeWindows - 不参与 guard 的窗口（如弹窗窗口，避免干扰其焦点状态）
 * @param {function} fn - 要执行的操作
 */
function withFocusGuard(excludeWindows, fn) {
    if (!IS_MAC) {
        fn();
        return;
    }

    const guard = [];
    for (const w of require('electron').BrowserWindow.getAllWindows()) {
        if (excludeWindows && excludeWindows.includes(w)) continue;
        if (!w.isDestroyed() && w.isVisible() && w.isFocusable()) {
            guard.push(w);
            w.setFocusable(false);
        }
    }

    fn();

    if (guard.length > 0) {
        setTimeout(() => {
            for (const w of guard) {
                if (!w.isDestroyed()) w.setFocusable(true);
            }
        }, 50);
    }
}

module.exports = {withFocusGuard};
