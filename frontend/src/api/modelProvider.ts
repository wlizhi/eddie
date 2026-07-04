/**
 * @author Eddie
 * @date 2026-06-22
 */

import type {ApiResult} from '@/types/chat'
import type {
    ModelItem,
    ModelProvider,
    ModelProviderCreatePayload,
    ModelProviderUpdatePayload
} from '@/types/modelProvider'
import {request} from '@/api/request'

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

/**
 * 新增服务商
 * POST /api/model-provider
 */
export async function createProvider(payload: ModelProviderCreatePayload): Promise<void> {
    const res = await fetch(`${BASE}`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '新增服务商失败')
}

/**
 * 修改服务商下的某个模型参数
 * PUT /api/model-provider/{providerId}/model
 */
export async function updateModel(providerId: number, payload: {
    code: string
    name?: string
    capabilities?: string[]
    currency?: string
    inputPrice?: number
    outputPrice?: number
    cacheInputPrice?: number
    cacheWriteInputPrice?: number
    callIntervalSec?: number
}): Promise<void> {
    const res = await fetch(`${BASE}/${providerId}/model`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '更新模型失败')
}

/**
 * 批量新增模型到服务商
 * POST /api/model-provider/{providerId}/models/batch-add
 */
export async function batchAddModels(providerId: number, models: {
    code: string
    name?: string
    object?: string
    ownedBy?: string
    capabilities?: string[]
    currency?: string
    inputPrice?: number
    outputPrice?: number
    cacheInputPrice?: number
    cacheWriteInputPrice?: number
}[]): Promise<void> {
    const res = await fetch(`${BASE}/${providerId}/models/batch-add`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({models}),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '批量新增模型失败')
}

/**
 * 批量删除服务商下的模型
 * POST /api/model-provider/{providerId}/models/batch-remove
 */
export async function batchRemoveModels(providerId: number, codes: string[]): Promise<void> {
    const res = await fetch(`${BASE}/${providerId}/models/batch-remove`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({codes}),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '批量删除模型失败')
}

/**
 * 远程拉取模型列表（从服务商 API 获取）
 * POST /api/model-provider/{id}/fetch-models
 */
export async function fetchRemoteModels(providerId: number): Promise<ModelItem[]> {
    return request<ModelItem[]>(`${BASE}/${providerId}/fetch-models`, {method: 'POST'})
}

/**
 * 全量更新排序序号（前端拖拽后按顺序传入 id 数组）
 * PUT /api/model-provider/sort-order
 */
export async function updateSortOrder(orderedIds: number[]): Promise<void> {
    const res = await fetch(`${BASE}/sort-order`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(orderedIds),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '更新排序失败')
}
