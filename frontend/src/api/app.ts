/**
 * @author Eddie
 * {@code @date} 2026-07-26
 */

import type {ApiResult} from '@/types/chat'

const BASE = '/api/app'

export interface AppVersionVO {
  currentVersion: string
  repository: string
}

export interface UpdateCheckResult {
  hasUpdate: boolean
  currentVersion: string
  latestVersion: string | null
  downloadUrl: string
  releaseNotesUrl: string
  releaseNotes: string | null
  message: string
  repository: string
}

/**
 * 获取当前版本信息
 * GET /api/app/version
 */
export async function fetchAppVersion(): Promise<AppVersionVO> {
  const res = await fetch(`${BASE}/version`)
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
  const json: ApiResult<AppVersionVO> = await res.json()
  if (json.code !== 200) throw new Error(json.message || '获取版本信息失败')
  return json.data
}

/**
 * 检查 GitHub 上是否有新版本
 * GET /api/app/check-update
 */
export async function checkUpdate(): Promise<UpdateCheckResult> {
  const res = await fetch(`${BASE}/check-update`)
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
  const json: ApiResult<UpdateCheckResult> = await res.json()
  if (json.code !== 200) throw new Error(json.message || '检查更新失败')
  return json.data
}
