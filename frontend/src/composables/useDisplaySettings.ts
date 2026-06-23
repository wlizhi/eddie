import {reactive} from 'vue'
import {fetchConfigs, updateConfigs} from '@/api/settings'

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
            lightBg: '#1e3a5f',
            lightBorder: '#3b5f8a',
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
            lightBg: '#1a3a2a',
            lightBorder: '#2d5a3f',
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
            lightBg: '#3a2a1a',
            lightBorder: '#5a472a',
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
            lightBg: '#2e1a4a',
            lightBorder: '#4a2d6e',
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
            lightBg: '#3b1f1f',
            lightBorder: '#7f3d3d',
            ring: 'rgba(248,113,113,0.12)',
            borderFocus: '#f87171',
            textAccent: '#f87171'
        },
    },
}

const DISPLAY_CONFIG_KEY = 'DISPLAY_SETTINGS'

export interface DisplaySettings {
    fontSize: FontSizeLevel
    fontFamily: string
    themeMode: 'light' | 'dark'
    colorScheme: string
    /** 自定义字体大小 px 值，设置后覆盖 fontSize 等级 */
    customFontSize?: number
}

const defaultSettings: DisplaySettings = {
    fontSize: 'medium',
    fontFamily: 'system',
    themeMode: 'light',
    colorScheme: 'blue',
}

export const displaySettings = reactive<DisplaySettings>({...defaultSettings})

let loaded = false

/** 加载显示设置 */
export async function loadDisplaySettings(): Promise<void> {
    if (loaded) return
    try {
        const configs = await fetchConfigs()
        const raw = configs[DISPLAY_CONFIG_KEY]
        if (raw) {
            const parsed = JSON.parse(raw) as Partial<DisplaySettings>
            Object.assign(displaySettings, {
                ...defaultSettings,
                ...parsed,
            })
        }
    } catch {
        // 使用默认值
    }
    loaded = true
    applyDisplaySettings()
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

/** 应用到 DOM */
export function applyDisplaySettings(): void {
    const root = document.documentElement

    // 主题模式
    root.dataset.theme = displaySettings.themeMode

    // 基准字号
    const basePx = getEffectiveFontSize()
    root.style.setProperty('--base-font-size', `${basePx}px`)

    // 字体类型
    const fontFamily = FONT_FAMILY_MAP[displaySettings.fontFamily] ?? FONT_FAMILY_MAP.system
    root.style.setProperty('--font-family', fontFamily)

    // 配色方案
    const scheme = COLOR_SCHEMES[displaySettings.colorScheme]
    if (scheme) {
        const theme = displaySettings.themeMode === 'dark' ? scheme.dark : scheme.light
        root.style.setProperty('--accent-default', theme.accent)
        root.style.setProperty('--accent-hover', theme.hover)
        root.style.setProperty('--accent-light-bg', theme.lightBg)
        root.style.setProperty('--accent-light-border', theme.lightBorder)
        root.style.setProperty('--accent-ring', theme.ring)
        root.style.setProperty('--border-focus', theme.borderFocus)
        root.style.setProperty('--text-accent', theme.textAccent)
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
