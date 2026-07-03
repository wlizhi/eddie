/**
 * @author Eddie
 * {@code @date} 2026-06-30
 *
 * 系统托盘管理：托盘图标创建、右键菜单、点击事件
 */

const {Tray, Menu} = require('electron');
const path = require('path');
const fs = require('fs');

let tray = null;

/**
 * 创建系统托盘
 * @param {Electron.BrowserWindow} mainWindow - 主窗口引用
 * @returns {boolean} 是否创建成功
 */
function createTray(mainWindow) {
    try {
        const iconPath = path.join(__dirname, 'icons', 'tray-icon.png');

        // 图标文件不存在时不阻塞启动（发行版打包后应有此文件）
        if (!fs.existsSync(iconPath)) {
            console.warn(`[Eddie] Tray icon not found: ${iconPath}, skipping tray creation`);
            return false;
        }

        tray = new Tray(iconPath);
        tray.setToolTip('Eddie');

        const contextMenu = Menu.buildFromTemplate([
            {
                label: '显示窗口',
                click: () => {
                    if (mainWindow && !mainWindow.isDestroyed()) {
                        mainWindow.show();
                        mainWindow.focus();
                    }
                },
            },
            {type: 'separator'},
            {
                label: '退出',
                click: () => {
                    global.isQuitting = true;
                    const {app} = require('electron');
                    app.quit();
                },
            },
        ]);

        tray.setContextMenu(contextMenu);

        // 点击托盘图标切换窗口显隐
        tray.on('click', () => {
            if (!mainWindow || mainWindow.isDestroyed()) return;
            if (mainWindow.isVisible()) {
                mainWindow.hide();
            } else {
                mainWindow.show();
                mainWindow.focus();
            }
        });

        return true;
    } catch (err) {
        console.warn(`[Eddie] Failed to create tray: ${err.message}`);
        return false;
    }
}

module.exports = {createTray};
