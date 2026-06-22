import type {ApiResult} from '@/types/chat'

const BASE = '/api/settings'

/**
 * 获取全部全局配置
 * GET /api/settings/configs
 * 返回 Map<configKey, configVal>，每个 configVal 为 JSON 字符串
 */
export async function fetchConfigs(): Promise<Record<string, string>> {
    const res = await fetch(`${BASE}/configs`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<Record<string, string>> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取全局配置失败')
    return json.data
}

/**
 * 全量更新全局配置
 * PUT /api/settings/configs
 * 请求体为 Map<configKey, configVal>，只更新 enum 中已定义的 key，非法 key 自动忽略
 */
export async function updateConfigs(configs: Record<string, string>): Promise<void> {
    const res = await fetch(`${BASE}/configs`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(configs),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '保存全局配置失败')
}
