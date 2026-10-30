const MOBILE_PATTERN = /Android|iPhone|iPad|iPod|HarmonyOS|Mobile/i

export function isMobileDevice(): boolean {
  return MOBILE_PATTERN.test(navigator.userAgent)
}

export function isLocalHost(): boolean {
  const host = window.location.hostname
  return (
    host === 'localhost' ||
    host === '127.0.0.1' ||
    host === '0.0.0.0' ||
    host === '::1' ||
    host === '[::1]'
  )
}

/**
 * 解析学生端下载页地址：
 * 1. 优先使用 VITE_APP_DOWNLOAD_URL；
 * 2. 否则使用当前站点同源的 /download/，避免把服务器 IP 写死在构建产物里。
 */
export function resolveAppDownloadUrl(): string {
  const configured = (import.meta.env.VITE_APP_DOWNLOAD_URL ?? '').trim()
  if (configured) {
    try {
      return new URL(configured, window.location.origin).toString()
    } catch {
      return configured
    }
  }
  return `${window.location.origin}/download/`
}

/**
 * 手机访问且不是本机/开发地址时，直接跳下载页；否则由页面弹出二维码。
 */
export function shouldOpenDownloadDirectly(): boolean {
  return isMobileDevice() && !import.meta.env.DEV && !isLocalHost()
}
