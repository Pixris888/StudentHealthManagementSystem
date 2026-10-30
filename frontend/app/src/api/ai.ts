import { del, get, post } from '@/utils/request'
import type { PageResult } from '@/types/system'
import type {
  AiAnswerVO,
  AiAskDTO,
  AiConsultationQuery,
  AiConsultationVO,
  AiFeedbackDTO,
  AiHealthPlanVO,
  AiPlanGenerateDTO,
  AiPlanQuery,
} from '@/types/ai'

/** AI 问答与计划生成是同步接口，后端最坏情况约 165 秒 */
const AI_TIMEOUT = 180000

/** 学生提问（支持多轮对话） */
export function askAi(data: AiAskDTO): Promise<AiAnswerVO> {
  return post<AiAnswerVO>('/ai/consultation/ask', data, { timeout: AI_TIMEOUT })
}

/** 我的问答历史分页 */
export function pageMyConsultations(
  params: AiConsultationQuery,
): Promise<PageResult<AiConsultationVO>> {
  return get<PageResult<AiConsultationVO>>('/ai/consultation/mine/page', {
    ...params,
  })
}

/** 某个会话的全部消息（按时间升序） */
export function getSessionMessages(sessionId: string): Promise<AiConsultationVO[]> {
  return get<AiConsultationVO[]>(
    `/ai/consultation/session/${encodeURIComponent(sessionId)}`,
  )
}

/** 我的问答详情 */
export function getMyConsultation(id: number): Promise<AiConsultationVO> {
  return get<AiConsultationVO>(`/ai/consultation/${id}`)
}

/** 删除我的问答记录 */
export function deleteMyConsultations(ids: number[]): Promise<boolean> {
  return del<boolean>(`/ai/consultation/${ids.join(',')}`)
}

/** 回答反馈 */
export function feedbackConsultation(
  id: number,
  data: AiFeedbackDTO,
): Promise<AiConsultationVO> {
  return post<AiConsultationVO>(`/ai/consultation/${id}/feedback`, data)
}

/** 生成健康计划 */
export function generatePlan(data: AiPlanGenerateDTO): Promise<AiHealthPlanVO> {
  return post<AiHealthPlanVO>('/ai/health_plan/generate', data, {
    timeout: AI_TIMEOUT,
  })
}

/** 我的计划历史分页 */
export function pageMyPlans(params: AiPlanQuery): Promise<PageResult<AiHealthPlanVO>> {
  return get<PageResult<AiHealthPlanVO>>('/ai/health_plan/mine/page', {
    ...params,
  })
}

/** 计划详情 */
export function getMyPlan(id: number): Promise<AiHealthPlanVO> {
  return get<AiHealthPlanVO>(`/ai/health_plan/${id}`)
}

/** 采纳计划 */
export function adoptPlan(id: number): Promise<AiHealthPlanVO> {
  return post<AiHealthPlanVO>(`/ai/health_plan/${id}/adopt`)
}

/** 删除计划 */
export function deleteMyPlans(ids: number[]): Promise<boolean> {
  return del<boolean>(`/ai/health_plan/${ids.join(',')}`)
}
