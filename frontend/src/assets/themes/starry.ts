/**
 * @author Eddie
 * @date 2026-06-24
 */

/**
 * 星空主题（Starry Night）
 * 深蓝/靛紫系，背景带有繁星、弯月和银河光带
 * 亮色 = 清透晨空（淡蓝白 + 晨星弯月）
 * 深色 = 深夜星空（深海军蓝 + 繁星弯月）
 */
import type {ThemeDefinition} from './index'

const theme: ThemeDefinition = {
    id: 'starry',
    name: '星空',
    color: '#f2f6fc',
    darkColor: '#0f172a',
    randomizeDecoration: true,
    variables: {
        light: {
            /* 背景色 — 清透晨空（淡蓝白，通透清新） */
            '--bg-primary': '#f2f6fc',
            '--bg-secondary': '#e8edf6',
            '--bg-tertiary': '#dce3f0',
            '--bg-hover': '#d3dceb',
            '--bg-selected': '#c4d0e5',
            '--bg-nav-rail': '#eaf0f8',
            '--bg-mask': 'rgba(30, 41, 59, 0.20)',
            '--bg-tooltip': '#1e293b',

            /* 文字色 — 清晰深灰 */
            '--text-primary': '#1a202c',
            '--text-secondary': '#2d3748',
            '--text-tertiary': '#4a5568',
            '--text-quaternary': '#718096',
            '--text-muted': '#a0aec0',
            '--text-inverse': '#ffffff',
            '--text-accent': '#6366f1',
            '--text-disabled': '#94a3b8',

            /* 边框色 — 轻柔细腻 */
            '--border-default': '#d6dce8',
            '--border-light': '#e2e6ee',
            '--border-lighter': '#eaeef4',
            '--border-hover': '#bcc6d6',
            '--border-focus': '#6366f1',

            /* 消息气泡 */
            '--msg-user-bg': '#6366f1',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#ffffff',
            '--msg-assistant-text': '#1a202c',

            /* 主色 / 强调色 — 靛紫 */
            '--accent-default': '#6366f1',
            '--accent-hover': '#4f46e5',
            '--accent-light-bg': '#eef2ff',
            '--accent-light-border': '#c7d2fe',
            '--accent-ring': 'rgba(99, 102, 241, 0.08)',

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
            '--scrollbar-thumb': '#c4cedc',
            '--icon-muted': '#c8d4e0',
            '--divider-light': '#e2e6ee',

            /* 装饰层已移除（性能原因），仅保留纯色背景 */
            '--bg-decoration': 'none',
        },
        dark: {
            /* 背景色 — 深夜海军蓝 */
            '--bg-primary': '#0f172a',
            '--bg-secondary': '#1e293b',
            '--bg-tertiary': '#334155',
            '--bg-hover': '#3b4f6b',
            '--bg-selected': '#4a5f7a',
            '--bg-nav-rail': '#1e293b',
            '--bg-mask': 'rgba(0, 0, 0, 0.55)',
            '--bg-tooltip': '#3b4f6b',

            /* 文字色 — 月白 */
            '--text-primary': '#f1f5f9',
            '--text-secondary': '#cbd5e1',
            '--text-tertiary': '#64748b',
            '--text-quaternary': '#94a3b8',
            '--text-muted': '#475569',
            '--text-inverse': '#0f172a',
            '--text-accent': '#a5b4fc',
            '--text-disabled': '#475569',

            /* 边框色 */
            '--border-default': '#334155',
            '--border-light': '#2a3a50',
            '--border-lighter': '#1e2a3a',
            '--border-hover': '#4a5f7a',
            '--border-focus': '#818cf8',

            /* 消息气泡 */
            '--msg-user-bg': '#6366f1',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#1e293b',
            '--msg-assistant-text': '#f1f5f9',

            /* 主色 / 强调色 — 淡靛紫 */
            '--accent-default': '#818cf8',
            '--accent-hover': '#6366f1',
            '--accent-light-bg': '#1e1e3a',
            '--accent-light-border': '#3a3a6a',
            '--accent-ring': 'rgba(129, 140, 248, 0.12)',

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
            '--text-code-bg': '#0a0a0a',
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
            '--scrollbar-thumb': '#4a5f7a',
            '--icon-muted': '#3b4f6b',
            '--divider-light': '#2a3a50',

            /* 装饰层已移除（性能原因），仅保留纯色背景 */
            '--bg-decoration': 'none',
        },
    },
}

export default theme
