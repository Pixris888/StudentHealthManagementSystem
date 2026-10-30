/** AI 健康助手模块类型定义（字段以后端接口契约为准） */

export type AnswerStatus = 0 | 1 | 2 | 3
export type FeedbackStatus = 0 | 1 | 2
export type PlanType = 1 | 2 | 3
export type PlanStatus = 0 | 1
export type LlmProvider = 'bailian' | 'ollama' | 'rule' | 'mock'

/** 学生提问请求 */
export interface AiAskDTO {
  question: string
  /** 首次提问不传，后端生成后回传 */
  sessionId?: string
}

/** 回答反馈请求 */
export interface AiFeedbackDTO {
  helpful: boolean
  remark?: string
}

/** 实时回答返回对象 */
export interface AiAnswerVO {
  consultationId: number
  sessionId: string
  answer: string
  sources?: string[]
  knowledgeSource?: string
  modelName?: string
  provider?: LlmProvider
  answerStatus: AnswerStatus
  fallback: boolean
  emergency: boolean
  consultTime?: string
}

/** 问答记录（学生历史 / 管理端列表、详情） */
export interface AiConsultationVO {
  id: number
  userId?: number
  studentNo?: string
  realName?: string
  sessionId?: string
  question: string
  answer?: string
  knowledgeSource?: string
  modelName?: string
  provider?: LlmProvider
  answerStatus: AnswerStatus
  errorMsg?: string
  promptTokens?: number
  completionTokens?: number
  latencyMs?: number
  feedback: FeedbackStatus
  feedbackRemark?: string
  retrievedIds?: string
  /** 实体中为 0/1，AiAnswerVO 中为 boolean */
  emergency: 0 | 1
  fallback: boolean
  consultTime?: string
  createTime?: string
}

/** 问答记录查询条件 */
export interface AiConsultationQuery {
  page?: number
  size?: number
  keyword?: string
  answerStatus?: AnswerStatus
  feedback?: FeedbackStatus
  startDate?: string
  endDate?: string
  /** 管理端按用户 ID 筛选 */
  userId?: number
}

/** 健康计划 */
export interface AiHealthPlanVO {
  id: number
  userId?: number
  planType: PlanType
  planTypeName: string
  planTitle?: string
  planContent?: string
  planData?: string
  baseCheckinDate?: string
  startDate?: string
  endDate?: string
  status: PlanStatus
  modelName?: string
  provider?: LlmProvider
  adoptedTime?: string
  createTime?: string
}

export interface AiPlanGenerateDTO {
  planType: PlanType
}

export interface AiPlanQuery {
  page?: number
  size?: number
  planType?: PlanType
  status?: PlanStatus
}

/** AI 使用统计 */
export interface AiStatsVO {
  days: number
  total: number
  todayCount: number
  fallbackCount: number
  fallbackRate: number
  emergencyCount: number
  feedbackCount: number
  helpfulCount: number
  satisfactionRate: number
  avgLatencyMs: number
  unansweredCount: number
}

/**
 * 聊天界面消息。
 * 实时回答使用 answer；历史会话使用 consultation（其中没有 sources 数组）。
 */
export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  /** 已收到首字，正在接收增量 */
  streaming?: boolean
  /** 用户主动停止生成 */
  stopped?: boolean
  error?: boolean
  errorMessage?: string
  answer?: AiAnswerVO
  consultation?: AiConsultationVO
  question?: string
  feedback?: FeedbackStatus
  createdAt: string
}

/** SSE meta 事件 */
export interface AiStreamMeta {
  consultationId: number
  sessionId: string
  sources?: string[]
  knowledgeSource?: string
  emergency?: boolean
  consultTime?: string
}

/** SSE error 事件 */
export interface AiStreamError {
  code: number
  message: string
  consultationId?: number
  partial?: boolean
}

export type AiStreamEventName = 'meta' | 'delta' | 'done' | 'error'

export interface AiStreamHandlers {
  onMeta?: (meta: AiStreamMeta) => void
  onDelta?: (content: string) => void
  onDone?: (answer: AiAnswerVO) => void
  onError?: (error: AiStreamError) => void
}

/** 流式请求最终结果；fallback 表示应回退同步 /ask */
export type AiStreamResult =
  | { status: 'done'; answer: AiAnswerVO }
  | { status: 'error'; error: AiStreamError }
  | { status: 'aborted' }
  | { status: 'fallback'; code: number; message: string }
