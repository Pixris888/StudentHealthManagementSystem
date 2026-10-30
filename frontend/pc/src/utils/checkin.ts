import type { TagType } from './dict'
import type { HealthCheckin } from '@/types/health'

export const CHECKIN_BACKFILL_DAYS = 7
export const CHECKIN_MAX_PHOTOS = 6
export const CHECKIN_SESSION_MAX_HOURS = 24
export const CHECKIN_SLEEP_TIMER_SOURCE = 1
export const CHECKIN_EXERCISE_TIMER_SOURCE = 1

export interface CheckinOption<T = number> {
  value: T
  label: string
  tagType?: TagType
}

export const CHECKIN_EMOTION_OPTIONS: Array<
  CheckinOption & { emoji: string }
> = [
  { value: 1, label: '愉悦', emoji: '😄', tagType: 'success' },
  { value: 2, label: '平静', emoji: '🙂', tagType: 'info' },
  { value: 3, label: '焦虑', emoji: '😟', tagType: 'warning' },
  { value: 4, label: '低落', emoji: '😔', tagType: 'info' },
  { value: 5, label: '愤怒', emoji: '😠', tagType: 'danger' },
]

export const CHECKIN_DIET_BALANCE_OPTIONS: CheckinOption[] = [
  { value: 1, label: '不均衡', tagType: 'danger' },
  { value: 2, label: '一般', tagType: 'warning' },
  { value: 3, label: '均衡', tagType: 'success' },
]

export const CHECKIN_EXERCISE_INTENSITY_OPTIONS: CheckinOption[] = [
  { value: 1, label: '低强度', tagType: 'info' },
  { value: 2, label: '中强度', tagType: 'warning' },
  { value: 3, label: '高强度', tagType: 'danger' },
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

export const CHECKIN_DATA_QUALITY_OPTIONS: CheckinOption[] = [
  { value: 0, label: '正常', tagType: 'success' },
  { value: 1, label: '异常待复核', tagType: 'danger' },
]

export const CHECKIN_YES_NO_OPTIONS: CheckinOption[] = [
  { value: 1, label: '是', tagType: 'success' },
  { value: 0, label: '否', tagType: 'warning' },
]

export const CHECKIN_SESSION_TYPE_OPTIONS: CheckinOption[] = [
  { value: 1, label: '睡眠', tagType: 'primary' },
  { value: 2, label: '运动', tagType: 'success' },
]

export const CHECKIN_SESSION_STATUS_OPTIONS: CheckinOption[] = [
  { value: 0, label: '进行中', tagType: 'primary' },
  { value: 1, label: '已结束', tagType: 'success' },
  { value: 2, label: '已过期', tagType: 'danger' },
]

const EMOTION_MAP = new Map(
  CHECKIN_EMOTION_OPTIONS.map((item) => [item.value, item]),
)
const DIET_BALANCE_MAP = new Map(
  CHECKIN_DIET_BALANCE_OPTIONS.map((item) => [item.value, item]),
)
const EXERCISE_INTENSITY_MAP = new Map(
  CHECKIN_EXERCISE_INTENSITY_OPTIONS.map((item) => [item.value, item]),
)
const PHOTO_TYPE_MAP = new Map(
  CHECKIN_PHOTO_TYPE_OPTIONS.map((item) => [item.value, item]),
)
const SESSION_TYPE_MAP = new Map(
  CHECKIN_SESSION_TYPE_OPTIONS.map((item) => [item.value, item]),
)
const SESSION_STATUS_MAP = new Map(
  CHECKIN_SESSION_STATUS_OPTIONS.map((item) => [item.value, item]),
)

export function emotionLabel(value?: number | null): string {
  if (value == null) return '未填写'
  return EMOTION_MAP.get(value)?.label ?? '未知'
}

export function emotionEmoji(value?: number | null): string {
  if (value == null) return '—'
  return EMOTION_MAP.get(value)?.emoji ?? '—'
}

export function dietBalanceLabel(value?: number | null): string {
  if (value == null) return '未填写'
  return DIET_BALANCE_MAP.get(value)?.label ?? '未知'
}

export function exerciseIntensityLabel(value?: number | null): string {
  if (value == null) return '未填写'
  return EXERCISE_INTENSITY_MAP.get(value)?.label ?? '未知'
}

export function photoTypeLabel(value?: number | null): string {
  if (value == null) return '未分类'
  return PHOTO_TYPE_MAP.get(value)?.label ?? '未知'
}

export function sessionTypeLabel(value?: number | null): string {
  if (value == null) return '未知'
  return SESSION_TYPE_MAP.get(value)?.label ?? '未知'
}

export function sessionStatusLabel(value?: number | null): string {
  if (value == null) return '未知'
  return SESSION_STATUS_MAP.get(value)?.label ?? '未知'
}

export function dataQualityTagType(value?: number | null): TagType {
  return value === 1 ? 'danger' : 'success'
}

export function yesNoTagType(value?: number | null): TagType {
  if (value === 1) return 'success'
  if (value === 0) return 'warning'
  return 'info'
}

export function sessionStatusTagType(value?: number | null): TagType {
  if (value === 0) return 'primary'
  if (value === 1) return 'success'
  if (value === 2) return 'danger'
  return 'info'
}

export function sessionTypeTagType(value?: number | null): TagType {
  if (value === 1) return 'primary'
  if (value === 2) return 'success'
  return 'info'
}

/** 将 HH:mm:ss 或 yyyy-MM-dd HH:mm:ss 转成 HH:mm */
export function formatClockTime(value?: string | null): string {
  if (!value) return '—'
  const text = value.replace('T', ' ')
  const time = text.includes(' ') ? text.split(' ')[1]! : text
  return time.slice(0, 5)
}

/** 将 yyyy-MM-dd HH:mm:ss 转成 MM-dd HH:mm */
export function formatDateTimeShort(value?: string | null): string {
  if (!value) return '—'
  return value.replace('T', ' ').slice(5, 16)
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

export function formatSleepHours(hours?: number | null): string {
  if (hours == null) return '—'
  return `${Number(hours).toFixed(1)} 小时`
}

export function formatDurationMinutes(minutes?: number | null): string {
  if (minutes == null) return '—'
  const total = Math.max(0, Math.round(minutes))
  const hours = Math.floor(total / 60)
  const rest = total % 60
  if (hours > 0) {
    return rest > 0 ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`
  }
  return `${rest} 分钟`
}

export function formatElapsedClock(seconds: number): string {
  const total = Math.max(0, Math.floor(seconds))
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const secs = total % 60
  return [hours, minutes, secs]
    .map((item) => String(item).padStart(2, '0'))
    .join(':')
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
  const start = new Date(startTime.replace(' ', 'T')).getTime()
  if (Number.isNaN(start)) return 0
  return Math.max(0, Math.floor((Date.now() - start) / 1000))
}

export function checkinCompletionCount(
  record?: HealthCheckin | null,
): number {
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
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export function currentMonthString(): string {
  return todayDateString().slice(0, 7)
}

export function monthBounds(month: string): {
  startDate: string
  endDate: string
} {
  const [yearText, monthText] = month.split('-')
  const year = Number(yearText)
  const monthIndex = Number(monthText)
  if (!year || !monthIndex) {
    const today = new Date()
    return {
      startDate: `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-01`,
      endDate: todayDateString(),
    }
  }
  const lastDay = new Date(year, monthIndex, 0).getDate()
  return {
    startDate: `${yearText}-${monthText}-01`,
    endDate: `${yearText}-${monthText}-${String(lastDay).padStart(2, '0')}`,
  }
}

export function formatMonthLabel(month: string): string {
  const [year, monthText] = month.split('-')
  return `${year} 年 ${Number(monthText)} 月`
}

export function earliestBackfillDate(
  days = CHECKIN_BACKFILL_DAYS,
): string {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() - Math.max(1, days) + 1)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export function canEditCheckinDate(
  date?: string | null,
  days = CHECKIN_BACKFILL_DAYS,
): boolean {
  if (!date) return false
  const target = date.slice(0, 10)
  return target >= earliestBackfillDate(days) && target <= todayDateString()
}

export function disabledBackfillDate(
  date: Date,
  days = CHECKIN_BACKFILL_DAYS,
): boolean {
  const value = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-')
  return value > todayDateString() || value < earliestBackfillDate(days)
}

export function formatWaterIntake(value?: number | null): string {
  if (value == null) return '—'
  return `${value} ml`
}

export function formatMoodScore(value?: number | null): string {
  if (value == null) return '—'
  return `${value} / 5`
}
