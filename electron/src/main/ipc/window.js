/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 窗口控制 IPC 处理器
 */

const {ipcMain} = require('electron');
const {getMainWindow} = require('../window');

function register() {
    ipcMain.on('window-minimize', () => getMainWindow()?.minimize());

    ipcMain.on('window-maximize', () => {
        const win = getMainWindow();
        win?.isMaximized() ? win.unmaximize() : win.maximize();
    });

    ipcMain.on('window-close', () => getMainWindow()?.close());

    ipcMain.handle('window-is-maximized', () => getMainWindow()?.isMaximized() ?? false);
}

module.exports = {register};
