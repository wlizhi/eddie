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
 * 模型项 — 对应后端 ModelVO
 */
export interface ModelItem {
    code: string
    object: string
    ownedBy: string
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
