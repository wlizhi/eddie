/**
 * @author Eddie
 * @date 2026-07-04
 */

/**
 * 智能体 CRUD API
 *
 * 对应后端 AgentController：
 *   GET    /api/agent/manage/list         → 查询智能体列表
 *   GET    /api/agent/manage/{id}         → 获取智能体详情
 *   POST   /api/agent/manage              → 新建智能体
 *   PUT    /api/agent/manage/{id}         → 更新智能体
 *   DELETE /api/agent/manage/{id}         → 删除智能体
 *   PUT    /api/agent/manage/batch-sort   → 批量排序
 *
 * 同时也复用全局 MCP API（/api/mcp-servers）获取已启用的 MCP 服务列表
 */
import type {ApiResult} from '@/types/chat'
import type {AgentCreateRequest, AgentDetailVO, AgentUpdateRequest, AgentVO,} from '@/types/agent'
import type {McpServerItem} from './assistant'
import type {ToolSourceVO} from '@/types/mcpServer'

const BASE = '/api/agent/manage'

/**
 * 查询智能体列表
 * @param showAll true=查询全部, false=仅查询启用（默认 false）
 */
export async function fetchAgentList(showAll = false): Promise<AgentVO[]> {
    const res = await fetch(`${BASE}/list?showAll=${showAll}`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<AgentVO[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取智能体列表失败')
    return json.data
}

/**
 * 获取智能体详情（配置回显）
 */
export async function fetchAgentDetail(id: number): Promise<AgentDetailVO> {
    const res = await fetch(`${BASE}/${id}`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<AgentDetailVO> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取智能体详情失败')
    return json.data
}

/**
 * 新建智能体
 */
export async function createAgent(data: AgentCreateRequest): Promise<AgentVO> {
    const res = await fetch(BASE, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<AgentVO> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '新建智能体失败')
    return json.data
}

/**
 * 更新智能体设置
 */
export async function updateAgent(id: number, data: AgentUpdateRequest): Promise<AgentVO> {
    const res = await fetch(`${BASE}/${id}`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<AgentVO> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '更新智能体失败')
    return json.data
}

/**
 * 批量排序：按 ID 数组顺序重新赋 sort_order
 */
export async function batchSortAgent(ids: number[]): Promise<void> {
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
 * 删除智能体
 */
export async function deleteAgent(id: number): Promise<void> {
    const res = await fetch(`${BASE}/${id}`, {
        method: 'DELETE',
        headers: {'Content-Type': 'application/json'},
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '删除智能体失败')
}

/**
 * 获取智能体已绑定的 MCP 工具列表（二层结构：MCP → tools）
 *
 * GET /api/agent/manage/{id}/mcp-tools
 * 用于输入框手动模式选择 MCP 使用。
 */
export async function fetchAgentBoundMcpTools(agentId: number): Promise<ToolSourceVO[]> {
    const res = await fetch(`${BASE}/${agentId}/mcp-tools`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<ToolSourceVO[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取智能体 MCP 工具列表失败')
    return json.data
}

const MCP_BASE = '/api/mcp-servers'

/**
 * 获取所有已启用的 MCP 服务列表
 *
 * GET /api/mcp-servers?enabled=true
 * 复用全局 MCP API，返回 { id, name } 结构供选择
 */
export async function fetchEnabledMcpServers(): Promise<McpServerItem[]> {
    const res = await fetch(`${MCP_BASE}?enabled=true`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<McpServerItem[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取 MCP 列表失败')
    return json.data
}
