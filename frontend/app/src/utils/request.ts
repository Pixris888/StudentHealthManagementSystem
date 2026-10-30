import { API_BASE_URL } from '@/config'
import type { ApiResponse, LoginResult } from '@/types/system'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './auth'

export class RequestError extends Error {
  code?: number

  constructor(message: string, code?: number) {
    super(message)
    this.name = 'RequestError'
    this.code = code
  }
}

export interface RequestConfig {
  timeout?: number
  silent?: boolean
  header?: Record<string, string>
}

interface RequestOptions extends RequestConfig {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: unknown
  params?: Record<string, unknown>
  retried?: boolean
}

let refreshPromise: Promise<string> | null = null
let redirecting = false

function toast(message: string): void {
  uni.showToast({ title: message, icon: 'none', duration: 2600 })
}

function redirectToLogin(): void {
  clearTokens()
  if (redirecting) return
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]?.route ?? ''
  if (current.includes('pages/login/index')) return
  redirecting = true
  setTimeout(() => {
    redirecting = false
  }, 1500)
  uni.reLaunch({ url: '/pages/login/index' })
}

function buildQuery(params?: Record<string, unknown>): string {
  if (!params) return ''
  const parts: string[] = []
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
  })
  return parts.length ? `?${parts.join('&')}` : ''
}

function isAuthRequest(url: string): boolean {
  return (
    url.includes('/auth/login') ||
    url.includes('/auth/register') ||
    url.includes('/auth/refresh')
  )
}

function refreshAccessToken(): Promise<string> {
  if (refreshPromise) return refreshPromise
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return Promise.reject(new RequestError('登录状态已失效，请重新登录', 401))
  }

  refreshPromise = new Promise<string>((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}/auth/refresh`,
      method: 'POST',
      data: { refreshToken },
      header: { 'Content-Type': 'application/json' },
      timeout: 15000,
      success: (res) => {
        const payload = res.data as ApiResponse<LoginResult>
        if (
          res.statusCode === 200 &&
          payload?.code === 200 &&
          payload.data?.accessToken
        ) {
          setTokens(payload.data.accessToken, payload.data.refreshToken || refreshToken)
          resolve(payload.data.accessToken)
          return
        }
        reject(new RequestError(payload?.msg || '登录状态已失效，请重新登录', 401))
      },
      fail: (error) => {
        reject(new RequestError(error.errMsg || '网络异常，请稍后重试', 0))
      },
    })
  }).finally(() => {
    refreshPromise = null
  })

  return refreshPromise
}

async function retryAfterRefresh<T>(options: RequestOptions): Promise<T> {
  try {
    await refreshAccessToken()
  } catch (error) {
    redirectToLogin()
    throw error
  }
  return request<T>({ ...options, retried: true, silent: options.silent })
}

export function request<T>(options: RequestOptions): Promise<T> {
  const method = options.method ?? 'GET'
  const url = `${API_BASE_URL}${options.url}${buildQuery(options.params)}`
  const header: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.header || {}),
  }
  const token = getAccessToken()
  if (token) header.Authorization = `Bearer ${token}`

  return new Promise<T>((resolve, reject) => {
    uni.request({
      url,
      method,
      data: options.data as string | Record<string, unknown> | ArrayBuffer | undefined,
      header,
      timeout: options.timeout ?? 20000,
      success: async (res) => {
        const status = res.statusCode
        const payload = res.data as ApiResponse<unknown> | unknown

        if (payload && typeof payload === 'object' && 'code' in payload) {
          const body = payload as ApiResponse<unknown>
          if (body.code === 200) {
            resolve(body.data as T)
            return
          }
          if (body.code === 401) {
            if (isAuthRequest(options.url) || options.retried) {
              if (!isAuthRequest(options.url)) redirectToLogin()
              const message = body.msg || '登录状态已失效，请重新登录'
              if (!options.silent) toast(message)
              reject(new RequestError(message, 401))
              return
            }
            try {
              resolve(await retryAfterRefresh<T>(options))
            } catch (error) {
              reject(error)
            }
            return
          }
          const message = body.msg || '操作失败'
          if (!options.silent) toast(message)
          reject(new RequestError(message, body.code))
          return
        }

        if (status === 401) {
          if (isAuthRequest(options.url) || options.retried) {
            redirectToLogin()
            reject(new RequestError('登录状态已失效，请重新登录', 401))
            return
          }
          try {
            resolve(await retryAfterRefresh<T>(options))
          } catch (error) {
            reject(error)
          }
          return
        }

        if (status >= 200 && status < 300) {
          resolve(payload as T)
          return
        }

        const message =
          status === 403
            ? '没有访问权限'
            : status
              ? `请求失败（${status}）`
              : '请求失败'
        if (!options.silent) toast(message)
        reject(new RequestError(message, status))
      },
      fail: (error) => {
        const message = error.errMsg?.includes('timeout')
          ? '请求超时，请稍后重试'
          : '网络异常，请确认后端服务已启动'
        if (!options.silent) toast(message)
        reject(new RequestError(message, 0))
      },
    })
  })
}

export function get<T>(
  url: string,
  params?: Record<string, unknown>,
  config?: RequestConfig,
): Promise<T> {
  return request<T>({ url, method: 'GET', params, ...config })
}

export function post<T>(
  url: string,
  data?: unknown,
  config?: RequestConfig,
): Promise<T> {
  return request<T>({ url, method: 'POST', data, ...config })
}

export function put<T>(
  url: string,
  data?: unknown,
  config?: RequestConfig,
): Promise<T> {
  return request<T>({ url, method: 'PUT', data, ...config })
}

export function del<T>(
  url: string,
  params?: Record<string, unknown>,
  config?: RequestConfig,
): Promise<T> {
  return request<T>({ url, method: 'DELETE', params, ...config })
}

export interface UploadOptions {
  url: string
  filePath: string
  name?: string
  formData?: Record<string, string>
  timeout?: number
  silent?: boolean
}

export function upload<T>(options: UploadOptions): Promise<T> {
  const doUpload = (retried = false): Promise<T> =>
    new Promise<T>((resolve, reject) => {
      const header: Record<string, string> = {}
      const token = getAccessToken()
      if (token) header.Authorization = `Bearer ${token}`

      uni.uploadFile({
        url: `${API_BASE_URL}${options.url}`,
        filePath: options.filePath,
        name: options.name ?? 'file',
        formData: options.formData,
        header,
        timeout: options.timeout ?? 60000,
        success: async (res) => {
          let payload: ApiResponse<unknown> | null = null
          try {
            payload = JSON.parse(res.data) as ApiResponse<unknown>
          } catch {
            payload = null
          }

          if (res.statusCode === 401 || payload?.code === 401) {
            if (retried) {
              redirectToLogin()
              reject(new RequestError('登录状态已失效，请重新登录', 401))
              return
            }
            try {
              await refreshAccessToken()
              resolve(await doUpload(true))
            } catch (error) {
              redirectToLogin()
              reject(error)
            }
            return
          }

          if (payload?.code === 200) {
            resolve(payload.data as T)
            return
          }

          const message = payload?.msg || `上传失败（${res.statusCode}）`
          if (!options.silent) toast(message)
          reject(new RequestError(message, payload?.code))
        },
        fail: (error) => {
          const message = error.errMsg?.includes('timeout')
            ? '上传超时，请稍后重试'
            : '上传失败，请检查网络'
          if (!options.silent) toast(message)
          reject(new RequestError(message, 0))
        },
      })
    })

  return doUpload()
}

export function download(
  url: string,
  filename: string,
  options?: { silent?: boolean; timeout?: number },
): Promise<string> {
  const doDownload = (retried = false): Promise<string> =>
    new Promise<string>((resolve, reject) => {
      const header: Record<string, string> = {}
      const token = getAccessToken()
      if (token) header.Authorization = `Bearer ${token}`

      uni.downloadFile({
        url: `${API_BASE_URL}${url}`,
        header,
        timeout: options?.timeout ?? 90000,
        success: async (res) => {
          if (res.statusCode === 401) {
            if (retried) {
              redirectToLogin()
              reject(new RequestError('登录状态已失效，请重新登录', 401))
              return
            }
            try {
              await refreshAccessToken()
              resolve(await doDownload(true))
            } catch (error) {
              redirectToLogin()
              reject(error)
            }
            return
          }

          if (res.statusCode !== 200) {
            const message = `下载失败（${res.statusCode}）`
            if (!options?.silent) toast(message)
            reject(new RequestError(message, res.statusCode))
            return
          }

          // #ifdef H5
          try {
            const anchor = document.createElement('a')
            anchor.href = res.tempFilePath
            anchor.download = filename
            document.body.appendChild(anchor)
            anchor.click()
            document.body.removeChild(anchor)
            resolve(res.tempFilePath)
          } catch {
            const message = '当前浏览器不支持直接下载'
            if (!options?.silent) toast(message)
            reject(new RequestError(message))
          }
          // #endif

          // #ifndef H5
          uni.openDocument({
            filePath: res.tempFilePath,
            showMenu: true,
            success: () => resolve(res.tempFilePath),
            fail: () => {
              const message = '文件已下载，但当前设备无法打开该格式'
              if (!options?.silent) toast(message)
              reject(new RequestError(message))
            },
          })
          // #endif
        },
        fail: (error) => {
          const message = error.errMsg?.includes('timeout')
            ? '下载超时，请稍后重试'
            : '下载失败，请检查网络'
          if (!options?.silent) toast(message)
          reject(new RequestError(message, 0))
        },
      })
    })

  return doDownload()
}
