/**
 * @author Eddie
 * @date 2026-07-04
 */

/**
 * 智能体会话管理 API
 *
 * 对应后端 AgentSessionController：
 *   POST   /api/agent/session?agentId=   → 创建会话
 *   GET    /api/agent/session/list?agentId=  → 会话列表
 *   DELETE /api/agent/session/{id}        → 删除会话
 *   PUT    /api/agent/session/{id}/title  → 手动重命名
 *   PUT    /api/agent/session/{id}/pin    → 置顶
 *   PUT    /api/agent/session/{id}/unpin  → 取消置顶
 */
import type {ApiResult} from '@/types/chat'
import type {PageResult, SessionVO} from '@/types/session'

const BASE = '/api/agent/session'

export async function createAgentSession(agentId: number): Promise<SessionVO> {
    const params = new URLSearchParams()
    params.set('agentId', String(agentId))
    const res = await fetch(`${BASE}?${params}`, {method: 'POST'})
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<SessionVO> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '创建智能体会话失败')
    return json.data
}

export async function fetchAgentSessionList(
    agentId: number,
    pageNum: number = 1,
    pageSize: number = 50,
    title?: string
): Promise<PageResult<SessionVO>> {
    const params = new URLSearchParams()
    params.set('agentId', String(agentId))
    params.set('pageNum', String(pageNum))
    params.set('pageSize', String(pageSize))
    if (title) params.set('title', title)
    const res = await fetch(`${BASE}/list?${params}`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<PageResult<SessionVO>> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取智能体会话列表失败')
    return json.data
}

export async function deleteAgentSession(id: number): Promise<void> {
    const res = await fetch(`${BASE}/${id}`, {method: 'DELETE'})
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '删除智能体会话失败')
}

export async function renameAgentSessionTitle(id: number, title: string): Promise<SessionVO> {
    const params = new URLSearchParams()
    params.set('title', title)
    const res = await fetch(`${BASE}/${id}/title?${params}`, {method: 'PUT'})
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<SessionVO> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '重命名智能体会话失败')
    return json.data
}

export async function pinAgentSession(id: number): Promise<void> {
    const res = await fetch(`${BASE}/${id}/pin`, {method: 'PUT'})
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '置顶智能体会话失败')
}

export async function unpinAgentSession(id: number): Promise<void> {
    const res = await fetch(`${BASE}/${id}/unpin`, {method: 'PUT'})
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '取消置顶智能体会话失败')
}

export async function generateAgentSessionTitle(id: number): Promise<string> {
    const res = await fetch(`${BASE}/${id}/generate-title`, {method: 'POST'})
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<string> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '生成智能体会话标题失败')
    return json.data
}
