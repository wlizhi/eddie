/**
 * 模型服务商 — 对应后端 ModelProviderVO
 */
export interface ModelProvider {
    id: number
    code: string
    name: string
    baseUrl: string
    apiKey: string
    enabled: number
    builtIn: number
    sortOrder: number
    models: ModelItem[]
    createdAt: string
    updatedAt: string
}

/**
 * 模型能力枚举映射
 */
export const CAPABILITY_LABELS: Record<string, string> = {
    vision: '视觉',
    web_search: '联网',
    reasoning: '推理',
    function_calling: '工具',
    rerank: '重排',
    embedding: '嵌入',
}

/**
 * 模型项 — 对应后端 ModelVO
 */
export interface ModelItem {
    code: string
    object: string
    ownedBy: string
    capabilities: string[]
    currency?: string
    inputPrice?: number
    outputPrice?: number
    cacheInputPrice?: number
    cacheWriteInputPrice?: number
}

/**
 * 新增服务商请求 — 对应后端 ModelProviderCreateRequest
 */
export interface ModelProviderCreatePayload {
    code: string
    name: string
    baseUrl: string
    apiKey?: string
    models?: string
    enabled?: number
    builtIn?: number
    sortOrder?: number
}

/**
 * 更新服务商请求 — 对应后端 ModelProviderUpdateRequest
 */
export interface ModelProviderUpdatePayload {
    id: number
    name?: string
    baseUrl?: string
    apiKey?: string
    models?: string
    enabled?: number
    sortOrder?: number
}
