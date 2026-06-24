/**
 * 海浪主题（Ocean Waves）
 * 蓝白/深海系，背景带有波浪形状的渐变装饰
 */
import type {ThemeDefinition} from './index'

const theme: ThemeDefinition = {
    id: 'ocean',
    name: '海浪',
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
            '--text-code-bg': '#1e1e1e',
            '--text-code': '#d4d4d4',

            /* 杂项 */
            '--scrollbar-thumb': '#8aadc4',
            '--icon-muted': '#b8d5e8',
            '--divider-light': '#d4e6f2',

            /* 背景装饰 — 波浪形状的径向渐变 */
            '--bg-decoration': `
        radial-gradient(ellipse 140% 35% at 100% 90%, rgba(14,165,233,0.45) 0%, transparent 70%),
        radial-gradient(ellipse 90% 30% at 10% 25%, rgba(56,189,248,0.30) 0%, transparent 70%),
        radial-gradient(circle 220px at 75% 10%, rgba(14,165,233,0.20) 0%, transparent 100%),
        radial-gradient(circle 180px at 20% 70%, rgba(56,189,248,0.12) 0%, transparent 100%)
      `,
        },
        dark: {
            /* 背景色 — 暮海蓝灰 */
            '--bg-primary': '#2a4060',
            '--bg-secondary': '#2a4a7a',
            '--bg-tertiary': '#3a5a8a',
            '--bg-hover': '#426a9a',
            '--bg-selected': '#4a7aaa',
            '--bg-nav-rail': '#2a4060',
            '--bg-mask': 'rgba(0, 0, 0, 0.40)',
            '--bg-tooltip': '#325a8a',

            /* 文字色 */
            '--text-primary': '#e8f4fd',
            '--text-secondary': '#8ad0ea',
            '--text-tertiary': '#5a8aaa',
            '--text-quaternary': '#6a9aba',
            '--text-muted': '#4a7a9a',
            '--text-inverse': '#2a4060',
            '--text-accent': '#38bdf8',
            '--text-disabled': '#4a7a9a',

            /* 边框色 */
            '--border-default': '#4a7aaa',
            '--border-light': '#3a6a9a',
            '--border-lighter': '#325a8a',
            '--border-hover': '#6a9aba',
            '--border-focus': '#38bdf8',

            /* 消息气泡 */
            '--msg-user-bg': '#0ea5e9',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#325a8a',
            '--msg-assistant-text': '#e8f4fd',

            /* 主色 / 强调色 */
            '--accent-default': '#7ab8d8',
            '--accent-hover': '#6aa8c8',
            '--accent-light-bg': '#2a3a50',
            '--accent-light-border': '#3a5a7a',
            '--accent-ring': 'rgba(122, 184, 216, 0.10)',

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
            '--text-code-bg': '#121212',
            '--text-code': '#d4d4d4',

            /* 杂项 */
            '--scrollbar-thumb': '#6a9aba',
            '--icon-muted': '#5a8aaa',
            '--divider-light': '#4a7aaa',

            /* 背景装饰 — 暮海波纹 */
            '--bg-decoration': `
        radial-gradient(ellipse 140% 35% at 100% 90%, rgba(56,189,248,0.55) 0%, transparent 70%),
        radial-gradient(ellipse 90% 30% at 10% 25%, rgba(14,165,233,0.40) 0%, transparent 70%),
        radial-gradient(circle 220px at 75% 10%, rgba(56,189,248,0.30) 0%, transparent 100%)
      `,
        },
    },
}

export default theme
