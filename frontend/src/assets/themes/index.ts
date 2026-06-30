/**
 * @author Eddie
 * @date 2026-06-24
 */

/**
 * 主题注册表
 * 利用 Vite glob import 自动扫描 themes/ 下所有主题文件（排除自身）
 * 新增主题只需在 themes/ 目录下新建 .ts 文件，自动注册到设置面板
 */

/** 单个主题的亮/深色变量集 */
export interface ThemeVariables {
    light: Record<string, string>
    dark: Record<string, string>
}

/** 主题定义 */
export interface ThemeDefinition {
    /** 唯一标识，如 'default', 'ocean', 'forest' */
    id: string
    /** 显示名称，如 '默认', '海洋', '森林' */
    name: string
    /** 预览色块（亮色用） */
    color: string
    /** 预览色块（深色用） */
    darkColor: string
    /** 完整 CSS 变量集合 */
    variables: ThemeVariables
    /**
     * 每次应用主题时，是否对 --bg-decoration 的渐变位置 at X% Y% 做完全随机化
     * 开启后每次打开页面或切换回该主题，渐变光晕的位置都会不同
     */
    randomizeDecoration?: boolean
}

// 自动扫描 themes/ 下所有 .ts 文件（排除 index.ts 自身）
const themeModules = import.meta.glob<{ default: ThemeDefinition }>(
    ['./*.ts', '!./index.ts'],
    {eager: true},
)

export const themeRegistry: ThemeDefinition[] = Object.values(themeModules).map(
    (m) => m.default,
)

/** 根据 id 查找主题定义 */
export function findTheme(id: string): ThemeDefinition | undefined {
    return themeRegistry.find((t) => t.id === id)
}

/** 获取主题列表（默认主题始终排第一，其余保持自然顺序） */
export function getThemes(): ThemeDefinition[] {
    const defaultIdx = themeRegistry.findIndex((t) => t.id === 'default')
    if (defaultIdx <= 0) return [...themeRegistry]
    return [
        themeRegistry[defaultIdx],
        ...themeRegistry.slice(0, defaultIdx),
        ...themeRegistry.slice(defaultIdx + 1),
    ]
}
