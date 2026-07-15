/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手模块入口
 */

const {selectionService} = require('./SelectionService');

/**
 * 初始化划词助手
 * 从后端 HTTP 拉取用户配置，根据 enabled 状态决定是否激活 hook。
 */
async function initSelectionAssistant() {
    selectionService.init();
    await selectionService.refreshConfig();
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
