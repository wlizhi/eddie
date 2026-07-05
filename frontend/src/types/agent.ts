/**
 * @author Eddie
 * @date 2026-07-04
 */

/**
 * 智能体相关类型定义
 *
 * 对应后端：
 *   AgentVO              → 列表展示
 *   AgentDetailVO        → 详情回显（含 modelParams / 执行控制参数）
 *   AgentCreateRequest   → 新建请求
 *   AgentUpdateRequest   → 更新请求（部分更新）
 *   ModelParams          → 模型参数（复用助手中的定义）
 */

import type {AssistantPreferences} from './assistant'

/**
 * 智能体列表项 VO（对应后端 AgentVO）
 */
export interface AgentVO {
    id: number
    name: string
    avatar: string | null
    description: string
    systemPrompt: string

    /** 主模型服务商实例 ID */
    mainProviderId: number
    /** 主模型服务商名称 */
    mainProviderName: string
    /** 主模型 ID */
    mainModelId: string

    /** 执行模式：FOREGROUND / BACKGROUND */
    executionMode: string
    /** 工具选择模式：auto / manual / none */
    toolSelectionMode: string

    /** 0=禁用, 1=启用 */
    enabled: number
    /** 排序序号 */
    sortOrder: number

    createdAt: number
    updatedAt: number
}

/**
 * 智能体详情 VO（配置回显，对应后端 AgentDetailVO）
 */
export interface AgentDetailVO {
    id: number
    name: string
    avatar: string | null
    description: string
    systemPrompt: string

    // ==================== 主模型配置 ====================

    /** 主模型服务商实例 ID */
    mainProviderId: number
    /** 主模型服务商 code */
    mainProviderCode: string
    /** 主模型服务商名称 */
    mainProviderName: string
    /** 主模型 ID */
    mainModelId: string
    /** 主模型参数 JSON */
    mainModelParams: string | null

    // ==================== 子代理模型配置 ====================

    /** 子代理模型服务商实例 ID */
    subProviderId: number | null
    /** 子代理模型服务商 code */
    subProviderCode: string | null
    /** 子代理模型服务商名称 */
    subProviderName: string | null
    /** 子代理模型 ID */
    subModelId: string | null
    /** 子代理模型参数 JSON */
    subModelParams: string | null

    // ==================== 执行控制 ====================

    /** 并发度 */
    semaphore: number
    /** 最大迭代次数 */
    maxIterations: number
    /** 单次执行超时（秒） */
    maxExecutionTimeSec: number
    /** 执行模式 */
    executionMode: string

    // ==================== 工具选择 ====================

    /** 工具选择模式 */
    toolSelectionMode: string

    /** 已绑定的 MCP Server ID 列表 */
    boundMcpServerIds?: number[]

    // ==================== 偏好设置 ====================

    /** 偏好设置 */
    preferences: AssistantPreferences | null

    // ==================== 状态 & 排序 ====================

    /** 是否启用 */
    enabled: number | boolean
    /** 排序序号 */
    sortOrder: number

    createdAt: number
    updatedAt: number
}

/**
 * 新建智能体请求参数（对应后端 AgentCreateRequest）
 */
export interface AgentCreateRequest {
    // ==================== 基本信息 ====================
    name: string
    avatar?: string
    description?: string
    systemPrompt?: string

    // ==================== 主模型配置 ====================
    mainProviderId: number
    mainModelId: string
    mainModelParams?: string

    // ==================== 子代理模型配置（可选） ====================
    subProviderId?: number
    subModelId?: string
    subModelParams?: string

    // ==================== 执行控制 ====================
    semaphore?: number
    maxIterations?: number
    maxExecutionTimeSec?: number
    executionMode?: string

    // ==================== 工具选择 ====================
    toolSelectionMode?: string

    /** 已启用的 MCP Server ID 列表 */
    enabledMcpServerIds?: number[]

    // ==================== 偏好设置 ====================
    preferences?: AssistantPreferences
}

/**
 * 更新智能体请求参数（对应后端 AgentUpdateRequest）
 */
export interface AgentUpdateRequest {
    // ==================== 基本信息 ====================
    name?: string
    avatar?: string
    description?: string
    systemPrompt?: string

    // ==================== 主模型配置 ====================
    mainProviderId?: number
    mainModelId?: string
    mainModelParams?: string

    // ==================== 子代理模型配置 ====================
    subProviderId?: number | null
    subModelId?: string | null
    subModelParams?: string | null

    // ==================== 执行控制 ====================
    semaphore?: number
    maxIterations?: number
    maxExecutionTimeSec?: number
    executionMode?: string

    // ==================== 工具选择 ====================
    toolSelectionMode?: string

    /** 已启用的 MCP Server ID 列表 */
    enabledMcpServerIds?: number[]

    // ==================== 偏好设置 ====================
    preferences?: AssistantPreferences

    // ==================== 状态 & 排序 ====================
    enabled?: number
    sortOrder?: number
}
