/**
 * @author Eddie
 * @date 2026-06-24
 */

/**
 * 极光主题（Aurora）
 * 绿紫渐变系，背景带有流动的极光光带形状
 */
import type {ThemeDefinition} from './index'

const theme: ThemeDefinition = {
    id: 'aurora',
    name: '极光',
    color: '#f0faf3',
    darkColor: '#1a3a2a',
    randomizeDecoration: true,
    variables: {
        light: {
            /* 背景色 — 极光白绿调 */
            '--bg-primary': '#f2faf5',
            '--bg-secondary': '#e3f5eb',
            '--bg-tertiary': '#cdeedb',
            '--bg-hover': '#b8e8cc',
            '--bg-selected': '#9cdfb8',
            '--bg-nav-rail': '#e8f2ec',
            '--bg-mask': 'rgba(6, 78, 59, 0.30)',
            '--bg-tooltip': '#065f46',

            /* 文字色 */
            '--text-primary': '#064e3b',
            '--text-secondary': '#065f46',
            '--text-tertiary': '#4a7c6b',
            '--text-quaternary': '#3d6b5a',
            '--text-muted': '#7aa694',
            '--text-inverse': '#ffffff',
            '--text-accent': '#10b981',
            '--text-disabled': '#7aa694',

            /* 边框色 */
            '--border-default': '#b3d9c8',
            '--border-light': '#c5e3d5',
            '--border-lighter': '#d8ede2',
            '--border-hover': '#7abfa5',
            '--border-focus': '#10b981',

            /* 消息气泡 */
            '--msg-user-bg': '#10b981',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#ffffff',
            '--msg-assistant-text': '#064e3b',

            /* 主色 / 强调色 */
            '--accent-default': '#10b981',
            '--accent-hover': '#059669',
            '--accent-light-bg': '#ecfdf5',
            '--accent-light-border': '#a7f3d0',
            '--accent-ring': 'rgba(16, 185, 129, 0.08)',

            /* 语义色 */
            '--danger-default': '#ef4444',
            '--danger-hover': '#dc2626',
            '--danger-light-bg': '#fef2f2',
            '--danger-light-border': '#fecaca',
            '--danger-ring': 'rgba(239, 68, 68, 0.12)',
            '--success-default': '#10b981',
            '--success-light-bg': '#d1fae5',
            '--success-text': '#059669',
            '--warning-default': '#d97706',
            '--warning-light-bg': '#fef3c7',

            /* 能力标签 */
            '--tag-vision-bg': '#ede9fe',
            '--tag-vision-text': '#7c3aed',
            '--tag-web-bg': '#dbeafe',
            '--tag-web-text': '#2563eb',
            '--tag-reasoning-bg': '#fef3c7',
            '--tag-reasoning-text': '#d97706',
            '--tag-fc-bg': '#d1fae5',
            '--tag-fc-text': '#059669',
            '--tag-rerank-bg': '#fce7f3',
            '--tag-rerank-text': '#db2777',
            '--tag-embedding-bg': '#e0e7ff',
            '--tag-embedding-text': '#4f46e5',

            /* 代码块 */
            '--text-code-bg': '#f6f8fa',
            '--text-code': '#24292f',

            /* 代码块语法高亮（GitHub Light 风格） */
            '--hljs-color': '#24292f',
            '--hljs-keyword': '#d73a49',
            '--hljs-type': '#6f42c1',
            '--hljs-string': '#032f62',
            '--hljs-comment': '#6a737d',
            '--hljs-number': '#005cc5',
            '--hljs-built-in': '#e36209',
            '--hljs-punctuation': '#24292f',
            '--hljs-variable': '#e36209',
            '--hljs-tag': '#22863a',
            '--hljs-selector-class': '#6f42c1',
            '--hljs-title': '#6f42c1',
            '--hljs-regexp': '#032f62',
            '--hljs-meta': '#24292f',
            '--hljs-deletion': '#b31d28',
            '--hljs-addition': '#22863a',

            /* 杂项 */
            '--scrollbar-thumb': '#7abfa5',
            '--icon-muted': '#b3d9c8',
            '--divider-light': '#c5e3d5',

            /* 装饰层已移除（性能原因），仅保留纯色背景 */
            '--bg-decoration': 'none',
        },
        dark: {
            /* 背景色 — 简约清新冷绿 */
            '--bg-primary': '#26382e',
            '--bg-secondary': '#2e4438',
            '--bg-tertiary': '#3a5448',
            '--bg-hover': '#486055',
            '--bg-selected': '#387048',
            '--bg-nav-rail': '#26382e',
            '--bg-mask': 'rgba(0, 0, 0, 0.32)',
            '--bg-tooltip': '#3a5448',

            /* 文字色 — 干净清爽 */
            '--text-primary': '#eaf5ed',
            '--text-secondary': '#b8d4c2',
            '--text-tertiary': '#6a9a7c',
            '--text-quaternary': '#7aaa8c',
            '--text-muted': '#5a8a6c',
            '--text-inverse': '#26382e',
            '--text-accent': '#5aea88',
            '--text-disabled': '#5a8a6c',

            /* 边框色 — 干净利落 */
            '--border-default': '#48705a',
            '--border-light': '#3a6048',
            '--border-lighter': '#2e5038',
            '--border-hover': '#6a9a7c',
            '--border-focus': '#5aea88',

            /* 消息气泡 */
            '--msg-user-bg': '#28c860',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#2e4438',
            '--msg-assistant-text': '#eaf5ed',

            /* 主色 / 强调色 — 清透 */
            '--accent-default': '#8ad4a0',
            '--accent-hover': '#6ac488',
            '--accent-light-bg': '#26382e',
            '--accent-light-border': '#385048',
            '--accent-ring': 'rgba(138, 212, 160, 0.12)',

            /* 语义色 */
            '--danger-default': '#f87171',
            '--danger-hover': '#ef4444',
            '--danger-light-bg': '#3b1f1f',
            '--danger-light-border': '#7f3d3d',
            '--danger-ring': 'rgba(248, 113, 113, 0.15)',
            '--success-default': '#34d399',
            '--success-light-bg': '#1a3a2a',
            '--success-text': '#10b981',
            '--warning-default': '#fbbf24',
            '--warning-light-bg': '#3a2e1a',

            /* 能力标签 */
            '--tag-vision-bg': '#2e1a4a',
            '--tag-vision-text': '#a78bfa',
            '--tag-web-bg': '#1a2a4a',
            '--tag-web-text': '#60a5fa',
            '--tag-reasoning-bg': '#3a2e1a',
            '--tag-reasoning-text': '#fbbf24',
            '--tag-fc-bg': '#1a3a2a',
            '--tag-fc-text': '#34d399',
            '--tag-rerank-bg': '#3a1a2a',
            '--tag-rerank-text': '#f472b6',
            '--tag-embedding-bg': '#1a1a3a',
            '--tag-embedding-text': '#818cf8',

            /* 代码块 */
            '--text-code-bg': '#1e1e1e',
            '--text-code': '#d4d4d4',

            /* 代码块语法高亮（VS Code Dark+ 风格） */
            '--hljs-color': '#d4d4d4',
            '--hljs-keyword': '#c586c0',
            '--hljs-type': '#4ec9b0',
            '--hljs-string': '#ce9178',
            '--hljs-comment': '#6a9955',
            '--hljs-number': '#b5cea8',
            '--hljs-built-in': '#dcdcaa',
            '--hljs-punctuation': '#b0b0b0',
            '--hljs-variable': '#9cdcfe',
            '--hljs-tag': '#569cd6',
            '--hljs-selector-class': '#dcdcaa',
            '--hljs-title': '#dcdcaa',
            '--hljs-regexp': '#ce9178',
            '--hljs-meta': '#569cd6',
            '--hljs-deletion': '#f44747',
            '--hljs-addition': '#4ec9b0',

            /* 杂项 */
            '--scrollbar-thumb': '#6a9a7c',
            '--icon-muted': '#5a8a6c',
            '--divider-light': '#3a6048',

            /* 装饰层已移除（性能原因），仅保留纯色背景 */
            '--bg-decoration': 'none',
        },
    },
}

export default theme
