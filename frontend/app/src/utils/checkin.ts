import dayjs from 'dayjs'
import type { HealthCheckin } from '@/types/health'

export const CHECKIN_BACKFILL_DAYS = 7
export const CHECKIN_MAX_PHOTOS = 6
export const CHECKIN_SLEEP_TIMER_SOURCE = 1
export const CHECKIN_EXERCISE_TIMER_SOURCE = 1

export type OptionTone = 'primary' | 'success' | 'warning' | 'danger' | 'info'

export interface CheckinOption<T = number> {
  value: T
  label: string
  tone?: OptionTone
}

export const CHECKIN_EMOTION_OPTIONS: Array<CheckinOption & { emoji: string }> = [
  { value: 1, label: '愉悦', emoji: '😄', tone: 'success' },
  { value: 2, label: '平静', emoji: '🙂', tone: 'info' },
  { value: 3, label: '焦虑', emoji: '😟', tone: 'warning' },
  { value: 4, label: '低落', emoji: '😔', tone: 'info' },
  { value: 5, label: '愤怒', emoji: '😠', tone: 'danger' },
]

export const CHECKIN_DIET_BALANCE_OPTIONS: CheckinOption[] = [
  { value: 1, label: '不均衡', tone: 'danger' },
  { value: 2, label: '一般', tone: 'warning' },
  { value: 3, label: '均衡', tone: 'success' },
]

export const CHECKIN_EXERCISE_INTENSITY_OPTIONS: CheckinOption[] = [
  { value: 1, label: '低强度', tone: 'info' },
  { value: 2, label: '中强度', tone: 'warning' },
  { value: 3, label: '高强度', tone: 'danger' },
]

export const CHECKIN_EXERCISE_TYPES = [
  '跑步',
  '快走',
  '骑行',
  '游泳',
  '球类运动',
  '健身训练',
  '瑜伽',
  '跳绳',
  '其他',
]

export const CHECKIN_PHOTO_TYPE_OPTIONS: CheckinOption[] = [
  { value: 1, label: '睡眠' },
  { value: 2, label: '运动' },
  { value: 3, label: '饮食' },
  { value: 4, label: '其他' },
]

export const CHECKIN_YES_NO_OPTIONS: CheckinOption[] = [
  { value: 1, label: '是', tone: 'success' },
  { value: 0, label: '否', tone: 'warning' },
]

export function emotionEmoji(value?: number | null): string {
  if (value == null) return '—'
  return CHECKIN_EMOTION_OPTIONS.find((item) => item.value === value)?.emoji ?? '—'
}

export function emotionLabel(value?: number | null): string {
  if (value == null) return '未填写'
  return CHECKIN_EMOTION_OPTIONS.find((item) => item.value === value)?.label ?? '未知'
}

export function dietBalanceLabel(value?: number | null): string {
  if (value == null) return '未填写'
  return CHECKIN_DIET_BALANCE_OPTIONS.find((item) => item.value === value)?.label ?? '未知'
}

export function exerciseIntensityLabel(value?: number | null): string {
  if (value == null) return '未填写'
  return CHECKIN_EXERCISE_INTENSITY_OPTIONS.find((item) => item.value === value)?.label ?? '未知'
}

export function photoTypeLabel(value?: number | null): string {
  if (value == null) return '未分类'
  return CHECKIN_PHOTO_TYPE_OPTIONS.find((item) => item.value === value)?.label ?? '未知'
}

/** 将 HH:mm:ss 或 yyyy-MM-dd HH:mm:ss 转成 HH:mm */
export function formatClockTime(value?: string | null): string {
  if (!value) return '—'
  const text = value.replace('T', ' ')
  const time = text.includes(' ') ? text.split(' ')[1]! : text
  return time.slice(0, 5)
}

export function parseTimeToMinutes(value?: string | null): number | null {
  if (!value) return null
  const match = value.match(/(\d{1,2}):(\d{1,2})/)
  if (!match) return null
  const hour = Number(match[1])
  const minute = Number(match[2])
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null
  return hour * 60 + minute
}

/** 与后端一致：跨午夜自动加 24 小时 */
export function calculateSleepHours(
  sleepTime?: string | null,
  wakeTime?: string | null,
): number | null {
  const sleep = parseTimeToMinutes(sleepTime)
  const wake = parseTimeToMinutes(wakeTime)
  if (sleep == null || wake == null) return null
  let minutes = wake - sleep
  if (minutes <= 0) minutes += 24 * 60
  return Math.round((minutes / 60) * 10) / 10
}

export function checkinCompletionCount(record?: HealthCheckin | null): number {
  if (!record) return 0
  return [
    record.sleepCompleted,
    record.exerciseCompleted,
    record.dietCompleted,
    record.emotionCompleted,
  ].filter(Boolean).length
}

export function isSleepTimerSource(record?: HealthCheckin | null): boolean {
  return record?.sleepSource === CHECKIN_SLEEP_TIMER_SOURCE
}

export function isExerciseTimerSource(record?: HealthCheckin | null): boolean {
  return record?.exerciseSource === CHECKIN_EXERCISE_TIMER_SOURCE
}

export function todayDateString(): string {
  return dayjs().format('YYYY-MM-DD')
}

export function currentMonthString(): string {
  return dayjs().format('YYYY-MM')
}

export function formatMonthLabel(month: string): string {
  const date = dayjs(`${month}-01`)
  return date.isValid() ? date.format('YYYY 年 M 月') : month
}

export function monthBounds(month: string): { startDate: string; endDate: string } {
  const date = dayjs(`${month}-01`)
  if (!date.isValid()) {
    return { startDate: todayDateString(), endDate: todayDateString() }
  }
  return {
    startDate: date.startOf('month').format('YYYY-MM-DD'),
    endDate: date.endOf('month').format('YYYY-MM-DD'),
  }
}

export function earliestBackfillDate(days = CHECKIN_BACKFILL_DAYS): string {
  return dayjs()
    .subtract(Math.max(1, days) - 1, 'day')
    .format('YYYY-MM-DD')
}

export function canEditCheckinDate(date?: string | null, days = CHECKIN_BACKFILL_DAYS): boolean {
  if (!date) return false
  const target = date.slice(0, 10)
  return target >= earliestBackfillDate(days) && target <= todayDateString()
}

export function recentDateOptions(days = CHECKIN_BACKFILL_DAYS): Array<{
  value: string
  label: string
}> {
  const result: Array<{ value: string; label: string }> = []
  for (let index = 0; index < days; index += 1) {
    const date = dayjs().subtract(index, 'day')
    const value = date.format('YYYY-MM-DD')
    const label = index === 0 ? '今天' : index === 1 ? '昨天' : date.format('M 月 D 日')
    result.push({ value, label })
  }
  return result
}

export function formatElapsedHuman(seconds: number): string {
  const total = Math.max(0, Math.floor(seconds))
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  if (hours > 0) {
    return minutes > 0 ? `${hours} 小时 ${minutes} 分钟` : `${hours} 小时`
  }
  if (minutes > 0) return `${minutes} 分钟`
  return `${total} 秒`
}

/** 计时开始时间按本地时间解析；服务端时间为 Asia/Shanghai */
export function elapsedSecondsSince(startTime?: string | null): number {
  if (!startTime) return 0
  const start = dayjs(startTime.replace(' ', 'T')).valueOf()
  if (Number.isNaN(start)) return 0
  return Math.max(0, Math.floor((Date.now() - start) / 1000))
}
