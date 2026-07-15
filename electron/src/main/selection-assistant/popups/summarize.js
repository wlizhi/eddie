/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * "总结"弹窗 HTML
 */

const {getLayoutHtml} = require('./layout');

function getSummarizeHtml(theme, text, textEscaped, fontSize) {
    const body = `<div class="content">
    <div class="sel-section">
        <div class="sel-label"><svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><path d="M3 1.5h5l3.5 3.5v7.5a1 1 0 0 1-1 1h-7.5a1 1 0 0 1-1-1v-10a1 1 0 0 1 1-1z"/><path d="M8 1.5v3.5h3.5"/></svg> 原文</div>
        <div class="sel-text">${textEscaped}</div>
    </div>
    <div class="dev-section">
        <div class="dev-icon"><svg width="32" height="32" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M24 6C14 6 6 14 6 24s8 18 18 18 18-8 18-18S34 6 24 6z"/><path d="M24 16v8"/><path d="M24 30h.02"/></svg></div>
        <div class="dev-title">总结功能开发中</div>
        <div class="dev-desc">该功能正在开发中，敬请期待后续版本更新</div>
    </div>
</div>`;
    return getLayoutHtml(theme, '<svg width="16" height="16" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="3.5" x2="11.5" y2="3.5"/><line x1="5" y1="7" x2="11.5" y2="7"/><line x1="5" y1="10.5" x2="11.5" y2="10.5"/><circle cx="2.5" cy="3.5" r=".8"/><circle cx="2.5" cy="7" r=".8"/><circle cx="2.5" cy="10.5" r=".8"/></svg> 总结', body, fontSize);
}

module.exports = {getSummarizeHtml};
