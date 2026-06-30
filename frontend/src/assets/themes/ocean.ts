/**
 * @author Eddie
 * @date 2026-06-24
 */

/**
 * 海浪主题（Ocean Waves）
 * 蓝白/深海系，背景带有波浪形状的渐变装饰
 */
import type {ThemeDefinition} from './index'

const theme: ThemeDefinition = {
    id: 'ocean',
    name: '海洋',
    color: '#f0f7ff',
    darkColor: '#0a1628',
    randomizeDecoration: true,
    variables: {
        light: {
            /* 背景色 — 柔和蓝白 */
            '--bg-primary': '#f4faff',
            '--bg-secondary': '#e8f4fd',
            '--bg-tertiary': '#d4ecfa',
            '--bg-hover': '#c5e5f8',
            '--bg-selected': '#a8d8f5',
            '--bg-nav-rail': '#eaf2f9',
            '--bg-mask': 'rgba(12, 74, 110, 0.30)',
            '--bg-tooltip': '#0c4a6e',

            /* 文字色 */
            '--text-primary': '#0c4a6e',
            '--text-secondary': '#1e6f9f',
            '--text-tertiary': '#5f8aa8',
            '--text-quaternary': '#4d7fa0',
            '--text-muted': '#8aadc4',
            '--text-inverse': '#ffffff',
            '--text-accent': '#0ea5e9',
            '--text-disabled': '#8aadc4',

            /* 边框色 */
            '--border-default': '#c4dcec',
            '--border-light': '#d4e6f2',
            '--border-lighter': '#e2eef6',
            '--border-hover': '#7fb8d6',
            '--border-focus': '#0ea5e9',

            /* 消息气泡 */
            '--msg-user-bg': '#0ea5e9',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#ffffff',
            '--msg-assistant-text': '#0c4a6e',

            /* 主色 / 强调色 */
            '--accent-default': '#0ea5e9',
            '--accent-hover': '#0284c7',
            '--accent-light-bg': '#e0f2fe',
            '--accent-light-border': '#bae6fd',
            '--accent-ring': 'rgba(14, 165, 233, 0.08)',

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
            '--scrollbar-thumb': '#8aadc4',
            '--icon-muted': '#b8d5e8',
            '--divider-light': '#d4e6f2',

            /* 装饰层已移除（性能原因），仅保留纯色背景 */
            '--bg-decoration': 'none',
        },
        dark: {
            /* 背景色 — 清新海蓝 */
            '--bg-primary': '#1a2840',
            '--bg-secondary': '#243850',
            '--bg-tertiary': '#304860',
            '--bg-hover': '#3c5870',
            '--bg-selected': '#386888',
            '--bg-nav-rail': '#1a2840',
            '--bg-mask': 'rgba(0, 0, 0, 0.35)',
            '--bg-tooltip': '#304860',

            /* 文字色 */
            '--text-primary': '#e2edf8',
            '--text-secondary': '#8ac4e8',
            '--text-tertiary': '#6a9ec0',
            '--text-quaternary': '#7aaed0',
            '--text-muted': '#5a8aaa',
            '--text-inverse': '#1a2840',
            '--text-accent': '#38bdf8',
            '--text-disabled': '#5a8aaa',

            /* 边框色 */
            '--border-default': '#3a6a8a',
            '--border-light': '#2a5a7a',
            '--border-lighter': '#1e4a6a',
            '--border-hover': '#5a8aaa',
            '--border-focus': '#38bdf8',

            /* 消息气泡 */
            '--msg-user-bg': '#0ea5e9',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#243850',
            '--msg-assistant-text': '#e2edf8',

            /* 主色 / 强调色 */
            '--accent-default': '#6aafe0',
            '--accent-hover': '#5a9fd0',
            '--accent-light-bg': '#1a2640',
            '--accent-light-border': '#284468',
            '--accent-ring': 'rgba(106, 175, 224, 0.12)',

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
            '--scrollbar-thumb': '#5a8aaa',
            '--icon-muted': '#3a6a8a',
            '--divider-light': '#2a5a7a',

            /* 装饰层已移除（性能原因），仅保留纯色背景 */
            '--bg-decoration': 'none',
        },
    },
}

export default theme
