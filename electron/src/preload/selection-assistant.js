/**
 * @author Eddie
 * {@code @date} 2026-07-15
 *
 * 划词助手窗口的预加载脚本
 * 工具栏和弹窗窗口都使用此脚本，暴露必要的 IPC 通道
 */

const {contextBridge, ipcRenderer} = require('electron');

contextBridge.exposeInMainWorld('selectionAPI', {
    // 发送：点击功能项
    sendAction: (actionId) => ipcRenderer.send('selection:action', actionId),

    // 发送：隐藏工具栏
    hideToolbar: () => ipcRenderer.send('selection:hide-toolbar'),

    // 发送：关闭弹窗
    closePopup: () => ipcRenderer.send('selection:close-popup'),

    // 发送：切换置顶
    togglePin: () => ipcRenderer.send('selection:toggle-pin'),
});
