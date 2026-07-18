/**
 * @author Eddie
 * {@code @date} 2026-07-18
 *
 * 本地 HTTP 服务 — 提供前端静态资源，并将 API 请求代理到后端。
 *
 * 让 Electron 应用无需等待后端 HTTP 即可加载 SPA：
 * - 前端文件从本地磁盘提供
 * - 后端可以异步启动，SPA 在连接后端前显示加载状态
 * - /api/* 请求自动代理到 http://localhost:11520
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const {app} = require('electron');

// ============================================================
// 配置
// ============================================================
const PORT = 11521;
const BACKEND_URL = 'http://localhost:11520';

// MIME 类型
const MIME_TYPES = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'application/javascript; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.json': 'application/json',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
    '.ttf': 'font/truetype',
    '.map': 'application/json',
};

// ============================================================
// 获取前端静态资源根目录
// ============================================================
function getFrontendDistPath() {
    if (app.isPackaged) {
        return path.join(process.resourcesPath, 'frontend-dist');
    }
    return path.join(__dirname, '..', '..', '..', 'frontend', 'dist');
}

// ============================================================
// 代理 API 请求到后端
// ============================================================
function proxyApiRequest(req, res) {
    const options = {
        hostname: 'localhost',
        port: 11520,
        path: req.url,
        method: req.method,
        headers: {...req.headers},
    };

    // 移除 hop-by-hop headers
    delete options.headers['host'];

    const proxyReq = http.request(options, (proxyRes) => {
        res.writeHead(proxyRes.statusCode, proxyRes.headers);
        proxyRes.pipe(res);
    });

    proxyReq.on('error', (err) => {
        console.error(`[Eddie-Server] API proxy error: ${err.message}`);
        res.writeHead(502, {'Content-Type': 'application/json'});
        res.end(JSON.stringify({code: 502, message: '后端服务不可用'}));
    });

    req.pipe(proxyReq);
}

// ============================================================
// 提供静态文件
// ============================================================
function serveStaticFile(res, filePath) {
    fs.readFile(filePath, (err, data) => {
        if (err) {
            res.writeHead(404, {'Content-Type': 'text/plain'});
            res.end('Not found');
            return;
        }
        const ext = path.extname(filePath).toLowerCase();
        const contentType = MIME_TYPES[ext] || 'application/octet-stream';
        res.writeHead(200, {'Content-Type': contentType});
        res.end(data);
    });
}

// ============================================================
// 启动本地 HTTP 服务
// ============================================================
function startLocalServer() {
    const distPath = getFrontendDistPath();

    const server = http.createServer((req, res) => {
        const url = req.url;

        // API 代理
        if (url.startsWith('/api/')) {
            proxyApiRequest(req, res);
            return;
        }

        // 静态文件
        let filePath;
        if (url === '/' || !path.extname(url)) {
            filePath = path.join(distPath, 'index.html');
        } else {
            filePath = path.join(distPath, url);
        }

        serveStaticFile(res, filePath);
    });

    server.listen(PORT, () => {
        console.log(`[Eddie-Server] Local server started on http://localhost:${PORT}`);
    });

    server.on('error', (err) => {
        console.error(`[Eddie-Server] Failed to start: ${err.message}`);
    });

    return server;
}

module.exports = {startLocalServer, PORT};
