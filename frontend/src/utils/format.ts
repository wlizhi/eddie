/**
 * 格式化工具函数
 *
 * 提供日期时间、数字等格式化功能。
 * 抽离为独立工具函数，便于复用和测试。
 */

/**
 * 将时间戳格式化为 "YYYY-MM-DD HH:mm:ss" 字符串
 * @param ts 毫秒时间戳
 * @returns 格式化后的时间字符串，非法或空输入返回空字符串
 */
export function formatTime(ts: number | undefined | null): string {
    if (ts == null || ts <= 0) return ''
    const d = new Date(ts)
    if (isNaN(d.getTime())) return ''
    const pad = (n: number) => n.toString().padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/**
 * 将时间戳格式化为 "MM-DD HH:mm" 短格式（无年、无秒）
 * @param ts 毫秒时间戳
 * @returns 格式化后的短时间字符串，非法或空输入返回空字符串
 */
export function formatShortTime(ts: number | undefined | null): string {
    if (ts == null || ts <= 0) return ''
    const d = new Date(ts)
    if (isNaN(d.getTime())) return ''
    const pad = (n: number) => n.toString().padStart(2, '0')
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
