import {reactive, watch} from 'vue'
import {fetchConfigs, updateConfigs} from '@/api/settings'
import {findTheme, getThemes, type ThemeDefinition} from '@/assets/themes/index'
import {getIconSizeCSSVariables} from '@/composables/useIconSize'

/** 字体大小等级 */
export type FontSizeLevel = 'small' | 'medium' | 'large'

/** 预设字体类型 */
interface FontOption {
    label: string
    value: string
}

export const FONT_OPTIONS: FontOption[] = [
    {label: '系统默认', value: 'system'},
    {label: '思源黑体 (Noto Sans SC)', value: 'noto'},
    {label: '思源宋体 (Noto Serif SC)', value: 'noto-serif'},
    {label: '微软雅黑', value: 'yahei'},
    {label: '宋体', value: 'songti'},
    {label: '黑体', value: 'heiti'},
    {label: '仿宋', value: 'fangsong'},
    {label: '楷体', value: 'kaiti'},
    {label: '等线', value: 'dengxian'},
    {label: '苹方 (PingFang SC)', value: 'pingfang'},
    {label: 'HarmonyOS Sans', value: 'harmony'},
    {label: 'MiSans', value: 'misans'},
    {label: 'OPPO Sans', value: 'opposans'},
    {label: '阿里巴巴普惠体', value: 'alibaba'},
    {label: '霞鹜文楷 (LXGW WenKai)', value: 'lxgw'},
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
    'noto-serif': '\'Noto Serif SC\', \'Source Han Serif SC\', serif',
    yahei: '\'Microsoft YaHei\', \'PingFang SC\', \'Noto Sans SC\', sans-serif',
    songti: '\'SimSun\', \'Noto Serif SC\', serif',
    heiti: '\'SimHei\', \'Microsoft YaHei\', \'Noto Sans SC\', sans-serif',
    fangsong: '\'FangSong\', \'STFangsong\', serif',
    kaiti: '\'KaiTi\', \'STKaiti\', \'Noto Serif SC\', serif',
    dengxian: '\'DengXian\', \'PingFang SC\', \'Noto Sans SC\', sans-serif',
    pingfang: '\'PingFang SC\', \'Microsoft YaHei\', \'Noto Sans SC\', sans-serif',
    harmony: '\'HarmonyOS Sans\', \'PingFang SC\', \'Microsoft YaHei\', sans-serif',
    misans: '\'MiSans\', \'PingFang SC\', \'Microsoft YaHei\', sans-serif',
    opposans: '\'OPPO Sans\', \'PingFang SC\', \'Microsoft YaHei\', sans-serif',
    alibaba: '\'Alibaba PuHuiTi\', \'PingFang SC\', \'Microsoft YaHei\', sans-serif',
    lxgw: '\'LXGW WenKai\', \'KaiTi\', \'Noto Serif SC\', serif',
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
    color: string          // 色块展示颜色（hex）
    light: ColorSchemeValue
    dark: ColorSchemeValue
}

/**
 * 预设强调色列表（按展示顺序）
 * 只保留用户选择的 7 个：琥珀、紫罗兰、绿色、蓝色、天蓝、玫瑰红、橙色
 */
export const COLOR_SCHEMES: ColorSchemeDefinition[] = [
    {
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
    {
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
    {
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
    {
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
    {
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
    {
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
    {
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
]

/** 默认强调色（列表第一个） */
export const DEFAULT_ACCENT_COLOR = COLOR_SCHEMES[0].color

/**
 * 根据 hex 颜色值查找匹配的预设强调色定义
 */
export function findColorScheme(hex: string): ColorSchemeDefinition | undefined {
    const normalized = hex.toLowerCase()
    return COLOR_SCHEMES.find((s) => s.color.toLowerCase() === normalized)
}

/**
 * 判断给定 hex 是否为已知预设色
 */
export function isPresetColor(hex: string): boolean {
    return !!findColorScheme(hex)
}

// ===== 颜色工具函数 =====

/** 将 hex 颜色(#rrggbb)解析为 RGB 数值 */
function hexToRgb(hex: string): { r: number; g: number; b: number } | null {
    const m = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
    if (!m) return null
    return {r: parseInt(m[1], 16), g: parseInt(m[2], 16), b: parseInt(m[3], 16)}
}

/** 将 RGB 数值转回 hex 字符串 */
function rgbToHex(r: number, g: number, b: number): string {
    return '#' + [r, g, b].map((c) => Math.round(Math.max(0, Math.min(255, c))).toString(16).padStart(2, '0')).join('')
}

/** 按百分比加深颜色（负数即变浅） */
function shadeColor(hex: string, percent: number): string {
    const rgb = hexToRgb(hex)
    if (!rgb) return hex
    return rgbToHex(rgb.r * (1 - percent / 100), rgb.g * (1 - percent / 100), rgb.b * (1 - percent / 100))
}

/** 生成 rgba 字符串 */
function toRgba(hex: string, alpha: number): string {
    const rgb = hexToRgb(hex)
    if (!rgb) return hex
    return `rgba(${rgb.r},${rgb.g},${rgb.b},${alpha})`
}

/**
 * 从单一 hex 颜色自动生成全套强调色变体
 * @param hex  用户选中的强调色
 * @param isDark  是否为深色模式
 */
export function generateAccentVariants(hex: string, isDark: boolean): ColorSchemeValue {
    return {
        accent: hex,
        hover: shadeColor(hex, isDark ? 10 : 15),
        lightBg: isDark ? toRgba(hex, 0.12) : shadeColor(hex, -92),
        lightBorder: isDark ? toRgba(hex, 0.25) : shadeColor(hex, -75),
        ring: toRgba(hex, 0.08),
        borderFocus: hex,
        textAccent: isDark ? shadeColor(hex, -30) : hex,
    }
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
    /** 宽屏模式 */
    wideMode: boolean
    /** 聊天模式（false = 问答模式左对齐） */
    chatMode: boolean
    /** 用户昵称（全局） */
    nickname?: string
    /** 用户头像（全局）：文字/emoji/图片URL */
    avatar?: string
    /** 元数据：显示时间 */
    showMetaTime: boolean
    /** 元数据：显示接口耗时 */
    showMetaDuration: boolean
    /** 元数据：显示 token 用量 */
    showMetaTokens: boolean
    /** 元数据：显示花费估算 */
    showMetaCost: boolean
}

const defaultSettings: DisplaySettings = {
    fontSize: 'medium',
    fontFamily: 'system',
    themeId: 'default',
    themeMode: 'light',
    colorScheme: DEFAULT_ACCENT_COLOR,
    wideMode: true,
    chatMode: true,
    nickname: '',
    avatar: '',
    showMetaTime: true,
    showMetaDuration: true,
    showMetaTokens: true,
    showMetaCost: true,
}

export const displaySettings = reactive<DisplaySettings>({...defaultSettings})

let loaded = false
/** 标记主题是否已从后端加载并应用到 DOM */
const isReady = {value: false}

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

            // 向后兼容：旧数据 colorScheme 存的是 key（如 'blue'），现改为存 hex 值
            if (parsed.colorScheme && !/^#/.test(parsed.colorScheme)) {
                // 尝试按旧 key 查找，找到则用其 color 值
                const oldKey = parsed.colorScheme
                // 旧 key → hex 映射
                const LEGACY_KEYS: Record<string, string> = {
                    blue: '#3b82f6',
                    green: '#22c55e',
                    orange: '#f97316',
                    purple: '#a855f7',
                    red: '#ef4444',
                    indigo: '#6366f1',
                    sky: '#38bdf8',
                    teal: '#14b8a6',
                    rose: '#f43f5e',
                    amber: '#f59e0b',
                    violet: '#8b5cf6',
                }
                const hex = LEGACY_KEYS[oldKey]
                if (hex) {
                    parsed.colorScheme = hex
                } else {
                    // 无法识别的旧 key，回退到默认
                    parsed.colorScheme = DEFAULT_ACCENT_COLOR
                }
            }

            // 如果后端返回 null 或空字符串，默认使用第一个预设色
            if (!parsed.colorScheme) {
                parsed.colorScheme = DEFAULT_ACCENT_COLOR
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
            wideMode: displaySettings.wideMode,
            chatMode: displaySettings.chatMode,
            nickname: displaySettings.nickname,
            avatar: displaySettings.avatar,
            showMetaTime: displaySettings.showMetaTime,
            showMetaDuration: displaySettings.showMetaDuration,
            showMetaTokens: displaySettings.showMetaTokens,
            showMetaCost: displaySettings.showMetaCost,
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

/**
 * 在原位置附近抖动渐变位置 — 让 at X% Y% 的坐标每次有 ±20% 的偏移
 * 既每次打开都不同，又不脱离渐变区域，避免屏幕大面积纯色
 * 只替换位置，不改变颜色、尺寸、透明度
 */
function randomizeGradientPositions(cssValue: string): string {
    const JITTER = 20 // 抖动范围 ±20 个百分点
    return cssValue.replace(/at\s+(\d+(?:\.\d+)?)%\s+(\d+(?:\.\d+)?)%/g, (_match, x, y) => {
        const newX = Math.round(parseFloat(x) + (Math.random() - 0.5) * 2 * JITTER)
        const newY = Math.round(parseFloat(y) + (Math.random() - 0.5) * 2 * JITTER)
        return `at ${Math.max(0, Math.min(100, newX))}% ${Math.max(0, Math.min(100, newY))}%`
    })
}

/** 移动端字号缩放系数（768px 以下视口生效） */
const MOBILE_FONT_SCALE = 1.25

/** 应用到 DOM */
export function applyDisplaySettings(): void {
    const root = document.documentElement

    // 主题模式（设置 data-theme 属性，供 CSS 选择器使用）
    root.dataset.theme = displaySettings.themeMode

    // 基准字号（移动端自动放大，使阅读体验与桌面端一致）
    let basePx = getEffectiveFontSize()
    if (window.innerWidth < 768) {
        basePx = Math.round(basePx * MOBILE_FONT_SCALE)
    }
    root.style.setProperty('--base-font-size', `${basePx}px`)

    // 头像大小 = 基准字体大小 × 2.5（放大差异让不同字体等级间的头像变化有感知度）
    //   小 14px → 35px | 中 16px → 40px | 大 18px → 45px | 自定义 20px → 50px
    const avatarSize = Math.round(basePx * 2.2)
    root.style.setProperty('--avatar-size', `${avatarSize}px`)

    // 图标尺寸 CSS 变量（基于基准字体的比例系数）
    const iconCSSVars = getIconSizeCSSVariables(basePx)
    for (const [key, value] of Object.entries(iconCSSVars)) {
        root.style.setProperty(key, value)
    }

    // 字体类型
    const fontFamily = FONT_FAMILY_MAP[displaySettings.fontFamily] ?? FONT_FAMILY_MAP.system
    root.style.setProperty('--font-family', fontFamily)

    // 获取当前主题定义，判断是否需要随机化装饰渐变
    const theme = findTheme(displaySettings.themeId)
    const shouldRandomize = theme?.randomizeDecoration ?? false

    // 应用主题的全部 CSS 变量
    const themeVars = getCurrentThemeVariables()
    if (themeVars) {
        for (const [key, value] of Object.entries(themeVars)) {
            // 将多行渐变压缩为单行，确保 var(--bg-decoration) 在任何上下文中都能正确解析
            let v = key === '--bg-decoration' ? value.replace(/\s+/g, ' ').trim() : value
            // 对开启了随机化的主题，每次应用时随机化渐变位置
            if (key === '--bg-decoration' && shouldRandomize && v !== 'none') {
                v = randomizeGradientPositions(v)
            }
            root.style.setProperty(key, v)
        }
    }

    // 配色方案（叠加覆盖主题中的 accent 相关变量）
    const colorVal = displaySettings.colorScheme
    let vars: ColorSchemeValue | null = null
    if (colorVal) {
        // 先按 hex 匹配预设色
        const preset = findColorScheme(colorVal)
        if (preset) {
            vars = displaySettings.themeMode === 'dark' ? preset.dark : preset.light
        } else if (/^#/.test(colorVal)) {
            // hex 值但非预设 → 自定义颜色，自动生成变体
            vars = generateAccentVariants(colorVal, displaySettings.themeMode === 'dark')
        }
    }
    // 无有效值则跳过（使用主题自身的 accent 变量）
    if (vars) {
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
            let deco = decoRaw.replace(/\s+/g, ' ').trim()
            // 对开启了随机化的主题，同样随机化背景装饰层
            if (shouldRandomize) {
                deco = randomizeGradientPositions(deco)
            }
            // 装饰渐变存入自定义属性，供 ::before 读取（脱离底色，避免阴影影响外框）
            backdrop.style.setProperty('--deco-instance', deco)
            // 底色直接设置（不含装饰，不会产生边框阴影）
            const bgPrimary = themeVars?.['--bg-primary'] || '#ffffff'
            backdrop.style.backgroundColor = bgPrimary
        } else {
            // 无装饰时清除行内样式，回退到 CSS 变量
            backdrop.style.removeProperty('--deco-instance')
            backdrop.style.backgroundColor = ''
        }
    }
}

// 导出主题相关工具函数，方便其他组件使用
export {getThemes, findTheme}
export type {ThemeDefinition}
