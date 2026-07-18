/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 弹窗尺寸持久化工具
 */

const fs = require('fs');
const path = require('path');

/**
 * 从磁盘加载已保存的弹窗尺寸
 * @param {string} filePath - 持久化文件路径
 * @returns {{width: number, height: number}|null}
 */
function loadPopupSize(filePath) {
    try {
        if (fs.existsSync(filePath)) {
            const raw = fs.readFileSync(filePath, 'utf-8');
            return JSON.parse(raw);
        }
    } catch { /* ignore */ }
    return null;
}

/**
 * 保存弹窗尺寸到磁盘
 * @param {string} filePath - 持久化文件路径
 * @param {number[]} size - [width, height]
 */
function savePopupSize(filePath, size) {
    try {
        const dir = path.dirname(filePath);
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, {recursive: true});
        }
        fs.writeFileSync(filePath, JSON.stringify({width: size[0], height: size[1]}), 'utf-8');
    } catch { /* ignore */ }
}

module.exports = {loadPopupSize, savePopupSize};
