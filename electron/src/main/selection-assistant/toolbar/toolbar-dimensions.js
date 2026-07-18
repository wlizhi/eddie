/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 工具栏尺寸计算工具函数
 */

// ============================================================
// 工具栏尺寸计算（基于 fontSize，跟随全局字体设置）
// ============================================================

// 基准：fontSize 为 14px 时，各尺寸
//   btnHeight = ceil(14 * 1.5) = 21
//   btnPad    = ceil(14 * 0.28) = 4
//   padV      = ceil(14 * 0.28) = 4
//   dividerH  = ceil(14 * 0.7) = 10
//   containerPadV = ceil(14 * 0.28) = 4
//   toolbarHeight = btnHeight + containerPadV * 2 = 29

function calcToolbarHeight(fontSize, toolbarStyle) {
    const btnPad = Math.ceil(fontSize * 0.09);
    const btnH = Math.ceil(fontSize * 1.5);
    const padV = toolbarStyle === 'compact'
        ? 0
        : Math.ceil(fontSize * 0.12);
    return btnH + padV * 2;
}

function getBtnHeight(fontSize) { return Math.ceil(fontSize * 1.5); }
function getBtnPad(fontSize) { return Math.ceil(fontSize * 0.09); }
function getContainerPadV(fontSize) { return Math.ceil(fontSize * 0.12); }
function getContainerPadH(fontSize) { return Math.ceil(fontSize * 0.12); }
function getBtnFontSize(fontSize) { return Math.max(11, Math.round(fontSize * 0.82)); }
function getDividerH(fontSize) { return Math.ceil(fontSize * 0.7); }
function getCloseBtnSize(fontSize) { return Math.ceil(fontSize * 1.14); }
function getToolbarGap(fontSize) { return Math.max(2, Math.round(fontSize * 0.14)); }

// 度量文字宽度
function measureText(s, fontSize) {
    const cjkW = Math.round(fontSize * 0.86); // 中文字宽 ≈ 0.86em
    const asciiW = Math.round(fontSize * 0.5); // ASCII 字宽 ≈ 0.5em
    let w = 0;
    for (const ch of s) w += ch.charCodeAt(0) > 127 ? cjkW : asciiW;
    return w;
}

// 根据功能项动态计算工具栏宽度
function calcToolbarWidth(items, fontSize, toolbarStyle) {
    const enabled = items.filter(i => i.enabled).sort((a, b) => a.order - b.order);
    if (!enabled.length) return 120;

    const padH = getContainerPadH(fontSize);
    const btnPad = getBtnPad(fontSize);
    const gap = getToolbarGap(fontSize);
    const iconW = Math.round(fontSize * 0.86);
    const gapIconText = Math.max(2, Math.round(fontSize * 0.15));
    const dividerW = 1;
    const closeBtn = getCloseBtnSize(fontSize);
    const showLabel = toolbarStyle !== 'compact';

    let actionsW = 0;
    enabled.forEach((item, idx) => {
        const textW = showLabel ? measureText(item.label || '', fontSize) : 0;
        const gapW = showLabel ? gapIconText : 0;
        actionsW += btnPad + iconW + gapW + textW + btnPad;
        if (idx < enabled.length - 1) actionsW += dividerW + gap;
    });

    return padH + actionsW + gap + closeBtn + padH;
}

module.exports = {
    calcToolbarHeight,
    getBtnHeight,
    getBtnPad,
    getContainerPadV,
    getContainerPadH,
    getBtnFontSize,
    getDividerH,
    getCloseBtnSize,
    getToolbarGap,
    measureText,
    calcToolbarWidth,
};
