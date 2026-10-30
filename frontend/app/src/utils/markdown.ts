import MarkdownIt from 'markdown-it'

/**
 * AI 回答使用 Markdown。uni-app 的 rich-text 不依赖 DOM，
 * 因此这里把常用标签的样式内联，避免 App 端样式丢失。
 * html:false 同时规避模型输出原始 HTML 带来的风险。
 */
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: false,
})

const textColor = '#334155'
const mutedColor = '#64748b'

md.renderer.rules.paragraph_open = () =>
  `<p style="margin:0 0 10px;line-height:1.75;font-size:15px;color:${textColor};">`
md.renderer.rules.paragraph_close = () => '</p>'

md.renderer.rules.heading_open = (tokens, idx) => {
  const level = Number(tokens[idx].tag.slice(1)) || 1
  const sizes = [20, 18, 16, 15, 15, 15]
  return `<h${level} style="margin:14px 0 8px;font-size:${sizes[level - 1]}px;font-weight:700;line-height:1.4;color:#0f172a;">`
}
md.renderer.rules.heading_close = (tokens, idx) => `</${tokens[idx].tag}>`

md.renderer.rules.strong_open = () =>
  '<strong style="font-weight:700;color:#0f172a;">'
md.renderer.rules.strong_close = () => '</strong>'
md.renderer.rules.em_open = () => '<em style="font-style:italic;">'
md.renderer.rules.em_close = () => '</em>'

md.renderer.rules.bullet_list_open = () =>
  '<ul style="margin:6px 0 10px;padding-left:20px;">'
md.renderer.rules.bullet_list_close = () => '</ul>'
md.renderer.rules.ordered_list_open = () =>
  '<ol style="margin:6px 0 10px;padding-left:22px;">'
md.renderer.rules.ordered_list_close = () => '</ol>'
md.renderer.rules.list_item_open = () =>
  `<li style="margin:4px 0;line-height:1.7;font-size:15px;color:${textColor};">`
md.renderer.rules.list_item_close = () => '</li>'

md.renderer.rules.blockquote_open = () =>
  `<blockquote style="margin:8px 0;padding:8px 12px;border-left:3px solid #14b8a6;background:#f0fdfa;color:${mutedColor};font-size:14px;line-height:1.7;border-radius:0 8px 8px 0;">`
md.renderer.rules.blockquote_close = () => '</blockquote>'

md.renderer.rules.code_inline = (tokens, idx) => {
  const content = md.utils.escapeHtml(tokens[idx].content)
  return `<code style="padding:1px 5px;border-radius:4px;background:#f1f5f9;color:#0f766e;font-size:13px;">${content}</code>`
}
md.renderer.rules.fence = (tokens, idx) => {
  const content = md.utils.escapeHtml(tokens[idx].content)
  return `<pre style="margin:8px 0 12px;padding:12px;border-radius:8px;background:#0f172a;color:#e2e8f0;overflow:auto;font-size:12px;line-height:1.6;"><code>${content}</code></pre>`
}
md.renderer.rules.code_block = md.renderer.rules.fence!

md.renderer.rules.hr = () =>
  '<hr style="margin:16px 0;border:none;border-top:1px solid #e2e8f0;" />'

md.renderer.rules.link_open = (tokens, idx) => {
  const href = tokens[idx].attrGet('href') || '#'
  return `<a href="${md.utils.escapeHtml(String(href))}" style="color:#0f9d8a;text-decoration:underline;">`
}
md.renderer.rules.link_close = () => '</a>'

md.renderer.rules.table_open = () =>
  '<table style="width:100%;border-collapse:collapse;margin:8px 0 12px;font-size:13px;">'
md.renderer.rules.table_close = () => '</table>'
md.renderer.rules.thead_open = () => '<thead style="background:#f0fdfa;">'
md.renderer.rules.thead_close = () => '</thead>'
md.renderer.rules.tbody_open = () => '<tbody>'
md.renderer.rules.tbody_close = () => '</tbody>'
md.renderer.rules.tr_open = () => '<tr>'
md.renderer.rules.tr_close = () => '</tr>'
md.renderer.rules.th_open = () =>
  '<th style="padding:6px 8px;border:1px solid #e2e8f0;text-align:left;color:#0f172a;">'
md.renderer.rules.th_close = () => '</th>'
md.renderer.rules.td_open = () =>
  `<td style="padding:6px 8px;border:1px solid #e2e8f0;color:${textColor};">`
md.renderer.rules.td_close = () => '</td>'

export function renderMarkdown(content?: string | null): string {
  if (!content) return ''
  return md.render(content)
}

export function plainText(content?: string | null): string {
  if (!content) return ''
  return md
    .renderInline(content)
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .trim()
}
