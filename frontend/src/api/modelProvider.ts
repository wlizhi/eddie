import type {ApiResult} from '@/types/chat'
import type {ModelItem, ModelProvider, ModelProviderUpdatePayload} from '@/types/modelProvider'

const BASE = '/api/model-provider'

/**
 * 获取所有服务商列表（含 models 嵌套）
 * GET /api/model-provider/list-with-models
 */
export async function listProviders(): Promise<ModelProvider[]> {
    const res = await fetch(`${BASE}/list-with-models`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<ModelProvider[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取服务商列表失败')
    return json.data
}

/**
 * 根据服务商 code 获取模型列表
 * GET /api/model-provider/{code}/models
 */
export async function getModelsByCode(code: string): Promise<ModelItem[]> {
    const res = await fetch(`${BASE}/${code}/models`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<ModelItem[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取模型列表失败')
    return json.data
}

/**
 * 更新服务商
 * PUT /api/model-provider
 */
export async function updateProvider(payload: ModelProviderUpdatePayload): Promise<void> {
    const res = await fetch(`${BASE}`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '更新失败')
}

/**
 * 删除服务商
 * DELETE /api/model-provider/{id}
 */
export async function deleteProvider(id: number): Promise<void> {
    const res = await fetch(`${BASE}/${id}`, {method: 'DELETE'})
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '删除失败')
}
