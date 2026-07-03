/**
 * @author Eddie
 * @date 2026-07-03
 */

/**
 * 前端统一请求工具
 *
 * 封装 fetch + ApiResult 解析，统一处理：
 * 1. HTTP 非 2xx → 优先解析 JSON body 提取后端 message
 * 2. HTTP 2xx + 业务 code ≠ 200 → 抛出 json.message
 * 3. 成功 → 返回 json.data
 *
 * 对应后端统一响应结构 ApiResult<T> { code, message, data }
 */
import type {ApiResult} from '@/types/chat'

export class ApiError extends Error {
    constructor(
        public readonly code: number,
        message: string,
    ) {
        super(message)
        this.name = 'ApiError'
    }
}

export async function request<T>(url: string, options?: RequestInit): Promise<T> {
    const res = await fetch(url, options)

    // 尝试解析 JSON 响应体（无论 HTTP 状态码如何）
    let json: ApiResult<T>
    try {
        json = await res.json()
    } catch {
        // 响应体非 JSON（如网络错误、HTML 页面等）
        throw new ApiError(res.status, `HTTP ${res.status}: ${res.statusText}`)
    }

    // HTTP 非 2xx：优先取后端返回的 message
    if (!res.ok) {
        throw new ApiError(json.code ?? res.status, json.message || `HTTP ${res.status}`)
    }

    // HTTP 2xx，但业务状态码不是成功
    if (json.code !== 200) {
        throw new ApiError(json.code, json.message || '请求失败')
    }

    return json.data as T
}
