/**
 * 图标尺寸统一管理 composable
 *
 * 基于全局基准字号计算响应式图标尺寸。
 * 用法：
 *   import { useIconSize } from '@/composables/useIconSize'
 *   const { iconSizeXs, iconSizeSm, iconSizeMd } = useIconSize()
 *   // 模板中：<Plus :size="iconSizeSm" />
 *
 * 基准字体 16px 时的实际值：
 *   xs=12px  sm=14px  md=16px  lg=20px  xl=24px  xxl=32px
 */
import {computed} from 'vue'
import {displaySettings, getEffectiveFontSize} from '@/composables/useDisplaySettings'

const SCALE = {
    xs: 0.75,
    sm: 0.875,
    md: 1,
    lg: 1.25,
    xl: 1.5,
    xxl: 2,
} as const

export type IconTier = keyof typeof SCALE

function iconSizeComputed(tier: IconTier) {
    return computed(() => {
        // 显式依赖字体变化，确保 fontSize/customFontSize 变化时重新计算
        void displaySettings.fontSize
        void displaySettings.customFontSize
        return Math.round(getEffectiveFontSize() * SCALE[tier])
    })
}

export function useIconSize() {
    return {
        iconSizeXs: iconSizeComputed('xs'),
        iconSizeSm: iconSizeComputed('sm'),
        iconSizeMd: iconSizeComputed('md'),
        iconSizeLg: iconSizeComputed('lg'),
        iconSizeXl: iconSizeComputed('xl'),
        iconSizeXxl: iconSizeComputed('xxl'),
    }
}

/** 获取所有图标尺寸对应的 CSS 变量键值对（用于 applyDisplaySettings）*/
export function getIconSizeCSSVariables(basePx: number): Record<string, string> {
    const vars: Record<string, string> = {}
    for (const [tier, scale] of Object.entries(SCALE)) {
        vars[`--icon-size-${tier}`] = `${Math.round(basePx * scale)}px`
    }
    return vars
}
