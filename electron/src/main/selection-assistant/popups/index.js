/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 弹窗 HTML 分派器 — 根据 actionId 返回对应功能的弹窗 HTML
 */

const {getOpenHtml} = require('./open');
const {getTranslateHtml} = require('./translate');
const {getExplainHtml} = require('./explain');
const {getSummarizeHtml} = require('./summarize');
const {getCopyHtml} = require('./copy');

/**
 * 根据 actionId 返回对应功能的弹窗 HTML
 * @param {string} actionId - 功能项 ID
 * @param {object} theme - 当前主题色
 * @param {string} text - 选中文本
 * @param {string} textEscaped - HTML 转义后的选中文本
 * @param {number} [fontSize=14] - 基准字体大小 px
 * @returns {string} 完整的 HTML 文档
 */
function getPopupHtml(actionId, theme, text, textEscaped, fontSize) {
    switch (actionId) {
        case 'translate':
            return getTranslateHtml(theme, text, textEscaped, fontSize);
        case 'explain':
            return getExplainHtml(theme, text, textEscaped, fontSize);
        case 'summarize':
            return getSummarizeHtml(theme, text, textEscaped, fontSize);
        case 'copy':
            return getCopyHtml(theme, text, textEscaped, fontSize);
        case 'open':
        default:
            return getOpenHtml(theme, text, textEscaped, fontSize);
    }
}

module.exports = {getPopupHtml};
