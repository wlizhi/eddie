/**
 * @author Eddie
 * @date 2026-06-24
 */

/**
 * 默认主题
 * 配色值精确对应 theme.css 中的 :root（亮色）和 [data-theme="dark"]（深色）
 */
import type {ThemeDefinition} from './index'

const theme: ThemeDefinition = {
    id: 'default',
    name: '默认',
    color: '#ffffff',
    darkColor: '#1a1a1a',
    variables: {
        light: {
            /* 背景色 */
            '--bg-primary': '#ffffff',
            '--bg-secondary': '#fafbfc',
            '--bg-tertiary': '#f4f5f7',
            '--bg-hover': '#f0f1f3',
            '--bg-selected': '#e8f0fe',
            '--bg-nav-rail': '#f4f5f7',
            '--bg-mask': 'rgba(0, 0, 0, 0.35)',
            '--bg-tooltip': '#2c2c2c',

            /* 文字色 */
            '--text-primary': '#1f1f1f',
            '--text-secondary': '#374151',
            '--text-tertiary': '#9ca3af',
            '--text-quaternary': '#6b7280',
            '--text-muted': '#b0b7c3',
            '--text-inverse': '#ffffff',
            '--text-accent': '#2563eb',
            '--text-disabled': '#b0b4bb',

            /* 边框色 */
            '--border-base': '#e6e8ec',
            '--border-default': '#e6e8ec',
            '--border-light': '#e0e2e6',
            '--border-lighter': '#f0f1f3',
            '--border-hover': '#d1d5db',
            '--border-focus': '#2563eb',

            /* 消息气泡 */
            '--msg-user-bg': '#2563eb',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#f4f5f7',
            '--msg-assistant-text': '#1f1f1f',

            /* 主色 / 强调色 */
            '--accent-default': '#2563eb',
            '--accent-hover': '#1d4ed8',
            '--accent-light-bg': '#e8f0fe',
            '--accent-light-border': '#bfdbfe',
            '--accent-ring': 'rgba(37, 99, 235, 0.08)',

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
            '--scrollbar-thumb': '#c0c4cc',
            '--icon-muted': '#d1d5db',
            '--divider-light': '#e0e2e6',
            '--bg-decoration': 'none',
        },
        dark: {
            /* 背景色 */
            '--bg-primary': '#1a1a1a',
            '--bg-secondary': '#252525',
            '--bg-tertiary': '#2d2d2d',
            '--bg-hover': '#353535',
            '--bg-selected': '#1e3a5f',
            '--bg-nav-rail': '#252525',
            '--bg-mask': 'rgba(0, 0, 0, 0.55)',
            '--bg-tooltip': '#3a3a3a',

            /* 文字色 */
            '--text-primary': '#e5e5e5',
            '--text-secondary': '#a0a0a0',
            '--text-tertiary': '#707070',
            '--text-quaternary': '#8a8a8a',
            '--text-muted': '#606060',
            '--text-inverse': '#1f1f1f',
            '--text-accent': '#60a5fa',
            '--text-disabled': '#555555',

            /* 边框色 */
            '--border-base': '#404040',
            '--border-default': '#404040',
            '--border-light': '#404040',
            '--border-lighter': '#353535',
            '--border-hover': '#555555',
            '--border-focus': '#60a5fa',

            /* 消息气泡 */
            '--msg-user-bg': '#2563eb',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#2d2d2d',
            '--msg-assistant-text': '#e5e5e5',

            /* 主色 / 强调色 */
            '--accent-default': '#60a5fa',
            '--accent-hover': '#3b82f6',
            '--accent-light-bg': '#1e3a5f',
            '--accent-light-border': '#3b5f8a',
            '--accent-ring': 'rgba(96, 165, 250, 0.12)',

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
            '--scrollbar-thumb': '#555555',
            '--icon-muted': '#6b6b6b',
            '--divider-light': '#353535',
            '--bg-decoration': 'none',
        },
    },
}

export default theme
