/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手 IPC 处理器
 */

const {ipcMain} = require('electron');
const {getSelectionService} = require('../selection-assistant');

function register() {
    const service = getSelectionService();

    // 工具栏操作：点击功能项（如 "打开"）
    ipcMain.on('selection:action', (_e, actionId) => {
        service.handleAction(actionId);
    });

    // 工具栏操作：隐藏工具栏
    ipcMain.on('selection:hide-toolbar', () => {
        service.windows.hideToolbar();
    });

    // 弹窗操作：关闭弹窗
    ipcMain.on('selection:close-popup', () => {
        service.windows.hideToolbar();
        if (service.windows.popupWindow && !service.windows.popupWindow.isDestroyed()) {
            service.windows.popupWindow.close();
        }
    });

    // 弹窗操作：切换置顶状态
    ipcMain.on('selection:toggle-pin', () => {
        if (service.windows.popupWindow && !service.windows.popupWindow.isDestroyed()) {
            const win = service.windows.popupWindow;
            const isOnTop = win.isAlwaysOnTop();
            win.setAlwaysOnTop(!isOnTop);
        }
    });

    // ============================================================
    // 配置管理（供后端调用）
    // ============================================================

    // 后端下发完整或部分配置
    ipcMain.on('selection:update-config', (_e, config) => {
        service.updateConfig(config);
    });

    // 后端请求当前配置状态
    ipcMain.handle('selection:get-config', () => {
        return service.getConfig();
    });

    // 后端查询是否已激活
    ipcMain.handle('selection:is-activated', () => {
        return service.isActivated();
    });
}

module.exports = {register};
