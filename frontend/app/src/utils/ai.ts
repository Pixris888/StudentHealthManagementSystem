import type { PlanType } from '@/types/ai'

export const AI_MAX_QUESTION_LENGTH = 500

export const AI_QUICK_QUESTIONS = [
  '最近总是熬夜，怎么调整作息？',
  '一周运动怎么安排比较合理？',
  '食堂怎么吃更健康？',
  '最近心情低落，有什么自我调节方法？',
]

export const PLAN_TYPE_OPTIONS: Array<{
  value: PlanType
  label: string
  desc: string
  icon: string
}> = [
  { value: 1, label: '作息计划', desc: '根据最近打卡调整睡眠与作息', icon: 'moon' },
  { value: 2, label: '运动计划', desc: '结合运动习惯制定一周安排', icon: 'fire' },
  { value: 3, label: '综合计划', desc: '作息、运动、饮食综合建议', icon: 'star' },
]

export function planTypeLabel(value?: PlanType | null): string {
  return PLAN_TYPE_OPTIONS.find((item) => item.value === value)?.label ?? '健康计划'
}

export function feedbackLabel(value?: number | null): string {
  if (value === 1) return '有帮助'
  if (value === 2) return '待改进'
  return ''
}

export function aiErrorText(error: unknown, fallback = '回答失败，请稍后重试'): string {
  const message = (error as Error | null)?.message
  if (message) return message
  return fallback
}
