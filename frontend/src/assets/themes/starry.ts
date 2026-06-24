/**
 * 星空主题（Starry Night）
 * 深蓝/靛紫系，背景带有繁星、弯月和银河光带
 * 亮色 = 晨曦星空（蒙蒙亮的天色 + 晨星弯月）
 * 深色 = 深夜星空（深海军蓝 + 繁星弯月）
 */
import type {ThemeDefinition} from './index'

const theme: ThemeDefinition = {
    id: 'starry',
    name: '星空',
    color: '#cad4e2',
    darkColor: '#0f172a',
    randomizeDecoration: true,
    variables: {
        light: {
            /* 背景色 — 蒙蒙亮的天色（晨曦蓝灰） */
            '--bg-primary': '#cad4e2',
            '--bg-secondary': '#d4dee8',
            '--bg-tertiary': '#dce3ed',
            '--bg-hover': '#e0e8f0',
            '--bg-selected': '#c8d8e8',
            '--bg-nav-rail': '#d4dee8',
            '--bg-mask': 'rgba(30, 41, 59, 0.30)',
            '--bg-tooltip': '#1e293b',

            /* 文字色 — 深蓝灰 */
            '--text-primary': '#1e293b',
            '--text-secondary': '#334155',
            '--text-tertiary': '#4a5f7a',
            '--text-quaternary': '#475569',
            '--text-muted': '#7a8da3',
            '--text-inverse': '#ffffff',
            '--text-accent': '#6366f1',
            '--text-disabled': '#94a3b8',

            /* 边框色 */
            '--border-default': '#c8d4e0',
            '--border-light': '#d4dce8',
            '--border-lighter': '#dce4ee',
            '--border-hover': '#a8b8c8',
            '--border-focus': '#6366f1',

            /* 消息气泡 */
            '--msg-user-bg': '#6366f1',
            '--msg-user-text': '#ffffff',
            '--msg-assistant-bg': '#ffffff',
            '--msg-assistant-text': '#1e293b',

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
            '--text-code-bg': '#1e1e1e',
            '--text-code': '#d4d4d4',

            /* 杂项 */
            '--scrollbar-thumb': '#a8b8c8',
            '--icon-muted': '#bcc8d8',
            '--divider-light': '#d4dce8',

            /*
             * 背景装饰 — 晨星 + 弯月 + 晨曦
             * CSS background 多层叠加顺序：第 1 层在最上方
             * 切出层必须在月亮主体之上才能形成月牙
             */
            '--bg-decoration': `
        radial-gradient(circle 2.5px at 12% 18%,
          rgba(255,255,255,0.55) 0%, rgba(255,255,255,0.55) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2px at 28% 10%,
          rgba(255,255,255,0.45) 0%, rgba(255,255,255,0.45) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 3px at 42% 25%,
          rgba(255,255,255,0.50) 0%, rgba(255,255,255,0.50) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2px at 58% 15%,
          rgba(255,255,255,0.40) 0%, rgba(255,255,255,0.40) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2.5px at 72% 28%,
          rgba(255,255,255,0.45) 0%, rgba(255,255,255,0.45) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 1.5px at 85% 12%,
          rgba(255,255,255,0.35) 0%, rgba(255,255,255,0.35) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2px at 35% 35%,
          rgba(255,255,255,0.35) 0%, rgba(255,255,255,0.35) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 1.5px at 50% 32%,
          rgba(255,255,255,0.30) 0%, rgba(255,255,255,0.30) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2px at 90% 30%,
          rgba(255,255,255,0.38) 0%, rgba(255,255,255,0.38) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 1.5px at 18% 42%,
          rgba(255,255,255,0.28) 0%, rgba(255,255,255,0.28) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 48px at 84% 17%,
          #cad4e2 0%, #cad4e2 35%, transparent 39%),
        radial-gradient(circle 55px at 82% 15%,
          rgba(255,243,205,0.35) 0%, rgba(255,243,205,0.35) 38%, transparent 42%),
        radial-gradient(circle 130px at 82% 15%,
          rgba(255,243,205,0.07) 0%, transparent 100%),
        linear-gradient(180deg,
          rgba(251,191,36,0.06) 0%, rgba(219,234,254,0.08) 40%, transparent 80%)
      `,
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

            /* 杂项 */
            '--scrollbar-thumb': '#4a5f7a',
            '--icon-muted': '#3b4f6b',
            '--divider-light': '#2a3a50',

            /*
             * 背景装饰 — 繁星 + 弯月 + 银河光带
             * CSS background 多层叠加顺序：第 1 层在最上方
             */
            '--bg-decoration': `
        radial-gradient(circle 3px at 8% 15%,
          rgba(255,255,255,0.70) 0%, rgba(255,255,255,0.70) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2.5px at 20% 5%,
          rgba(255,255,255,0.55) 0%, rgba(255,255,255,0.55) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 4px at 35% 18%,
          rgba(255,255,255,0.75) 0%, rgba(255,255,255,0.75) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2.5px at 50% 8%,
          rgba(255,255,255,0.50) 0%, rgba(255,255,255,0.50) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 3px at 62% 22%,
          rgba(255,255,255,0.60) 0%, rgba(255,255,255,0.60) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2px at 72% 5%,
          rgba(255,255,255,0.45) 0%, rgba(255,255,255,0.45) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2.5px at 92% 18%,
          rgba(255,255,255,0.55) 0%, rgba(255,255,255,0.55) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 3px at 15% 35%,
          rgba(255,255,255,0.50) 0%, rgba(255,255,255,0.50) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2px at 28% 42%,
          rgba(255,255,255,0.40) 0%, rgba(255,255,255,0.40) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 4px at 42% 30%,
          rgba(255,255,255,0.60) 0%, rgba(255,255,255,0.60) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2.5px at 55% 38%,
          rgba(255,255,255,0.45) 0%, rgba(255,255,255,0.45) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 3px at 70% 32%,
          rgba(255,255,255,0.55) 0%, rgba(255,255,255,0.55) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2px at 85% 35%,
          rgba(255,255,255,0.40) 0%, rgba(255,255,255,0.40) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 3px at 95% 25%,
          rgba(255,255,255,0.50) 0%, rgba(255,255,255,0.50) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2px at 10% 50%,
          rgba(255,255,255,0.35) 0%, rgba(255,255,255,0.35) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 3px at 40% 52%,
          rgba(255,255,255,0.40) 0%, rgba(255,255,255,0.40) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2px at 65% 48%,
          rgba(255,255,255,0.35) 0%, rgba(255,255,255,0.35) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 2.5px at 80% 55%,
          rgba(255,255,255,0.30) 0%, rgba(255,255,255,0.30) 35%, rgba(255,255,255,0.08) 60%, transparent 75%),
        radial-gradient(circle 48px at 84% 12%,
          #0f172a 0%, #0f172a 35%, transparent 39%),
        radial-gradient(circle 55px at 82% 10%,
          rgba(255,243,205,0.40) 0%, rgba(255,243,205,0.40) 38%, transparent 42%),
        radial-gradient(circle 90px at 80% 12%,
          rgba(255,243,205,0.08) 0%, transparent 100%),
        radial-gradient(ellipse 90% 14% at 50% 45%,
          rgba(255,255,255,0.05) 0%, transparent 100%),
        linear-gradient(135deg,
          rgba(99,102,241,0.10) 0%, transparent 40%,
          transparent 60%, rgba(139,92,246,0.08) 100%)
      `,
        },
    },
}

export default theme
