/**
 * @author Eddie
 * @date 2026-06-20
 */

import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import {dirname, resolve} from 'path'
import {fileURLToPath} from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
        },
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:11520',
                changeOrigin: true,
            },
        },
    },
    build: {
        outDir: 'dist',
        emptyOutDir: true,
        // highlight.js 含所有语言定义约 ~900 kB，提高阈值消除误警
        chunkSizeWarningLimit: 1000,
        // Rolldown: 按包分组拆分 node_modules，避免单个 vendor chunk 过大
        rolldownOptions: {
            output: {
                codeSplitting: {
                    groups: [
                        {
                            test: /[\\/]node_modules[\\/](vue|@vue|vue-router|pinia)[\\/]/,
                            name: 'vendor-vue',
                        },
                        {
                            test: /[\\/]node_modules[\\/]marked[\\/]/,
                            name: 'vendor-marked',
                        },
                        {
                            test: /[\\/]node_modules[\\/]highlight\.js[\\/]/,
                            name: 'vendor-highlight',
                        },
                        {
                            test: /[\\/]node_modules[\\/]naive-ui[\\/]/,
                            name: 'vendor-naive',
                        },
                        {
                            test: /[\\/]node_modules[\\/]@lucide[\\/]/,
                            name: 'vendor-lucide',
                        },
                        {
                            test: /[\\/]node_modules[\\/]/,
                            name: 'vendor-other',
                        },
                    ],
                },
            },
        },
    },
})
