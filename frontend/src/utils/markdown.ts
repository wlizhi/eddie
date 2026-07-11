/**
 * @author Eddie
 * @date 2026-06-21
 */

/**
 * Markdown 工具函数
 *
 * 使用 marked 将 Markdown 文本渲染为 HTML。
 * highlight.js 提供代码语法高亮。
 * 自定义 marked renderer 为代码块添加语言标签和复制按钮容器。
 */
import {Marked, Renderer} from 'marked'
import hljs from 'highlight.js'

const marked = new Marked({
    breaks: true,
    gfm: true,
    renderer: getRenderer(),
})

/** 获取自定义 Renderer，为代码块添加头部栏（语言标签 + 复制按钮） */
function getRenderer(): Renderer {
    const renderer = new Renderer()

    // 覆写 code 块渲染
    renderer.code = ({text, lang}) => {
        const language = lang && hljs.getLanguage(lang) ? lang : ''
        let highlighted: string
        try {
            highlighted = language
                ? hljs.highlight(text, {language}).value
                : hljs.highlightAuto(text).value
        } catch {
            highlighted = escapeHtml(text)
        }

        const langLabel = language
            ? `<span class="code-lang-label">${escapeHtml(language)}</span>`
            : ''

        // 行数超过阈值时默认折叠
        const lineCount = text.split('\n').length
        const defaultCollapsed = lineCount > 15

        return `<div class="code-block-wrapper${defaultCollapsed ? ' collapsed' : ''}">
  <div class="code-block-header">
    <div class="code-header-left" onclick="(function(el){
      el.closest('.code-block-wrapper').classList.toggle('collapsed');
    })(this)">
      <svg class="code-toggle-icon" width="1em" height="1em" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
      ${langLabel}
    </div>
    <div class="code-header-right">
      <button class="code-copy-btn" title="复制代码">
        <svg class="copy-icon" width="1em" height="1em" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>
        <svg class="check-icon" width="1em" height="1em" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
      </button>
    </div>
  </div>
  <div class="code-block-body">
    <pre><code class="hljs${language ? ' language-' + escapeHtml(language) : ''}">${highlighted}</code></pre>
  </div>
</div>`
    }

    return renderer
}

function escapeHtml(text: string): string {
    return text
        .replace(/&/g, '&')
        .replace(/</g, '<')
        .replace(/>/g, '>')
        .replace(/"/g, '"')
}

/**
 * 将 Markdown 文本渲染为 HTML 字符串
 * @param text 原始 Markdown 文本
 * @returns 渲染后的 HTML，解析失败时返回原文本
 */
export function renderMd(text: string): string {
    if (!text) return ''
    try {
        return marked.parse(text) as string
    } catch {
        return text
    }
}

// ===== 代码块复制按钮：全局事件委托 =====

/**
 * 初始化代码块复制按钮的点击事件。
 * 使用事件委托监听 document 上的 click，无需在每次 renderMd 时重新绑定。
 */
function initCodeCopy(): void {
    document.addEventListener('click', async (e) => {
        const btn = (e.target as HTMLElement).closest<HTMLButtonElement>('.code-copy-btn')
        if (!btn) return

        const wrapper = btn.closest<HTMLElement>('.code-block-wrapper')
        if (!wrapper) return

        const codeElement = wrapper.querySelector<HTMLElement>('pre code')
        if (!codeElement) return

        // 获取代码文本（highlight.js 转义了 HTML，需要从 textContent 获取原始文本）
        const codeText = codeElement.textContent ?? ''

        try {
            await navigator.clipboard.writeText(codeText)
            btn.classList.add('copied')
            setTimeout(() => btn.classList.remove('copied'), 2000)
        } catch {
            // 降级方案：部分浏览器（非 HTTPS）不支持 clipboard API
            const textarea = document.createElement('textarea')
            textarea.value = codeText
            textarea.style.position = 'fixed'
            textarea.style.opacity = '0'
            document.body.appendChild(textarea)
            textarea.select()
            document.execCommand('copy')
            document.body.removeChild(textarea)
            btn.classList.add('copied')
            setTimeout(() => btn.classList.remove('copied'), 2000)
        }
    })
}

// 模块加载时自动初始化
initCodeCopy()
