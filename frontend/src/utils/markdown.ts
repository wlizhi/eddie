/**
 * Markdown 工具函数
 *
 * 使用 marked 将 Markdown 文本渲染为 HTML。
 * 抽离为独立工具函数，避免每个组件重复实例化 Marked。
 */
import {Marked} from 'marked'

const marked = new Marked({breaks: true, gfm: true})

/**
 * 将 Markdown 文本渲染为 HTML 字符串
 * @param text 原始 Markdown 文本
 * @returns 渲染后的 HTML，解析失败时返回原文本
 */
export function renderMd(text: string): string {
    if (!text) return ''
    try {
        return marked.parse(text) as string
    } catch {
        return text
    }
}
