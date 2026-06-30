/**
 * @author Eddie
 * @date 2026-06-22
 */

import {CAPABILITY_LABELS} from '@/types/modelProvider'

/** 后端返回的能力名是大写(VISION)，转小写统一处理 */
export function normalizeCaps(caps?: string[]): string[] {
    return (caps ?? []).map(c => c.toLowerCase())
}

/** 各能力对应的 SVG 图标 */
export function capIcon(code: string, size = 12): string {
    const svgs: Record<string, string> = {
        vision: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></svg>`,
        web_search: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>`,
        reasoning: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a4 4 0 0 1 4 4c0 1.5-.8 2.8-2 3.5V12h-4V9.5A4 4 0 0 1 8 6a4 4 0 0 1 4-4z"/><path d="M9 15h6v2H9z"/><path d="M10 19h4v3h-4z"/></svg>`,
        function_calling: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>`,
        rerank: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 9 3-3 3 3"/><path d="M6 6v12"/><path d="m15 15 3 3 3-3"/><path d="M18 18V6"/></svg>`,
        embedding: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>`,
    }
    return svgs[code] || ''
}

/** 内建能力的 code 与中文名映射 */
export const CAPABILITY_TYPES = [
    {code: 'vision', label: '视觉'},
    {code: 'web_search', label: '联网'},
    {code: 'reasoning', label: '推理'},
    {code: 'function_calling', label: '工具'},
    {code: 'rerank', label: '重排'},
    {code: 'embedding', label: '嵌入'},
]

/** 互斥能力组（重排和嵌入单选项） */
export const EXCLUSIVE_CAPS = ['rerank', 'embedding']

/** 多选能力组（可多选，与互斥组互斥） */
export const MULTI_CAPS = ['vision', 'web_search', 'reasoning', 'function_calling']

/** 各能力的颜色 */
export const CAP_COLORS: Record<string, string> = {
    vision: '#7c3aed',
    web_search: '#2563eb',
    reasoning: '#d97706',
    function_calling: '#059669',
    rerank: '#db2777',
    embedding: '#4f46e5',
}

/** 获取能力选项的样式：选中=彩色，未选中=灰色 */
export function getCapStyle(code: string, selected: boolean) {
    if (!selected) return {}
    return {color: CAP_COLORS[code] || '#6b7280', borderColor: CAP_COLORS[code] || '#6b7280'}
}

/** 切换能力选中状态（全部可选，互斥组自动取消选中） */
export function toggleCapability(caps: string[], code: string): string[] {
    const idx = caps.indexOf(code)
    const result = [...caps]

    if (idx >= 0) {
        result.splice(idx, 1)
    } else {
        if (EXCLUSIVE_CAPS.includes(code)) {
            // 点击互斥组（重排/嵌入）：清空全部，只选它
            result.length = 0
        } else {
            // 点击多选组：清除互斥组的选中
            const filtered = result.filter(c => !EXCLUSIVE_CAPS.includes(c))
            result.length = 0
            result.push(...filtered)
        }
        result.push(code)
    }
    return result
}

export {CAPABILITY_LABELS}
