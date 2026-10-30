export type AnswerStatus = 0 | 1 | 2 | 3
export type FeedbackStatus = 0 | 1 | 2
export type PlanType = 1 | 2 | 3
export type PlanStatus = 0 | 1
export type LlmProvider = 'bailian' | 'ollama' | 'rule' | 'mock'

export interface AiAskDTO {
  question: string
  sessionId?: string
}

export interface AiFeedbackDTO {
  helpful: boolean
  remark?: string
}

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
  feedback: FeedbackStatus
  feedbackRemark?: string
  emergency: 0 | 1
  fallback: boolean
  consultTime?: string
  createTime?: string
}

export interface AiConsultationQuery {
  page?: number
  size?: number
  keyword?: string
  answerStatus?: AnswerStatus
  feedback?: FeedbackStatus
  startDate?: string
  endDate?: string
}

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

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  /** 已经收到首字，正在接收增量 */
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

/** 流式请求的最终结果；fallback 表示应该回退同步 /ask */
export type AiStreamResult =
  | { status: 'done'; answer: AiAnswerVO }
  | { status: 'error'; error: AiStreamError }
  | { status: 'aborted' }
  | { status: 'fallback'; code: number; message: string }

/** App 端 renderjs 桥的请求参数 */
export interface SseBridgeRequest {
  id: string
  action: 'start' | 'abort'
  url?: string
  token?: string
  body?: unknown
  timeout?: number
}

/** App 端 renderjs 桥回传的事件 */
export interface SseBridgeEvent {
  id: string
  event:
    | 'started'
    | 'meta'
    | 'delta'
    | 'done'
    | 'error'
    | 'http-error'
    | 'network-error'
    | 'timeout'
    | 'aborted'
    | 'ended'
  /** meta/delta/done/error 事件的原始 JSON 字符串 */
  data?: string
  code?: number
  message?: string
  httpStatus?: number
  partial?: boolean
}
