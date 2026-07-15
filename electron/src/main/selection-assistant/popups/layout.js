/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 弹窗通用布局 — header（标题+置顶/关闭按钮）、footer
 */

/**
 * 生成弹窗通用布局
 * @param {object} theme - 当前主题色
 * @param {string} title - 窗口标题
 * @param {string} bodyHtml - 正文 HTML
 * @param {number} [fontSize=14] - 基准字体大小 px
 * @returns {string} 完整 HTML 文档
 */
function getLayoutHtml(theme, title, bodyHtml, fontSize) {
    const fs = fontSize || 14;
    const headerPadV = Math.max(4, Math.round(fs * 0.43));
    const headerPadH = Math.max(8, Math.round(fs * 0.86));
    const titleFs = Math.max(12, Math.round(fs * 0.93));
    const iconBtnSz = Math.max(18, Math.round(fs * 1.57));
    const iconBtnFs = Math.max(10, Math.round(fs * 0.86));
    const iconBtnRadius = Math.max(4, Math.round(fs * 0.36));
    const headerActionsGap = Math.max(2, Math.round(fs * 0.28));

    return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<style>
*{margin:0;padding:0;box-sizing:border-box}
html,body{height:100%}
body{
    font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;
    font-size:${fs}px;background:${theme.bgPrimary};color:${theme.textPrimary};
    display:flex;flex-direction:column;overflow:hidden;
}
.header{
    display:flex;align-items:center;justify-content:space-between;
    padding:${headerPadV}px ${headerPadH}px;border-bottom:1px solid ${theme.border};
    background:${theme.bgSecondary};-webkit-app-region:drag;
}
.header h2{font-size:${titleFs}px;font-weight:600}
.header-actions{display:flex;gap:${headerActionsGap}px;-webkit-app-region:no-drag}
.icon-btn{
    display:inline-flex;align-items:center;justify-content:center;
    width:${iconBtnSz}px;height:${iconBtnSz}px;border:none;border-radius:${iconBtnRadius}px;
    font-size:${iconBtnFs}px;cursor:pointer;background:transparent;
    color:${theme.textTertiary};transition:background .15s;
}
.icon-btn:hover{background:${theme.hover};color:${theme.textPrimary}}
${getPopupStyles(theme)}
</style>
</head>
<body>
<div class="header">
    <h2>${title}</h2>
    <div class="header-actions">
        <button class="icon-btn" id="pinBtn" title="置顶"><svg width="16" height="16" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 2 12 5.5"/><path d="M9.5 1.5 7 4A28 28 0 0 0 4 9l1.5 1.5A28 28 0 0 0 10 7.5L12.5 5"/><path d="M5 9.5 2 12.5"/></svg></button>
        <button class="icon-btn" id="closeBtn" title="关闭"><svg width="16" height="16" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><line x1="4" y1="4" x2="10" y2="10"/><line x1="10" y1="4" x2="4" y2="10"/></svg></button>
    </div>
</div>
${bodyHtml}
</body>
</html>`;
}

/**
 * 各功能弹窗通用样式
 */
function getPopupStyles(theme) {
    return `
.content{
    flex:1;display:flex;flex-direction:column;
    padding:16px;gap:12px;overflow-y:auto;
}
.sel-section{
    display:flex;flex-direction:column;gap:6px;
}
.sel-label{
    font-size:12px;font-weight:500;color:${theme.textTertiary};
    display:flex;align-items:center;gap:4px;
    text-transform:uppercase;letter-spacing:.5px;
}
.sel-text{
    padding:10px 12px;border-radius:6px;
    background:${theme.bgCard};border:1px solid ${theme.border};
    font-size:13px;line-height:1.6;color:${theme.textSecondary};
    max-height:100px;overflow-y:auto;white-space:pre-wrap;word-break:break-all;
}
.dev-section{
    flex:1;display:flex;flex-direction:column;
    align-items:center;justify-content:center;gap:10px;
}
.dev-icon{color:${theme.textTertiary};opacity:.4}
.dev-title{font-size:16px;font-weight:600;color:${theme.textPrimary}}
.dev-desc{font-size:12px;color:${theme.textTertiary};text-align:center;line-height:1.5}
`;
}

module.exports = {getLayoutHtml};
