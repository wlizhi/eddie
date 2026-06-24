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
            '--text-code-bg': '#1e1e1e',
            '--text-code': '#d4d4d4',

            /* 杂项 */
            '--scrollbar-thumb': '#8aadc4',
            '--icon-muted': '#b8d5e8',
            '--divider-light': '#d4e6f2',

            /*
             * 背景装饰 — 海浪波浪
             * 厚椭圆弧线错位重叠，模拟涌浪的立体起伏
             */
            '--bg-decoration': `
        radial-gradient(ellipse 90% 18% at 30% 14%,
          rgba(14,165,233,0.22) 0%, transparent 100%),
        radial-gradient(ellipse 80% 18% at 68% 14%,
          rgba(56,189,248,0.15) 0%, transparent 100%),
        radial-gradient(circle 70px at 28% 14%,
          rgba(255,255,255,0.06) 0%, transparent 100%),
        radial-gradient(ellipse 100% 20% at 55% 30%,
          rgba(14,165,233,0.16) 0%, transparent 100%),
        radial-gradient(ellipse 90% 20% at 25% 30%,
          rgba(56,189,248,0.11) 0%, transparent 100%),
        radial-gradient(circle 60px at 58% 30%,
          rgba(255,255,255,0.05) 0%, transparent 100%),
        radial-gradient(ellipse 110% 22% at 35% 46%,
          rgba(14,165,233,0.12) 0%, transparent 100%),
        radial-gradient(ellipse 100% 22% at 65% 46%,
          rgba(56,189,248,0.08) 0%, transparent 100%),
        radial-gradient(ellipse 120% 25% at 50% 62%,
          rgba(14,165,233,0.09) 0%, transparent 100%),
        radial-gradient(ellipse 110% 25% at 30% 62%,
          rgba(56,189,248,0.06) 0%, transparent 100%),
        radial-gradient(ellipse 130% 28% at 45% 78%,
          rgba(14,165,233,0.06) 0%, transparent 100%),
        radial-gradient(ellipse 100% 35% at 50% 100%,
          rgba(14,165,233,0.08) 0%, transparent 100%),
        radial-gradient(ellipse 60% 40% at 30% 0%,
          rgba(56,189,248,0.05) 0%, transparent 100%)
      `,
        },
        dark: {
            /* 背景色 — 深海暗蓝 */
            '--bg-primary': '#0b1426',
            '--bg-secondary': '#101d3a',
            '--bg-tertiary': '#16284a',
            '--bg-hover': '#1c3460',
            '--bg-selected': '#22427a',
            '--bg-nav-rail': '#0b1426',
            '--bg-mask': 'rgba(0, 0, 0, 0.50)',
            '--bg-tooltip': '#1a3055',

            /* 文字色 */
            '--text-primary': '#dce8f5',
            '--text-secondary': '#7ab4d8',
            '--text-tertiary': '#6a9ec0',
            '--text-quaternary': '#7ab4d8',
            '--text-muted': '#4a7a9a',
            '--text-inverse': '#0b1426',
            '--text-accent': '#38bdf8',
            '--text-disabled': '#3a6a8a',

            /* 边框色 */
            '--border-default': '#2a4a7a',
            '--border-light': '#1e3a62',
            '--border-lighter': '#16284a',
            '--border-hover': '#4a7aaa',
            '--border-focus': '#38bdf8',

            /* 消息气泡 */
            '--msg-user-bg': '#0ea5e9',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#14264a',
            '--msg-assistant-text': '#dce8f5',

            /* 主色 / 强调色 */
            '--accent-default': '#5a9fc8',
            '--accent-hover': '#4a8fb8',
            '--accent-light-bg': '#141e32',
            '--accent-light-border': '#1e3460',
            '--accent-ring': 'rgba(90, 159, 200, 0.12)',

            /* 语义色 */
            '--danger-default': '#f87171',
            '--danger-hover': '#ef4444',
            '--danger-light-bg': '#2e1414',
            '--danger-light-border': '#6e2828',
            '--danger-ring': 'rgba(248, 113, 113, 0.15)',
            '--success-default': '#34d399',
            '--success-light-bg': '#142e1e',
            '--success-text': '#10b981',
            '--warning-default': '#fbbf24',
            '--warning-light-bg': '#2e2414',

            /* 能力标签 */
            '--tag-vision-bg': '#1e0e3a',
            '--tag-vision-text': '#a78bfa',
            '--tag-web-bg': '#0e1e3a',
            '--tag-web-text': '#60a5fa',
            '--tag-reasoning-bg': '#2e1e0e',
            '--tag-reasoning-text': '#fbbf24',
            '--tag-fc-bg': '#0e2e1e',
            '--tag-fc-text': '#34d399',
            '--tag-rerank-bg': '#2e0e1e',
            '--tag-rerank-text': '#f472b6',
            '--tag-embedding-bg': '#0e0e2e',
            '--tag-embedding-text': '#818cf8',

            /* 代码块 */
            '--text-code-bg': '#0a0a0a',
            '--text-code': '#d4d4d4',

            /* 杂项 */
            '--scrollbar-thumb': '#3a6a9a',
            '--icon-muted': '#2a5a8a',
            '--divider-light': '#1e3a62',

            /*
             * 背景装饰 — 深海波浪（降低强度避免抢眼）
             * 厚椭圆弧线错位重叠，模拟深海涌浪的立体起伏
             */
            '--bg-decoration': `
        radial-gradient(ellipse 90% 18% at 30% 14%,
          rgba(56,189,248,0.15) 0%, transparent 100%),
        radial-gradient(ellipse 80% 18% at 68% 14%,
          rgba(14,165,233,0.10) 0%, transparent 100%),
        radial-gradient(circle 70px at 28% 14%,
          rgba(255,255,255,0.04) 0%, transparent 100%),
        radial-gradient(ellipse 100% 20% at 55% 30%,
          rgba(56,189,248,0.12) 0%, transparent 100%),
        radial-gradient(ellipse 90% 20% at 25% 30%,
          rgba(14,165,233,0.08) 0%, transparent 100%),
        radial-gradient(circle 60px at 58% 30%,
          rgba(255,255,255,0.03) 0%, transparent 100%),
        radial-gradient(ellipse 110% 22% at 35% 46%,
          rgba(56,189,248,0.10) 0%, transparent 100%),
        radial-gradient(ellipse 100% 22% at 65% 46%,
          rgba(14,165,233,0.06) 0%, transparent 100%),
        radial-gradient(ellipse 120% 25% at 50% 62%,
          rgba(56,189,248,0.08) 0%, transparent 100%),
        radial-gradient(ellipse 110% 25% at 30% 62%,
          rgba(14,165,233,0.05) 0%, transparent 100%),
        radial-gradient(ellipse 130% 28% at 45% 78%,
          rgba(56,189,248,0.06) 0%, transparent 100%),
        radial-gradient(ellipse 100% 35% at 50% 100%,
          rgba(14,165,233,0.08) 0%, transparent 100%),
        radial-gradient(ellipse 60% 40% at 30% 0%,
          rgba(56,189,248,0.05) 0%, transparent 100%)
      `,
        },
    },
}

export default theme
