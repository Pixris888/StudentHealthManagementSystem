import { API_ORIGIN } from '@/config'

/**
 * 后端本地存储返回 /uploads/xxx 相对路径；OSS 模式返回完整签名 URL。
 * App 端必须补全域名/IP，H5 同源代理时保持相对路径。
 */
export function resolveFileUrl(url?: string | null): string {
  if (!url) return ''
  const value = String(url).trim()
  if (!value) return ''
  if (/^(https?:|data:|blob:)/i.test(value)) return value
  if (value.startsWith('//')) return `https:${value}`
  if (value.startsWith('/')) return `${API_ORIGIN}${value}`
  return `${API_ORIGIN}/${value.replace(/^\/+/, '')}`
}
