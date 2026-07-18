/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 工具栏 HTML 模板生成
 */

const {
    calcToolbarHeight,
    getBtnHeight,
    getBtnPad,
    getContainerPadV,
    getContainerPadH,
    getBtnFontSize,
    getDividerH,
    getCloseBtnSize,
    getToolbarGap,
} = require('./toolbar-dimensions');

// 全局默认 CSS font-family（系统默认字体）
const DEFAULT_FONT_FAMILY = "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans SC', sans-serif";

/**
 * 生成工具栏 HTML
 */
function getToolbarHtml(theme, text, items, fontSize, fontFamily, toolbarStyle) {
    const textEscaped = text
        .replace(/&/g, '&')
        .replace(/</g, '<')
        .replace(/>/g, '>')
        .replace(/"/g, '"');

    const showLabel = toolbarStyle !== 'compact';
    const iconSize = Math.round(fontSize * 0.86);
    const itemsHtml = items
        .filter(i => i.enabled)
        .sort((a, b) => a.order - b.order)
        .map((i, idx, arr) => {
            const label = i.label || '';
            const icon = i.icon
                ? i.icon.replace(/width="14"/, `width="${iconSize}"`)
                        .replace(/height="14"/, `height="${iconSize}"`)
                : '';
            const btnContent = showLabel ? `${icon} ${label}` : `${icon}`;
            const btn = `<button class="action-btn" data-id="${i.id}">${btnContent}</button>`;
            return idx < arr.length - 1 ? btn + '<div class="btn-divider"></div>' : btn;
        })
        .join('');

    const fontFamilyCSS = fontFamily || DEFAULT_FONT_FAMILY;

    const toolbarH = calcToolbarHeight(fontSize, toolbarStyle);
    const btnH = getBtnHeight(fontSize);
    const btnPad = getBtnPad(fontSize);
    const btnFs = getBtnFontSize(fontSize);
    const dividerH = getDividerH(fontSize);
    const closeSz = getCloseBtnSize(fontSize);
    const padV = getContainerPadV(fontSize);
    const padH = getContainerPadH(fontSize);
    const tbPadH = Math.ceil(fontSize * 0.12);
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
    display:flex;align-items:center;height:${toolbarH}px;
    padding:0 ${tbPadH}px;gap:${gap}px;
    background:${theme.bgSecondary};border-radius:8px;
    border:1px solid ${theme.border};
    box-shadow:0 4px 10px rgba(0,0,0,.3);
}
.actions{display:flex;align-items:center;gap:${gap}px;flex-shrink:0}
.action-btn{
    display:inline-flex;align-items:center;gap:${Math.max(2, Math.round(fontSize * 0.15))}px;
    padding:${btnPad}px ${btnPad}px;border:none;border-radius:5px;
    font-size:${btnFs}px;cursor:pointer;white-space:nowrap;
    background:transparent;color:${theme.textPrimary};
    transition:background .12s;
}
.action-btn:hover{background:${theme.hover}}
.action-btn:active{background:${theme.border}}
.action-btn svg{pointer-events:none}
.btn-divider{
    width:1px;height:${dividerH}px;align-self:center;
    background:${theme.border};opacity:.3;flex-shrink:0;
}
.close-btn{
    display:inline-flex;align-items:center;justify-content:center;
    width:${closeSz}px;height:${closeSz}px;border:none;border-radius:4px;flex-shrink:0;
    font-size:${Math.round(fontSize * 0.86)}px;cursor:pointer;background:transparent;
    color:${theme.textTertiary};transition:background .12s;
}
.close-btn:hover{background:${theme.hover};color:${theme.textPrimary}}
.close-btn svg{pointer-events:none}
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

module.exports = {
    getToolbarHtml,
};
