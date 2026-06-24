/**
 * 极光主题（Aurora）
 * 绿紫渐变系，背景带有流动的极光光带形状
 */
import type {ThemeDefinition} from './index'

const theme: ThemeDefinition = {
    id: 'lavender',
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
            '--text-code-bg': '#1e1e1e',
            '--text-code': '#d4d4d4',

            /* 杂项 */
            '--scrollbar-thumb': '#7abfa5',
            '--icon-muted': '#b3d9c8',
            '--divider-light': '#c5e3d5',

            /* 背景装饰 — 极光流动光带 */
            '--bg-decoration': `
        radial-gradient(ellipse 120% 45% at 50% 0%, rgba(16,185,129,0.45) 0%, transparent 70%),
        radial-gradient(ellipse 90% 40% at 30% 50%, rgba(139,92,246,0.30) 0%, transparent 70%),
        radial-gradient(ellipse 100% 35% at 70% 70%, rgba(16,185,129,0.20) 0%, transparent 100%),
        radial-gradient(circle 200px at 85% 30%, rgba(139,92,246,0.12) 0%, transparent 100%)
      `,
        },
        dark: {
            /* 背景色 — 暗绿极光 */
            '--bg-primary': '#1a3a2a',
            '--bg-secondary': '#2a4a3a',
            '--bg-tertiary': '#3a5a4a',
            '--bg-hover': '#4a6a5a',
            '--bg-selected': '#4a7a5a',
            '--bg-nav-rail': '#1a3a2a',
            '--bg-mask': 'rgba(0, 0, 0, 0.40)',
            '--bg-tooltip': '#4a6a5a',

            /* 文字色 */
            '--text-primary': '#ecfdf5',
            '--text-secondary': '#8adbaa',
            '--text-tertiary': '#5a8a6a',
            '--text-quaternary': '#6a9a7a',
            '--text-muted': '#4a7a5a',
            '--text-inverse': '#1a3a2a',
            '--text-accent': '#34d399',
            '--text-disabled': '#4a7a5a',

            /* 边框色 */
            '--border-default': '#4a7a5a',
            '--border-light': '#3a6a4a',
            '--border-lighter': '#2a5a3a',
            '--border-hover': '#6a9a7a',
            '--border-focus': '#34d399',

            /* 消息气泡 */
            '--msg-user-bg': '#10b981',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#3a5a4a',
            '--msg-assistant-text': '#ecfdf5',

            /* 主色 / 强调色 */
            '--accent-default': '#78b898',
            '--accent-hover': '#68a888',
            '--accent-light-bg': '#1e3028',
            '--accent-light-border': '#385048',
            '--accent-ring': 'rgba(120, 184, 152, 0.10)',

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

            /* 杂项 */
            '--scrollbar-thumb': '#6a9a7a',
            '--icon-muted': '#4a7a5a',
            '--divider-light': '#3a6a4a',

            /* 背景装饰 */
            '--bg-decoration': `
        radial-gradient(ellipse 120% 45% at 50% 0%, rgba(52,211,153,0.50) 0%, transparent 70%),
        radial-gradient(ellipse 90% 40% at 30% 50%, rgba(167,139,250,0.35) 0%, transparent 70%),
        radial-gradient(ellipse 100% 35% at 70% 70%, rgba(16,185,129,0.25) 0%, transparent 100%)
      `,
        },
    },
}

export default theme
