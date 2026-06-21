/**
 * 会话管理 API
 *
 * 对应后端 SessionController：
 *   POST   /api/session                   → 创建会话
 *   GET    /api/session/list?assistantId=  → 会话列表
 *   DELETE /api/session/{id}               → 删除会话
 *   PUT    /api/session/{id}/title         → 手动重命名
 *   POST   /api/session/{id}/generate-title→ AI 生成标题
 *   PUT    /api/session/{id}/pin           → 置顶
 *   PUT    /api/session/{id}/unpin         → 取消置顶
 *   GET    /api/session/{id}/messages      → 游标分页消息
 */
import type {ApiResult} from '@/types/chat'
import type {
    MessageVO,
    SessionCreateRequest,
    SessionVO,
    TitleGenerateRequest,
    TitleRenameRequest
} from '@/types/session'

const BASE = '/api/session'

export async function createSession(data: SessionCreateRequest): Promise<SessionVO> {
    const res = await fetch(BASE, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<SessionVO> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '创建会话失败')
    return json.data
}

export async function fetchSessionList(assistantId: number): Promise<SessionVO[]> {
    const res = await fetch(`${BASE}/list?assistantId=${assistantId}`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<SessionVO[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取会话列表失败')
    return json.data
}

export async function deleteSession(id: number): Promise<void> {
    const res = await fetch(`${BASE}/${id}`, {
        method: 'DELETE',
        headers: {'Content-Type': 'application/json'},
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '删除会话失败')
}

export async function renameTitle(id: number, data: TitleRenameRequest): Promise<SessionVO> {
    const res = await fetch(`${BASE}/${id}/title`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<SessionVO> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '重命名失败')
    return json.data
}

export async function generateTitle(id: number, data: TitleGenerateRequest): Promise<string> {
    const res = await fetch(`${BASE}/${id}/generate-title`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<string> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '生成标题失败')
    return json.data
}

export async function pinSession(id: number): Promise<void> {
    const res = await fetch(`${BASE}/${id}/pin`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '置顶失败')
}

export async function unpinSession(id: number): Promise<void> {
    const res = await fetch(`${BASE}/${id}/unpin`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<void> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '取消置顶失败')
}

export async function fetchMessages(sessionId: number, beforeId?: number): Promise<MessageVO[]> {
    const params = beforeId != null ? `?beforeId=${beforeId}` : ''
    const res = await fetch(`${BASE}/${sessionId}/messages${params}`)
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    const json: ApiResult<MessageVO[]> = await res.json()
    if (json.code !== 200) throw new Error(json.message || '获取消息失败')
    return json.data
}
