/**
 * @author Eddie
 * @date 2026-06-24
 */

/**
 * 珊瑚主题（Coral）
 * 暖橙/珊瑚色系，背景带有珊瑚状有机形状渐变
 */
import type {ThemeDefinition} from './index'

const theme: ThemeDefinition = {
    id: 'coral',
    name: '珊瑚',
    color: '#fff8f0',
    darkColor: '#1a0e08',
    randomizeDecoration: true,
    variables: {
        light: {
            /* 背景色 — 柔和暖白 */
            '--bg-primary': '#fff8f4',
            '--bg-secondary': '#ffefe6',
            '--bg-tertiary': '#ffe0d1',
            '--bg-hover': '#ffd4bf',
            '--bg-selected': '#ffc4a8',
            '--bg-nav-rail': '#fdf0e8',
            '--bg-mask': 'rgba(124, 45, 18, 0.30)',
            '--bg-tooltip': '#7c2d12',

            /* 文字色 */
            '--text-primary': '#431407',
            '--text-secondary': '#7c2d12',
            '--text-tertiary': '#9a6b57',
            '--text-quaternary': '#8c5a45',
            '--text-muted': '#b8927e',
            '--text-inverse': '#ffffff',
            '--text-accent': '#ea580c',
            '--text-disabled': '#b8927e',

            /* 边框色 */
            '--border-default': '#e8cdbf',
            '--border-light': '#f0d9ce',
            '--border-lighter': '#f5e4db',
            '--border-hover': '#d4a692',
            '--border-focus': '#ea580c',

            /* 消息气泡 */
            '--msg-user-bg': '#ea580c',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#f5e8dde3',
            '--msg-assistant-text': '#431407',

            /* 主色 / 强调色 */
            '--accent-default': '#ea580c',
            '--accent-hover': '#c2410c',
            '--accent-light-bg': '#fff7ed',
            '--accent-light-border': '#fed7aa',
            '--accent-ring': 'rgba(234, 88, 12, 0.08)',

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
            '--scrollbar-thumb': '#d4a692',
            '--icon-muted': '#e8cdbf',
            '--divider-light': '#f0d9ce',

            /*
             * 背景装饰 — 珊瑚分枝
             * 弧形窄椭圆从底部向外辐射 + 硬边圆点模拟珊瑚触须
             */
            '--bg-decoration': `
        radial-gradient(circle 28px at 20% 38%,
          rgba(251,146,60,0.20) 0%, rgba(251,146,60,0.20) 30%, rgba(234,88,12,0.08) 55%, transparent 70%),
        radial-gradient(circle 18px at 16% 34%,
          rgba(234,88,12,0.14) 0%, rgba(234,88,12,0.14) 30%, rgba(234,88,12,0.08) 55%, transparent 70%),
        radial-gradient(circle 32px at 82% 40%,
          rgba(249,115,22,0.18) 0%, rgba(249,115,22,0.18) 30%, rgba(234,88,12,0.08) 55%, transparent 70%),
        radial-gradient(circle 20px at 86% 35%,
          rgba(234,88,12,0.12) 0%, rgba(234,88,12,0.12) 30%, rgba(234,88,12,0.08) 55%, transparent 70%),
        radial-gradient(circle 22px at 60% 58%,
          rgba(251,146,60,0.14) 0%, rgba(251,146,60,0.14) 30%, rgba(234,88,12,0.08) 55%, transparent 70%),
        radial-gradient(ellipse 12% 38% at 35% 80%,
          rgba(234,88,12,0.24) 0%, transparent 100%),
        radial-gradient(ellipse 10% 30% at 28% 60%,
          rgba(249,115,22,0.18) 0%, transparent 100%),
        radial-gradient(ellipse 8% 24% at 22% 42%,
          rgba(234,88,12,0.14) 0%, transparent 100%),
        radial-gradient(ellipse 12% 38% at 68% 82%,
          rgba(249,115,22,0.22) 0%, transparent 100%),
        radial-gradient(ellipse 10% 32% at 75% 62%,
          rgba(234,88,12,0.16) 0%, transparent 100%),
        radial-gradient(ellipse 8% 26% at 80% 44%,
          rgba(249,115,22,0.12) 0%, transparent 100%),
        radial-gradient(ellipse 8% 30% at 55% 85%,
          rgba(234,88,12,0.18) 0%, transparent 100%),
        radial-gradient(ellipse 6% 24% at 58% 65%,
          rgba(251,146,60,0.12) 0%, transparent 100%),
        radial-gradient(ellipse 100% 25% at 50% 100%,
          rgba(234,88,12,0.08) 0%, transparent 100%)
      `,
        },
        dark: {
            /* 背景色 — 暖褐珊瑚 */
            '--bg-primary': '#3a2218',
            '--bg-secondary': '#4a2e20',
            '--bg-tertiary': '#5a3a28',
            '--bg-hover': '#6a4630',
            '--bg-selected': '#8a5030',
            '--bg-nav-rail': '#3a2218',
            '--bg-mask': 'rgba(0, 0, 0, 0.40)',
            '--bg-tooltip': '#6a4630',

            /* 文字色 */
            '--text-primary': '#fef3ed',
            '--text-secondary': '#fdba74',
            '--text-tertiary': '#b07a5a',
            '--text-quaternary': '#c08a6a',
            '--text-muted': '#8a6048',
            '--text-inverse': '#3a2218',
            '--text-accent': '#fb923c',
            '--text-disabled': '#8a6048',

            /* 边框色 */
            '--border-default': '#6a4630',
            '--border-light': '#5a3a28',
            '--border-lighter': '#4a2e20',
            '--border-hover': '#b07a5a',
            '--border-focus': '#fb923c',

            /* 消息气泡 */
            '--msg-user-bg': '#ea580c',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#5a3a28',
            '--msg-assistant-text': '#fef3ed',

            /* 主色 / 强调色 */
            '--accent-default': '#c8a888',
            '--accent-hover': '#b89878',
            '--accent-light-bg': '#352218',
            '--accent-light-border': '#4a3424',
            '--accent-ring': 'rgba(200, 168, 136, 0.10)',

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
            '--scrollbar-thumb': '#b07a5a',
            '--icon-muted': '#8a6048',
            '--divider-light': '#5a3a28',

            /*
             * 背景装饰 — 珊瑚分枝（深色）
             * 弧形窄椭圆从底部向外辐射 + 硬边圆点模拟珊瑚触须
             */
            '--bg-decoration': `
        radial-gradient(circle 28px at 20% 38%,
          rgba(251,146,60,0.30) 0%, rgba(251,146,60,0.30) 30%, rgba(234,88,12,0.08) 55%, transparent 70%),
        radial-gradient(circle 18px at 16% 34%,
          rgba(234,88,12,0.22) 0%, rgba(234,88,12,0.22) 30%, rgba(234,88,12,0.08) 55%, transparent 70%),
        radial-gradient(circle 32px at 82% 40%,
          rgba(249,115,22,0.28) 0%, rgba(249,115,22,0.28) 30%, rgba(234,88,12,0.08) 55%, transparent 70%),
        radial-gradient(circle 20px at 86% 35%,
          rgba(234,88,12,0.20) 0%, rgba(234,88,12,0.20) 30%, rgba(234,88,12,0.08) 55%, transparent 70%),
        radial-gradient(circle 22px at 60% 58%,
          rgba(251,146,60,0.22) 0%, rgba(251,146,60,0.22) 30%, rgba(234,88,12,0.08) 55%, transparent 70%),
        radial-gradient(ellipse 12% 38% at 35% 80%,
          rgba(251,146,60,0.35) 0%, transparent 100%),
        radial-gradient(ellipse 10% 30% at 28% 60%,
          rgba(249,115,22,0.26) 0%, transparent 100%),
        radial-gradient(ellipse 8% 24% at 22% 42%,
          rgba(234,88,12,0.20) 0%, transparent 100%),
        radial-gradient(ellipse 12% 38% at 68% 82%,
          rgba(249,115,22,0.32) 0%, transparent 100%),
        radial-gradient(ellipse 10% 32% at 75% 62%,
          rgba(234,88,12,0.24) 0%, transparent 100%),
        radial-gradient(ellipse 8% 26% at 80% 44%,
          rgba(249,115,22,0.18) 0%, transparent 100%),
        radial-gradient(ellipse 8% 30% at 55% 85%,
          rgba(234,88,12,0.26) 0%, transparent 100%),
        radial-gradient(ellipse 6% 24% at 58% 65%,
          rgba(251,146,60,0.18) 0%, transparent 100%),
        radial-gradient(ellipse 100% 25% at 50% 100%,
          rgba(234,88,12,0.14) 0%, transparent 100%)
      `,
        },
    },
}

export default theme
