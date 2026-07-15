/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手模块入口
 */

const {selectionService} = require('./SelectionService');
const {SELECTION_ASSISTANT_DEFAULTS} = require('./config');

/**
 * 初始化划词助手
 * 应用默认配置，如果 enabled: true 则会自动激活 hook。
 * 后续后端可随时通过 IPC selection:update-config 覆盖配置。
 */
function initSelectionAssistant() {
    selectionService.init();
    // 应用默认配置（含 enabled 开关），让 config.js 控制是否激活
    selectionService.updateConfig(SELECTION_ASSISTANT_DEFAULTS);
}

/**
 * 获取划词助手服务实例
 */
function getSelectionService() {
    return selectionService;
}

module.exports = {
    initSelectionAssistant,
    getSelectionService,
};
