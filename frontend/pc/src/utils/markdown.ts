import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

/**
 * AI 回答与健康计划正文均为 Markdown。
 * 禁止原始 HTML，渲染结果必须再经过 DOMPurify 清洗。
 */
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

/**
 * 模型有时会把整段回答 / 计划包在 ```markdown ... ``` 中，
 * 直接渲染会显示成代码块。这里只剥离包裹整段内容的 markdown 围栏，
 * 不处理代码语言围栏或正文中部的围栏。
 */
function unwrapMarkdownFence(source: string): string {
  const trimmed = source.trim()
  // 允许围栏后还有免责声明等正文；只剥离开头的 markdown 围栏
  const match = trimmed.match(
    /^```(?:markdown|md)[ \t]*\r?\n([\s\S]*?)\r?\n```[ \t]*(?:\r?\n)?/i,
  )
  if (!match) return source
  const body = match[1].trim()
  const rest = trimmed.slice(match[0].length).trim()
  return rest ? `${body}\n\n${rest}` : body
}

export function renderMarkdown(source?: string | null): string {
  if (!source) return ''
  return DOMPurify.sanitize(md.render(unwrapMarkdownFence(source)), {
    USE_PROFILES: { html: true },
  })
}
