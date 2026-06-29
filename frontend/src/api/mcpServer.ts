import type {ApiResult} from '@/types/chat'
import type {McpConnectResult, McpServer, McpServerCreateRequest} from '@/types/mcpServer'

const BASE = '/api/mcp-servers'

/**
 * 查询全量 MCP + 工具二层列表
 * GET /api/mcp-servers
 */
export async function listMcpServers(): Promise<McpServer[]> {
    const res = await fetch(BASE)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<McpServer[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取 MCP 列表失败')
    return json.data
}

/**
 * 新增 MCP 服务器
 * POST /api/mcp-servers
 */
export async function createMcpServer(req: McpServerCreateRequest): Promise<McpServer> {
    const res = await fetch(BASE, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(req),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<McpServer> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '创建 MCP 服务失败')
    return json.data
}

/**
 * 更新 MCP 或工具的启用状态（启用时自动测试连接）
 * PATCH /api/mcp-servers/status
 */
export async function updateMcpStatus(payload: {
    mcpServerId: number
    mcpEnabled?: boolean
    tools?: { id: number; enabled: boolean }[]
}): Promise<McpConnectResult> {
    const res = await fetch(`${BASE}/status`, {
        method: 'PATCH',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<McpConnectResult> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '更新状态失败')
    return json.data
}

/**
 * 手动同步 MCP 服务器工具
 * POST /api/mcp-servers/{id}/sync-tools
 */
export async function syncMcpTools(id: number): Promise<McpConnectResult> {
    const res = await fetch(`${BASE}/${id}/sync-tools`, {method: 'POST'})
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<McpConnectResult> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '同步工具失败')
    return json.data
}

/**
 * 编辑 MCP 服务器（全量更新）
 * PUT /api/mcp-servers/{id}
 */
export async function updateMcpServer(id: number, req: McpServerCreateRequest): Promise<McpServer> {
    const res = await fetch(`${BASE}/${id}`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(req),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<McpServer> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '更新 MCP 服务失败')
    return json.data
}

/**
 * 内置工具启用/禁用切换（不涉及 MCP 连接）
 * 支持两种模式：
 * - MCP 级别：传 mcpServerId + enabled，批量更新该 MCP 下所有工具
 * - 工具级别：传 toolId + enabled，更新单个工具并联动 MCP 状态
 * PATCH /api/mcp-servers/built-in/status
 */
export async function updateBuiltInToolStatus(payload: {
    mcpServerId?: number
    toolId?: number
    enabled: boolean
}): Promise<void> {
    const res = await fetch(`${BASE}/built-in/status`, {
        method: 'PATCH',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '操作失败')
}

/**
 * 测试 MCP 服务器连接（不写入数据库，不改启用状态）
 * POST /api/mcp-servers/test-connection
 */
export async function testMcpConnection(req: McpServerCreateRequest): Promise<McpConnectResult> {
    const res = await fetch(`${BASE}/test-connection`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(req),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<McpConnectResult> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '测试连接失败')
    return json.data
}

/**
 * 删除 MCP 服务器
 * DELETE /api/mcp-servers/{id}
 */
export async function deleteMcpServer(id: number): Promise<void> {
    const res = await fetch(`${BASE}/${id}`, {method: 'DELETE'})
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '删除 MCP 服务失败')
}
