/**
 * 接口地址配置
 *
 * - H5 开发：默认使用 Vite 代理 `/api`，无需修改。
 * - App 真机调试：改成电脑的局域网 IP，例如 http://192.168.1.20:8888/api，
 *   并确保手机和电脑在同一网络。
 * - 正式打包：必须通过 VITE_API_BASE_URL 指定对外可访问的 API 地址，
 *   例如 https://example.com/api。
 *
 * 注意：App 包里的地址是编译期写死的，不能用 127.0.0.1/localhost——
 * 在手机上它指向手机自己，而不是你的服务器。
 *
 * 也可以在构建时通过 VITE_API_BASE_URL 覆盖，优先级最高。
 */
export const APP_FALLBACK_API_BASE_URL = 'http://127.0.0.1:8888/api'

const envBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim()

let baseUrl = envBaseUrl || ''

// #ifdef H5
// H5 开发环境走 Vite 代理，生产环境建议 Nginx 反向代理 /api
baseUrl = baseUrl || '/api'
// #endif

// #ifndef H5
// App 端没有同源代理，必须使用绝对地址
baseUrl = baseUrl || APP_FALLBACK_API_BASE_URL
// #endif

export const API_BASE_URL = baseUrl.replace(/\/+$/, '')

/**
 * 文件资源根地址。
 * 后端本地存储返回 /uploads/xxx 相对路径，需要补上域名/IP；
 * OSS 模式返回完整签名 URL，这里不会处理。
 */
export const API_ORIGIN = /^https?:\/\//i.test(API_BASE_URL)
  ? API_BASE_URL.replace(/\/api$/i, '')
  : ''

export const APP_VERSION = '1.0.0'
