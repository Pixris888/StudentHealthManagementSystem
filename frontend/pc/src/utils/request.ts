import axios, { type AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse, LoginResult } from '@/types/system'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './auth'

/** 与后端 application.yml 的 context-path 对齐，开发环境由 Vite 代理到 8080 */
const baseURL = '/api'

const service = axios.create({
  baseURL,
  timeout: 15000,
})

let refreshPromise: Promise<string> | null = null
let redirecting = false

function redirectToLogin(): void {
  clearTokens()
  if (redirecting) return
  redirecting = true
  const current = window.location.pathname + window.location.search
  const target = window.location.pathname.startsWith('/login')
    ? '/login'
    : `/login?redirect=${encodeURIComponent(current)}`
  window.location.href = target
}

async function refreshAccessToken(): Promise<string> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('登录状态已失效，请重新登录')
  }
  // 使用原生 axios，避免再次进入拦截器造成循环
  const { data } = await axios.post<ApiResponse<LoginResult>>(`${baseURL}/auth/refresh`, {
    refreshToken,
  })
  if (data.code !== 200 || !data.data?.accessToken || !data.data?.refreshToken) {
    throw new Error(data.msg || '登录状态已失效，请重新登录')
  }
  setTokens(data.data.accessToken, data.data.refreshToken)
  return data.data.accessToken
}

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    // Excel 等文件下载需要保留完整 response，以便读取 Content-Disposition
    if (response.config.responseType === 'blob') {
      return response
    }
    const payload = response.data as ApiResponse
    // 后端所有接口统一被 RestResponseAdvice 包装为 { code, msg, data }
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code === 200) {
        return payload.data
      }
      if (payload.code === 401) {
        redirectToLogin()
      } else {
        ElMessage.error(payload.msg || '操作失败')
      }
      return Promise.reject(new Error(payload.msg || '操作失败'))
    }
    return response.data
  },
  async (error: AxiosError<ApiResponse>) => {
    const config = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    const status = error.response?.status
    const isAuthRequest = Boolean(
      config?.url?.includes('/auth/login') ||
        config?.url?.includes('/auth/register') ||
        config?.url?.includes('/auth/refresh'),
    )

    // Access Token 过期：使用 Refresh Token 换新后重放一次原请求
    if (status === 401 && config && !config._retry && !isAuthRequest) {
      config._retry = true
      try {
        if (!refreshPromise) {
          refreshPromise = refreshAccessToken().finally(() => {
            refreshPromise = null
          })
        }
        const token = await refreshPromise
        config.headers.Authorization = `Bearer ${token}`
        return service(config)
      } catch (refreshError) {
        redirectToLogin()
        return Promise.reject(refreshError)
      }
    }

    if (status === 401) {
      redirectToLogin()
    } else {
      const isTimeout =
        error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT'
      const message =
        error.response?.data?.msg ||
        (isTimeout
          ? '请求超时，请稍后重试'
          : status === 403
            ? '没有访问权限'
            : status
              ? `请求失败（${status}）`
              : '网络异常，请确认后端服务已启动')
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

export function get<T>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config }) as unknown as Promise<T>
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config) as unknown as Promise<T>
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config) as unknown as Promise<T>
}

export function del<T>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, { params, ...config }) as unknown as Promise<T>
}

export default service
