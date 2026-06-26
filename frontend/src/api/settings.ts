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

/**
 * 更新用户头像（支持文字、emoji、图片上传）
 * POST /api/settings/user-avatar
 *
 * @param avatarText 文字或 emoji（可选）
 * @param file       图片文件（可选）
 * @returns 更新后的头像值
 */
export async function updateUserAvatar(avatarText?: string, file?: File): Promise<string> {
    const fd = new FormData()
    if (file) {
        fd.append('file', file)
    } else if (avatarText) {
        fd.append('avatar', avatarText)
    }
    const res = await fetch(`${BASE}/user-avatar`, {
        method: 'POST',
        body: fd,
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<string> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '更新头像失败')
    return json.data
}
