import { ElMessage } from 'element-plus'
import request from '@/utils/request'

function parseFilename(disposition: string): string {
  if (!disposition) return ''
  const utf8 = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8?.[1]) {
    try {
      return decodeURIComponent(utf8[1].trim())
    } catch {
      return utf8[1].trim()
    }
  }
  const normal = disposition.match(/filename="?([^";]+)"?/i)
  if (normal?.[1]) {
    try {
      return decodeURIComponent(normal[1].trim())
    } catch {
      return normal[1].trim()
    }
  }
  return ''
}

/**
 * 下载二进制文件：保留响应头解析文件名，并处理后端以 JSON 返回的业务错误。
 */
export async function downloadFile(
  url: string,
  params: Record<string, unknown>,
  fallbackName: string,
  timeout = 15000,
): Promise<boolean> {
  try {
    const response = (await request.get(url, {
      params,
      responseType: 'blob',
      timeout,
    })) as unknown as {
      data: Blob
      headers?: Record<string, string>
    }

    const blob = response.data
    if (!(blob instanceof Blob)) {
      ElMessage.error('下载失败：响应格式不正确')
      return false
    }

    // 后端业务错误可能仍以 JSON 返回
    if (
      blob.type.includes('application/json') ||
      blob.type.includes('text/plain')
    ) {
      const text = await blob.text()
      try {
        const body = JSON.parse(text) as { msg?: string }
        ElMessage.error(body.msg || '下载失败')
      } catch {
        ElMessage.error(text || '下载失败')
      }
      return false
    }

    const disposition =
      response.headers?.['content-disposition'] ??
      response.headers?.['Content-Disposition'] ??
      ''
    const filename = parseFilename(disposition) || fallbackName
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = filename
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1500)
    ElMessage.success('文件已开始下载')
    return true
  } catch {
    // HTTP 错误提示由 request 拦截器统一处理
    return false
  }
}

/** 下载 Excel（保留原有调用方式） */
export function downloadExcel(
  url: string,
  params: Record<string, unknown>,
  fallbackName: string,
): Promise<boolean> {
  return downloadFile(url, params, fallbackName)
}
