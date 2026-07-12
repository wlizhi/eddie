/**
 * @author Eddie
 * {@code @date} 2026-07-11
 */

/**
 * 工具调用结果格式化工具。
 *
 * 将工具返回的原始文本处理为 Markdown 格式：
 * - 合法 JSON → 包裹 ```json 代码块，highlight.js 自动高亮
 * - 含 JSON 片段的文本（如 "查询完成\n\n[{...}]"）→ 识别并包裹 JSON 部分
 * - MCP 信封格式 {"content":[...]} → 提取内容后逐段处理
 * - 纯文本 → 原样返回，renderMd 自然渲染其中的 Markdown/代码块
 */

/**
 * 格式化工具返回结果文本。
 *
 * @param result 工具返回的原始结果字符串
 * @returns 处理后的 Markdown 字符串
 */
export function formatToolResult(result: string): string {
  if (!result) return ''

  // 注意：不要在这里调用 fixNewlines()！它会将 JSON 字符串值中的 \n（合法转义序列）
  // 替换为真实换行符，导致 JSON.parse() 失败，语法高亮失效。
  // fixNewlines 仅在确认文本不是 JSON 后的降级路径中调用（见 wrapJsonBlock 末尾）。
  const text = result

  // 1. 尝试拆包 ApiResult 通用结构（{code: 200, data: ...}）
  //    成功 → data 进入后续格式化；失败 → 继续原流程
  const unwrapped = unwrapApiResult(text)
  if (unwrapped !== null) return unwrapped

  // 2. MCP 信封格式：{"content":[{"type":"text","text":"..."}],"isError":false}
  //    提取 text/resource 内容后逐段格式化
  try {
    const parsed = JSON.parse(text)
    if (parsed.content && Array.isArray(parsed.content)) {
      const texts = parsed.content
        .filter((c: any) => c.type === 'text' || c.type === 'resource')
        .map((c: any) => c.text ?? c.resource ?? '')
        .filter(Boolean)
      if (texts.length > 0) {
        return texts.map((t: string) => wrapJsonBlock(t)).join('\n\n')
      }
    }
  } catch {
    // 非 MCP 格式，继续后续处理
  }

  // 3. 普通文本 → 尝试识别其中的 JSON
  return wrapJsonBlock(text)
}

/**
 * 尝试拆包 ApiResult 通用结构。
 *
 * 如果结果是 {"code":200, "data":...}，提取 data 后重新走格式化逻辑：
 * - data 为字符串 → 传入 wrapJsonBlock 检测是 JSON 还是 Markdown
 * - data 为对象/数组 → JSON pretty-print + 高亮
 * - data 为 null/undefined → 返回空串
 *
 * 非 ApiResult 或 code !== 200 时返回 null，由调用方继续原逻辑。
 */
function unwrapApiResult(text: string): string | null {
  try {
    const parsed = JSON.parse(text)
    if (parsed && typeof parsed === 'object' && 'code' in parsed && 'data' in parsed) {
      if (parsed.code === 200) {
        const data = parsed.data
        if (data === null || data === undefined) return ''
        // data 是字符串 → 用 wrapJsonBlock 检测是 JSON 还是 Markdown
        if (typeof data === 'string') return wrapJsonBlock(data)
        // data 是对象/数组 → 直接 JSON pretty-print 高亮
        return '```json\n' + JSON.stringify(data, null, 2) + '\n```'
      }
      // code !== 200，不拆包，显示完整错误响应
      return null
    }
  } catch {
    // 非 JSON，不处理
  }
  return null
}

/**
 * 如果内容是 JSON 则用 ```json 代码块包裹（pretty-print 美化），否则原样返回。
 * 支持整体 JSON 以及文本尾部包含 JSON 片段两种场景。
 */
function wrapJsonBlock(text: string): string {
  const trimmed = text.trim()
  if (!trimmed) return text

  // 整体是合法 JSON → pretty-print + 代码块高亮
  try {
    const parsed = JSON.parse(trimmed)
    return '```json\n' + JSON.stringify(parsed, null, 2) + '\n```'
  } catch {
    // 非完整 JSON，继续
  }

  // 尾部包含 [...] 或 {...} JSON 片段
  // 如 "查询完成（共 9 行）\n\n[{\"name\":\"ai_assistant\"}]"
  const m = trimmed.match(/^([\s\S]*?)((?:\[[\s\S]*\]|\{[\s\S]*\})\s*)$/)
  if (m) {
    const jsonCandidate = m[2].trim()
    try {
      const parsed = JSON.parse(jsonCandidate)
      const prefix = m[1].trim()
      const prettyJson = '```json\n' + JSON.stringify(parsed, null, 2) + '\n```'
      if (prefix) {
        return prefix + '\n\n' + prettyJson
      }
      return prettyJson
    } catch {
      // 不是合法 JSON，原样返回
    }
  }

  // 非 JSON → 修复转义换行符后返回，让 renderMd 渲染
  return fixNewlines(trimmed)
}

/**
 * 修复内容中可能存在的转义换行符 → 真实换行符。
 * 某些外部工具返回的文本中，\n 以字面量形式存在而非真实换行符，
 * 需要转换以便 Markdown 渲染器正确解析。
 */
function fixNewlines(text: string): string {
  return text.replace(/\\n/g, '\n')
}
