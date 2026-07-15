/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * IPC 注册入口：统一在此注册所有 IPC 处理器
 */

const windowIpc = require('./window');
const themeIpc = require('./theme');
const selectionIpc = require('./selection');

function setupIpc() {
    windowIpc.register();
    themeIpc.register();
    selectionIpc.register();
}

module.exports = {setupIpc};
