import {reactive, watch} from 'vue'
import {fetchConfigs, updateConfigs} from '@/api/settings'
import {findTheme, getThemes, type ThemeDefinition} from '@/assets/themes/index'

/** 字体大小等级 */
export type FontSizeLevel = 'small' | 'medium' | 'large'

/** 预设字体类型 */
export interface FontOption {
    label: string
    value: string
}

export const FONT_OPTIONS: FontOption[] = [
    {label: '系统默认', value: 'system'},
    {label: '思源黑体', value: 'noto'},
    {label: '微软雅黑', value: 'yahei'},
    {label: '宋体', value: 'songti'},
    {label: '等线', value: 'dengxian'},
]

/** 字体大小等级对应的基准 px（差异拉大，让切换有感知） */
export const FONT_SIZE_MAP: Record<FontSizeLevel, number> = {
    small: 14,
    medium: 16,
    large: 18,
}

/** 建议范围 / 硬性兜底 */
export const MIN_RECOMMENDED = 12
export const MAX_RECOMMENDED = 24
const MIN_SAFE = 10
const MAX_SAFE = 28

/** 将输入值 clamp 到安全范围内 */
export function clampFontSize(px: number): number {
    return Math.max(MIN_SAFE, Math.min(MAX_SAFE, Math.round(px)))
}

/** 字体类型 → CSS font-family */
const FONT_FAMILY_MAP: Record<string, string> = {
    system: '-apple-system, BlinkMacSystemFont, \'Segoe UI\', Roboto, \'Helvetica Neue\', Arial, \'Noto Sans SC\', sans-serif',
    noto: '\'Noto Sans SC\', \'PingFang SC\', \'Microsoft YaHei\', sans-serif',
    yahei: '\'Microsoft YaHei\', \'PingFang SC\', \'Noto Sans SC\', sans-serif',
    songti: '\'SimSun\', \'Noto Serif SC\', serif',
    dengxian: '\'DengXian\', \'PingFang SC\', \'Noto Sans SC\', sans-serif',
}

/** 配色方案定义：每个方案的亮/深色主题值 */
export interface ColorSchemeValue {
    accent: string
    hover: string
    lightBg: string
    lightBorder: string
    ring: string
    borderFocus: string
    textAccent: string
}

export interface ColorSchemeDefinition {
    label: string
    color: string          // 色块展示颜色
    light: ColorSchemeValue
    dark: ColorSchemeValue
}

export const COLOR_SCHEMES: Record<string, ColorSchemeDefinition> = {
    blue: {
        label: '蓝色',
        color: '#3b82f6',
        light: {
            accent: '#2563eb',
            hover: '#1d4ed8',
            lightBg: '#e8f0fe',
            lightBorder: '#bfdbfe',
            ring: 'rgba(37,99,235,0.08)',
            borderFocus: '#2563eb',
            textAccent: '#2563eb'
        },
        dark: {
            accent: '#60a5fa',
            hover: '#3b82f6',
            lightBg: 'rgba(96,165,250,0.12)',
            lightBorder: 'rgba(96,165,250,0.25)',
            ring: 'rgba(96,165,250,0.12)',
            borderFocus: '#60a5fa',
            textAccent: '#60a5fa'
        },
    },
    green: {
        label: '绿色',
        color: '#22c55e',
        light: {
            accent: '#16a34a',
            hover: '#15803d',
            lightBg: '#dcfce7',
            lightBorder: '#bbf7d0',
            ring: 'rgba(22,163,74,0.08)',
            borderFocus: '#16a34a',
            textAccent: '#16a34a'
        },
        dark: {
            accent: '#4ade80',
            hover: '#22c55e',
            lightBg: 'rgba(74,222,128,0.12)',
            lightBorder: 'rgba(74,222,128,0.25)',
            ring: 'rgba(74,222,128,0.12)',
            borderFocus: '#4ade80',
            textAccent: '#4ade80'
        },
    },
    orange: {
        label: '橙色',
        color: '#f97316',
        light: {
            accent: '#ea580c',
            hover: '#c2410c',
            lightBg: '#fff7ed',
            lightBorder: '#fed7aa',
            ring: 'rgba(234,88,12,0.08)',
            borderFocus: '#ea580c',
            textAccent: '#ea580c'
        },
        dark: {
            accent: '#fb923c',
            hover: '#f97316',
            lightBg: 'rgba(251,146,60,0.12)',
            lightBorder: 'rgba(251,146,60,0.25)',
            ring: 'rgba(251,146,60,0.12)',
            borderFocus: '#fb923c',
            textAccent: '#fb923c'
        },
    },
    purple: {
        label: '紫色',
        color: '#a855f7',
        light: {
            accent: '#9333ea',
            hover: '#7e22ce',
            lightBg: '#f3e8ff',
            lightBorder: '#e9d5ff',
            ring: 'rgba(147,51,234,0.08)',
            borderFocus: '#9333ea',
            textAccent: '#9333ea'
        },
        dark: {
            accent: '#c084fc',
            hover: '#a855f7',
            lightBg: 'rgba(192,132,252,0.12)',
            lightBorder: 'rgba(192,132,252,0.25)',
            ring: 'rgba(192,132,252,0.12)',
            borderFocus: '#c084fc',
            textAccent: '#c084fc'
        },
    },
    red: {
        label: '红色',
        color: '#ef4444',
        light: {
            accent: '#ef4444',
            hover: '#dc2626',
            lightBg: '#fef2f2',
            lightBorder: '#fecaca',
            ring: 'rgba(239,68,68,0.08)',
            borderFocus: '#ef4444',
            textAccent: '#ef4444'
        },
        dark: {
            accent: '#f87171',
            hover: '#ef4444',
            lightBg: 'rgba(248,113,113,0.12)',
            lightBorder: 'rgba(248,113,113,0.25)',
            ring: 'rgba(248,113,113,0.12)',
            borderFocus: '#f87171',
            textAccent: '#f87171'
        },
    },
    indigo: {
        label: '靛蓝',
        color: '#6366f1',
        light: {
            accent: '#4f46e5',
            hover: '#4338ca',
            lightBg: '#eef2ff',
            lightBorder: '#c7d2fe',
            ring: 'rgba(79,70,229,0.08)',
            borderFocus: '#4f46e5',
            textAccent: '#4f46e5',
        },
        dark: {
            accent: '#818cf8',
            hover: '#6366f1',
            lightBg: 'rgba(129,140,248,0.12)',
            lightBorder: 'rgba(129,140,248,0.25)',
            ring: 'rgba(129,140,248,0.12)',
            borderFocus: '#818cf8',
            textAccent: '#818cf8',
        },
    },
    sky: {
        label: '天蓝',
        color: '#38bdf8',
        light: {
            accent: '#0ea5e9',
            hover: '#0284c7',
            lightBg: '#f0f9ff',
            lightBorder: '#bae6fd',
            ring: 'rgba(14,165,233,0.08)',
            borderFocus: '#0ea5e9',
            textAccent: '#0ea5e9',
        },
        dark: {
            accent: '#38bdf8',
            hover: '#0ea5e9',
            lightBg: 'rgba(56,189,248,0.12)',
            lightBorder: 'rgba(56,189,248,0.25)',
            ring: 'rgba(56,189,248,0.12)',
            borderFocus: '#38bdf8',
            textAccent: '#38bdf8',
        },
    },
    teal: {
        label: '青色',
        color: '#14b8a6',
        light: {
            accent: '#0d9488',
            hover: '#0f766e',
            lightBg: '#f0fdfa',
            lightBorder: '#99f6e4',
            ring: 'rgba(13,148,136,0.08)',
            borderFocus: '#0d9488',
            textAccent: '#0d9488',
        },
        dark: {
            accent: '#2dd4bf',
            hover: '#14b8a6',
            lightBg: 'rgba(45,212,191,0.12)',
            lightBorder: 'rgba(45,212,191,0.25)',
            ring: 'rgba(45,212,191,0.12)',
            borderFocus: '#2dd4bf',
            textAccent: '#2dd4bf',
        },
    },
    rose: {
        label: '玫瑰红',
        color: '#f43f5e',
        light: {
            accent: '#e11d48',
            hover: '#be123c',
            lightBg: '#fff1f2',
            lightBorder: '#fecdd3',
            ring: 'rgba(225,29,72,0.08)',
            borderFocus: '#e11d48',
            textAccent: '#e11d48',
        },
        dark: {
            accent: '#fb7185',
            hover: '#f43f5e',
            lightBg: 'rgba(251,113,133,0.12)',
            lightBorder: 'rgba(251,113,133,0.25)',
            ring: 'rgba(251,113,133,0.12)',
            borderFocus: '#fb7185',
            textAccent: '#fb7185',
        },
    },
    amber: {
        label: '琥珀',
        color: '#f59e0b',
        light: {
            accent: '#d97706',
            hover: '#b45309',
            lightBg: '#fffbeb',
            lightBorder: '#fde68a',
            ring: 'rgba(217,119,6,0.08)',
            borderFocus: '#d97706',
            textAccent: '#d97706',
        },
        dark: {
            accent: '#fbbf24',
            hover: '#f59e0b',
            lightBg: 'rgba(251,191,36,0.12)',
            lightBorder: 'rgba(251,191,36,0.25)',
            ring: 'rgba(251,191,36,0.12)',
            borderFocus: '#fbbf24',
            textAccent: '#fbbf24',
        },
    },
    violet: {
        label: '紫罗兰',
        color: '#8b5cf6',
        light: {
            accent: '#7c3aed',
            hover: '#6d28d9',
            lightBg: '#f5f3ff',
            lightBorder: '#ddd6fe',
            ring: 'rgba(124,58,237,0.08)',
            borderFocus: '#7c3aed',
            textAccent: '#7c3aed',
        },
        dark: {
            accent: '#a78bfa',
            hover: '#8b5cf6',
            lightBg: 'rgba(167,139,250,0.12)',
            lightBorder: 'rgba(167,139,250,0.25)',
            ring: 'rgba(167,139,250,0.12)',
            borderFocus: '#a78bfa',
            textAccent: '#a78bfa',
        },
    },
}

const DISPLAY_CONFIG_KEY = 'DISPLAY_SETTINGS'

export interface DisplaySettings {
    fontSize: FontSizeLevel
    fontFamily: string
    /** 主题 ID（对应 themes/ 目录下的主题定义，如 'default', 'ocean'） */
    themeId: string
    /** 主题变体：亮色/深色（保持与旧版 themeMode 字段名兼容） */
    themeMode: 'light' | 'dark'
    colorScheme: string
    /** 自定义字体大小 px 值，设置后覆盖 fontSize 等级 */
    customFontSize?: number
}

const defaultSettings: DisplaySettings = {
    fontSize: 'medium',
    fontFamily: 'system',
    themeId: 'default',
    themeMode: 'light',
    colorScheme: 'blue',
}

export const displaySettings = reactive<DisplaySettings>({...defaultSettings})

let loaded = false
/** 标记主题是否已从后端加载并应用到 DOM */
export const isReady = {value: false}

/**
 * 全局自动 watch：displaySettings 的任何变化自动同步到 DOM
 * 消除各组件手动调用 applyDisplay() 的负担
 */
watch(
    () => ({...displaySettings}),
    () => {
        applyDisplaySettings()
    },
    {deep: true},
)

/** 加载显示设置 */
export async function loadDisplaySettings(): Promise<void> {
    if (loaded) return
    try {
        const configs = await fetchConfigs()
        const raw = configs[DISPLAY_CONFIG_KEY]
        if (raw) {
            const parsed = JSON.parse(raw) as Partial<DisplaySettings> & { themeMode?: string }

            // 向后兼容：旧数据只有 themeMode（'light'|'dark'），没有 themeId
            if (!('themeId' in parsed) && parsed.themeMode) {
                parsed.themeId = 'default'
            }

            Object.assign(displaySettings, {
                ...defaultSettings,
                ...parsed,
            })
        }
    } catch {
        // 使用默认值
    }
    loaded = true
    // 无论是否从后端加载到数据，都确保主题应用到 DOM
    applyDisplaySettings()
    isReady.value = true
}

/** 应用显示设置到本地（仅前端生效，不持久化） */
export function applyDisplay(): void {
    applyDisplaySettings()
}

/** 保存显示设置到后端并持久化 */
export async function saveDisplaySettings(): Promise<void> {
    // 先立即应用到 DOM，用户即时看到效果
    applyDisplaySettings()

    const payload: Record<string, string> = {
        [DISPLAY_CONFIG_KEY]: JSON.stringify({
            fontSize: displaySettings.fontSize,
            fontFamily: displaySettings.fontFamily,
            themeId: displaySettings.themeId,
            themeMode: displaySettings.themeMode,
            colorScheme: displaySettings.colorScheme,
            customFontSize: displaySettings.customFontSize,
        }),
    }
    await updateConfigs(payload)
}

/** 获取当前生效的字体大小 px 值 */
export function getEffectiveFontSize(): number {
    if (displaySettings.customFontSize && displaySettings.customFontSize > 0) {
        return clampFontSize(displaySettings.customFontSize)
    }
    return FONT_SIZE_MAP[displaySettings.fontSize] ?? 16
}

/** 获取当前主题的亮/深色变量集 */
function getCurrentThemeVariables(): Record<string, string> | null {
    const theme = findTheme(displaySettings.themeId)
    if (!theme) return null
    return theme.variables[displaySettings.themeMode]
}

/** 应用到 DOM */
export function applyDisplaySettings(): void {
    const root = document.documentElement

    // 主题模式（设置 data-theme 属性，供 CSS 选择器使用）
    root.dataset.theme = displaySettings.themeMode

    // 基准字号
    const basePx = getEffectiveFontSize()
    root.style.setProperty('--base-font-size', `${basePx}px`)

    // 字体类型
    const fontFamily = FONT_FAMILY_MAP[displaySettings.fontFamily] ?? FONT_FAMILY_MAP.system
    root.style.setProperty('--font-family', fontFamily)

    // 应用主题的全部 CSS 变量
    const themeVars = getCurrentThemeVariables()
    if (themeVars) {
        for (const [key, value] of Object.entries(themeVars)) {
            // 将多行渐变压缩为单行，确保 var(--bg-decoration) 在任何上下文中都能正确解析
            const v = key === '--bg-decoration' ? value.replace(/\s+/g, ' ').trim() : value
            root.style.setProperty(key, v)
        }
    }

    // 配色方案（叠加覆盖主题中的 accent 相关变量）
    const scheme = COLOR_SCHEMES[displaySettings.colorScheme]
    if (scheme) {
        const vars = displaySettings.themeMode === 'dark' ? scheme.dark : scheme.light
        root.style.setProperty('--accent-default', vars.accent)
        root.style.setProperty('--accent-hover', vars.hover)
        root.style.setProperty('--accent-light-bg', vars.lightBg)
        root.style.setProperty('--accent-light-border', vars.lightBorder)
        root.style.setProperty('--accent-ring', vars.ring)
        root.style.setProperty('--border-focus', vars.borderFocus)
        root.style.setProperty('--text-accent', vars.textAccent)
    }

    // 背景装饰层：.app-backdrop 多层背景（形状渐变叠在背景色之上）
    const backdrop = document.querySelector('.app-backdrop') as HTMLElement | null
    const decoRaw = themeVars?.['--bg-decoration']
    if (backdrop) {
        if (decoRaw && decoRaw !== 'none') {
            // 将多行渐变压缩为单行
            const deco = decoRaw.replace(/\s+/g, ' ').trim()
            const bgPrimary = themeVars?.['--bg-primary'] || '#ffffff'
            backdrop.style.background = `${deco}, ${bgPrimary}`
        } else {
            // 无装饰时清除行内样式，回退到 CSS 变量
            backdrop.style.background = ''
        }
    }
}

/** 获取字体大小等级的显示标签 */
export function getFontSizeLabel(level: FontSizeLevel): string {
    const map: Record<FontSizeLevel, string> = {
        small: '小',
        medium: '中',
        large: '大',
    }
    return map[level]
}

// 导出主题相关工具函数，方便其他组件使用
export {getThemes, findTheme}
export type {ThemeDefinition}
