/**
 * @author Eddie
 * @date 2026-06-21
 */

/**
 * 助手 CRUD API
 *
 * 对应后端 AssistantController：
 *   GET    /api/assistant/list     → 查询助手列表
 *   GET    /api/assistant/{id}     → 获取助手详情
 *   POST   /api/assistant          → 新建助手
 *   PUT    /api/assistant/{id}     → 更新助手
 *   DELETE /api/assistant/{id}     → 删除助手
 *
 * MCP Server API：
 *   GET    /api/mcp-servers?enabled=true  → 获取所有已启用 MCP 服务
 */
import type {ApiResult} from '@/types/chat'
import type {
    AssistantCreateRequest,
    AssistantDetailVO,
    AssistantUpdateRequest,
    AssistantVO,
    PromptVariableInfo
} from '@/types/assistant'
import type {McpToolItem, ToolSourceVO} from '@/types/mcpServer'

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
 * 更新助手头像（支持文字、emoji、图片上传）
 *
 * @param id        助手 ID
 * @param formData  FormData，可包含：
 *                  - avatar: string (文字/emoji)
 *                  - file: Blob/File (图片)
 */
export async function updateAssistantAvatar(id: number, formData: FormData): Promise<AssistantVO> {
    const res = await fetch(`${BASE}/${id}/avatar`, {
        method: 'POST',
        body: formData,
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<AssistantVO> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '更新头像失败')
    return json.data
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

/**
 * 获取助手已绑定的 MCP 工具列表（二层结构：MCP → tools）
 *
 * GET /api/assistant/{id}/mcp-tools
 * 用于输入框手动模式选择 MCP 使用。
 */
export async function fetchBoundMcpTools(assistantId: number): Promise<ToolSourceVO[]> {
    const res = await fetch(`${BASE}/${assistantId}/mcp-tools`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<ToolSourceVO[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取绑定 MCP 工具列表失败')
    return json.data
}

// ==================== System Prompt Variables API ====================

const SYSTEM_BASE = '/api/system'

/**
 * 获取系统提示词支持的模板变量列表
 *
 * GET /api/system/prompt-variables
 * 返回 PromptVariableResolver 中注册的所有变量（key / template / example / description）
 */
export async function fetchPromptVariables(): Promise<PromptVariableInfo[]> {
    const res = await fetch(`${SYSTEM_BASE}/prompt-variables`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<PromptVariableInfo[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取变量列表失败')
    return json.data
}

// ==================== MCP Server API ====================

const MCP_BASE = '/api/mcp-servers'

/**
 * MCP 服务器摘要（前端展示用）
 */
export interface McpServerItem {
    id: number
    name: string
    /** 该 MCP 服务下的工具列表 */
    tools?: McpToolItem[]
    /** 来源类型 */
    sourceType?: 'BUILT_IN' | 'USER' | 'PROVIDER'
}

/**
 * 工具状态选项（三态）
 */
export const TOOL_STATUS_OPTIONS = [
  {label: '自动', value: 1 as const},
  {label: '审批', value: 2 as const},
  {label: '禁用', value: 0 as const},
]

/**
 * 获取所有已启用的 MCP 服务列表
 *
 * GET /api/mcp-servers?enabled=true
 */
export async function fetchEnabledMcpServers(): Promise<McpServerItem[]> {
    const res = await fetch(`${MCP_BASE}?enabled=true`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<McpServerItem[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取 MCP 列表失败')
    return json.data
}
