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

  const text = fixNewlines(result)

  // 1. MCP 信封格式：{"content":[{"type":"text","text":"..."}],"isError":false}
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

  // 2. 普通文本 → 尝试识别其中的 JSON
  return wrapJsonBlock(text)
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

  return text
}

/**
 * 修复内容中可能存在的转义换行符 → 真实换行符。
 * 某些外部工具返回的文本中，\n 以字面量形式存在而非真实换行符，
 * 需要转换以便 Markdown 渲染器正确解析。
 */
function fixNewlines(text: string): string {
  return text.replace(/\\n/g, '\n')
}
