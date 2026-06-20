/**
 * 助手 CRUD API
 *
 * 对应后端 AssistantController：
 *   GET    /api/assistant/list     → 查询助手列表
 *   GET    /api/assistant/{id}     → 获取助手详情
 *   POST   /api/assistant          → 新建助手
 *   PUT    /api/assistant/{id}     → 更新助手
 *   DELETE /api/assistant/{id}     → 删除助手
 */
import type {ApiResult} from '@/types/chat'
import type {AssistantCreateRequest, AssistantDetailVO, AssistantUpdateRequest, AssistantVO,} from '@/types/assistant'

const BASE = '/api/assistant'

/**
 * 查询助手列表
 * @param showAll true=查询全部, false=仅查询启用（默认 false）
 */
export async function fetchAssistantList(showAll = false): Promise<AssistantVO[]> {
    const res = await fetch(`${BASE}/list?showAll=${showAll}`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<AssistantVO[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取助手列表失败')
    return json.data
}

/**
 * 获取助手详情（配置回显）
 */
export async function fetchAssistantDetail(id: number): Promise<AssistantDetailVO> {
    const res = await fetch(`${BASE}/${id}`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<AssistantDetailVO> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取助手详情失败')
    return json.data
}

/**
 * 新建助手
 */
export async function createAssistant(data: AssistantCreateRequest): Promise<AssistantVO> {
    const res = await fetch(BASE, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<AssistantVO> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '新建助手失败')
    return json.data
}

/**
 * 更新助手设置（支持部分更新）
 */
export async function updateAssistant(id: number, data: AssistantUpdateRequest): Promise<AssistantVO> {
    const res = await fetch(`${BASE}/${id}`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<AssistantVO> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '更新助手失败')
    return json.data
}

/**
 * 批量排序：按 ID 数组顺序重新赋 sort_order
 */
export async function batchSortAssistant(ids: number[]): Promise<void> {
    const res = await fetch(`${BASE}/batch-sort`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(ids),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '排序失败')
}

/**
 * 删除助手
 */
export async function deleteAssistant(id: number): Promise<void> {
    const res = await fetch(`${BASE}/${id}`, {
        method: 'DELETE',
        headers: {'Content-Type': 'application/json'},
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '删除助手失败')
}
