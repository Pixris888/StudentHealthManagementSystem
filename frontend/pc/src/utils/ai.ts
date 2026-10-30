import type { TagType } from './dict'
import type {
  AnswerStatus,
  FeedbackStatus,
  LlmProvider,
  PlanStatus,
  PlanType,
} from '@/types/ai'

export interface AiOption<T extends number = number> {
  value: T
  label: string
  tagType: TagType
}

export const ANSWER_STATUS_OPTIONS: AiOption<AnswerStatus>[] = [
  { value: 0, label: '处理中', tagType: 'info' },
  { value: 1, label: '成功', tagType: 'success' },
  { value: 2, label: '失败', tagType: 'danger' },
  { value: 3, label: '已降级', tagType: 'warning' },
]

export const FEEDBACK_OPTIONS: AiOption<FeedbackStatus>[] = [
  { value: 0, label: '未评价', tagType: 'info' },
  { value: 1, label: '有帮助', tagType: 'success' },
  { value: 2, label: '没帮助', tagType: 'danger' },
]

export const PLAN_TYPE_OPTIONS: AiOption<PlanType>[] = [
  { value: 1, label: '作息计划', tagType: 'primary' },
  { value: 2, label: '运动计划', tagType: 'success' },
  { value: 3, label: '综合计划', tagType: 'warning' },
]

export const PLAN_STATUS_OPTIONS: AiOption<PlanStatus>[] = [
  { value: 0, label: '未采纳', tagType: 'info' },
  { value: 1, label: '已采纳', tagType: 'success' },
]

const ANSWER_STATUS_MAP = new Map(
  ANSWER_STATUS_OPTIONS.map((item) => [item.value, item]),
)
const FEEDBACK_MAP = new Map(
  FEEDBACK_OPTIONS.map((item) => [item.value, item]),
)
const PLAN_TYPE_MAP = new Map(
  PLAN_TYPE_OPTIONS.map((item) => [item.value, item]),
)
const PLAN_STATUS_MAP = new Map(
  PLAN_STATUS_OPTIONS.map((item) => [item.value, item]),
)

const PROVIDER_LABELS: Record<string, string> = {
  bailian: '阿里百炼',
  ollama: '本地 Ollama',
  rule: '规则兜底',
  mock: 'Mock 模式',
}

export function answerStatusLabel(value?: number | null): string {
  if (value == null) return '未知'
  return ANSWER_STATUS_MAP.get(value as AnswerStatus)?.label ?? '未知'
}

export function answerStatusTagType(value?: number | null): TagType {
  if (value == null) return 'info'
  return ANSWER_STATUS_MAP.get(value as AnswerStatus)?.tagType ?? 'info'
}

export function feedbackLabel(value?: number | null): string {
  if (value == null) return '未评价'
  return FEEDBACK_MAP.get(value as FeedbackStatus)?.label ?? '未评价'
}

export function feedbackTagType(value?: number | null): TagType {
  if (value == null) return 'info'
  return FEEDBACK_MAP.get(value as FeedbackStatus)?.tagType ?? 'info'
}

export function planTypeLabel(
  value?: number | null,
  fallback?: string,
): string {
  if (fallback) return fallback
  if (value == null) return '未知类型'
  return PLAN_TYPE_MAP.get(value as PlanType)?.label ?? '未知类型'
}

export function planStatusLabel(value?: number | null): string {
  if (value == null) return '未知'
  return PLAN_STATUS_MAP.get(value as PlanStatus)?.label ?? '未知'
}

export function planStatusTagType(value?: number | null): TagType {
  if (value == null) return 'info'
  return PLAN_STATUS_MAP.get(value as PlanStatus)?.tagType ?? 'info'
}

export function providerLabel(provider?: LlmProvider | string | null): string {
  if (!provider) return ''
  return PROVIDER_LABELS[provider] ?? provider
}

/** 学生端降级标签文案：优先说明实际备用方案 */
export function fallbackLabel(provider?: LlmProvider | string | null): string {
  if (provider === 'rule') return '规则建议'
  if (provider === 'ollama') return '本地备用模型'
  if (provider === 'mock') return '模拟回答'
  return '备用方案'
}

/** 毫秒转可读耗时 */
export function formatLatency(value?: number | null): string {
  if (value == null || Number.isNaN(Number(value))) return '—'
  const latency = Number(value)
  if (latency < 1000) return `${Math.round(latency)} ms`
  return `${(latency / 1000).toFixed(1)} s`
}

/** 后端百分比字段转展示文本 */
export function formatRate(value?: number | null): string {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return `${Number(value).toFixed(1)}%`
}

export function formatTokenCount(value?: number | null): string {
  if (value == null) return '—'
  return String(value)
}

interface AiErrorLike {
  code?: string
  message?: string
  response?: {
    status?: number
    data?: { msg?: string }
  }
}

/** 将 Axios / 业务错误转换为适合页面展示的中文提示 */
export function resolveAiError(
  error: unknown,
  fallback = '操作失败，请稍后重试',
): string {
  const err = error as AiErrorLike | null
  if (err?.code === 'ECONNABORTED' || err?.code === 'ETIMEDOUT') {
    return 'AI 响应超时，请稍后重试'
  }
  const status = err?.response?.status
  if (status === 403) return '没有访问权限'
  if (status === 401) return '登录状态已失效，请重新登录'
  const backendMessage = err?.response?.data?.msg
  if (backendMessage) return backendMessage
  if (
    err?.message &&
    err.message !== 'Network Error' &&
    !err.message.startsWith('Request failed with status code')
  ) {
    return err.message
  }
  if (err?.message === 'Network Error') {
    return '网络异常，请确认后端服务已启动后重试'
  }
  if (status) return `请求失败（${status}）`
  return fallback
}
