/**
 * @author Eddie
 * @date 2026-06-24
 */

/**
 * 暮光主题（Twilight）
 * 灰紫/暮色系，背景带有暮光渐晕形状
 */
import type {ThemeDefinition} from './index'

const theme: ThemeDefinition = {
    id: 'dusk',
    name: '暮光',
    color: '#f7f5fb',
    darkColor: '#2a2040',
    randomizeDecoration: true,
    variables: {
        light: {
            /* 背景色 — 柔和灰紫调 */
            '--bg-primary': '#f8f6fb',
            '--bg-secondary': '#f0ecf6',
            '--bg-tertiary': '#e4ddf0',
            '--bg-hover': '#d8ceea',
            '--bg-selected': '#c7bae0',
            '--bg-nav-rail': '#f2eff7',
            '--bg-mask': 'rgba(46, 16, 101, 0.25)',
            '--bg-tooltip': '#4c1d95',

            /* 文字色 */
            '--text-primary': '#2e1065',
            '--text-secondary': '#4c1d95',
            '--text-tertiary': '#7c6b9a',
            '--text-quaternary': '#6d5a8c',
            '--text-muted': '#9d8bb8',
            '--text-inverse': '#ffffff',
            '--text-accent': '#7c3aed',
            '--text-disabled': '#9d8bb8',

            /* 边框色 */
            '--border-default': '#d2c5e0',
            '--border-light': '#ddd2e8',
            '--border-lighter': '#e8e0ef',
            '--border-hover': '#b09cc9',
            '--border-focus': '#7c3aed',

            /* 消息气泡 */
            '--msg-user-bg': '#7c3aed',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#ffffff',
            '--msg-assistant-text': '#2e1065',

            /* 主色 / 强调色 */
            '--accent-default': '#7c3aed',
            '--accent-hover': '#6d28d9',
            '--accent-light-bg': '#f5f3ff',
            '--accent-light-border': '#ddd6fe',
            '--accent-ring': 'rgba(124, 58, 237, 0.08)',

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
            '--scrollbar-thumb': '#b09cc9',
            '--icon-muted': '#d2c5e0',
            '--divider-light': '#ddd2e8',

            /*
             * 背景装饰 — 暮光地平线余晖
             * 底部暖橙 → 中部粉紫 → 顶部冷紫，模拟日落天色过渡
             */
            '--bg-decoration': `
        radial-gradient(ellipse 100% 28% at 50% 100%,
          rgba(251,146,60,0.18) 0%, transparent 80%),
        radial-gradient(ellipse 80% 20% at 40% 82%,
          rgba(244,114,182,0.12) 0%, transparent 80%),
        radial-gradient(ellipse 120% 18% at 55% 58%,
          rgba(167,139,250,0.10) 0%, transparent 80%),
        radial-gradient(ellipse 100% 14% at 30% 48%,
          rgba(124,58,237,0.08) 0%, transparent 80%),
        radial-gradient(ellipse 140% 12% at 60% 38%,
          rgba(139,92,246,0.06) 0%, transparent 80%),
        radial-gradient(ellipse 80% 18% at 50% 10%,
          rgba(99,102,241,0.05) 0%, transparent 80%)
      `,
        },
        dark: {
            /* 背景色 — 暗紫暮光 */
            '--bg-primary': '#2a2040',
            '--bg-secondary': '#3a2a50',
            '--bg-tertiary': '#4a3a60',
            '--bg-hover': '#5a4a70',
            '--bg-selected': '#6a4a8a',
            '--bg-nav-rail': '#2a2040',
            '--bg-mask': 'rgba(0, 0, 0, 0.40)',
            '--bg-tooltip': '#5a4a70',

            /* 文字色 */
            '--text-primary': '#f0ecf6',
            '--text-secondary': '#b89afa',
            '--text-tertiary': '#8a6aaa',
            '--text-quaternary': '#9a7aba',
            '--text-muted': '#6a4a8a',
            '--text-inverse': '#2a2040',
            '--text-accent': '#a78bfa',
            '--text-disabled': '#6a4a8a',

            /* 边框色 */
            '--border-default': '#5a4a70',
            '--border-light': '#4a3a60',
            '--border-lighter': '#3a2a50',
            '--border-hover': '#8a6aaa',
            '--border-focus': '#a78bfa',

            /* 消息气泡 */
            '--msg-user-bg': '#7c3aed',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#4a3a60',
            '--msg-assistant-text': '#f0ecf6',

            /* 主色 / 强调色 */
            '--accent-default': '#b098c8',
            '--accent-hover': '#a088b8',
            '--accent-light-bg': '#2a1a3a',
            '--accent-light-border': '#4a2a5a',
            '--accent-ring': 'rgba(176, 152, 200, 0.10)',

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
            '--scrollbar-thumb': '#8a6aaa',
            '--icon-muted': '#6a4a8a',
            '--divider-light': '#4a3a60',

            /*
             * 背景装饰 — 暮光地平线余晖（深色）
             * 底部暖橙 → 中部粉紫 → 顶部冷紫，模拟日落天色过渡
             */
            '--bg-decoration': `
        radial-gradient(ellipse 100% 28% at 50% 100%,
          rgba(251,146,60,0.28) 0%, transparent 80%),
        radial-gradient(ellipse 80% 20% at 40% 82%,
          rgba(244,114,182,0.20) 0%, transparent 80%),
        radial-gradient(ellipse 120% 18% at 55% 58%,
          rgba(167,139,250,0.18) 0%, transparent 80%),
        radial-gradient(ellipse 100% 14% at 30% 48%,
          rgba(124,58,237,0.14) 0%, transparent 80%),
        radial-gradient(ellipse 140% 12% at 60% 38%,
          rgba(139,92,246,0.10) 0%, transparent 80%),
        radial-gradient(ellipse 80% 18% at 50% 10%,
          rgba(99,102,241,0.08) 0%, transparent 80%)
      `,
        },
    },
}

export default theme
