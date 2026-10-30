import dayjs from 'dayjs'

export function todayString(): string {
  return dayjs().format('YYYY-MM-DD')
}

export function currentMonthString(): string {
  return dayjs().format('YYYY-MM')
}

export function formatDate(value?: string | null, pattern = 'YYYY-MM-DD'): string {
  if (!value) return '—'
  const date = dayjs(value)
  return date.isValid() ? date.format(pattern) : String(value)
}

export function formatDateTime(value?: string | null): string {
  return formatDate(value, 'YYYY-MM-DD HH:mm')
}

export function formatDateTimeShort(value?: string | null): string {
  return formatDate(value, 'MM-DD HH:mm')
}

export function formatTime(value?: string | null): string {
  return formatDate(value, 'HH:mm')
}

export function formatMonth(month?: string | null): string {
  if (!month) return '—'
  const date = dayjs(`${month}-01`)
  return date.isValid() ? date.format('YYYY 年 M 月') : String(month)
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

export function formatSleepHours(hours?: number | null): string {
  if (hours == null) return '—'
  return `${Number(hours).toFixed(1)} 小时`
}

export function formatWaterIntake(value?: number | null): string {
  if (value == null) return '—'
  return `${value} ml`
}

export function formatMoodScore(value?: number | null): string {
  if (value == null) return '—'
  return `${value} / 5`
}

export function formatNumber(value?: number | null, digits = 1): string {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return Number(value).toFixed(digits).replace(/\.0$/, '')
}

export function formatPercent(value?: number | null): string {
  if (value == null) return '—'
  return `${Number(value).toFixed(1).replace(/\.0$/, '')}%`
}

export function greeting(): string {
  const hour = dayjs().hour()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}

export function todayLabel(): string {
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const date = dayjs()
  return `${date.format('M 月 D 日')} ${weekdays[date.day()]}`
}

export function formatDurationSeconds(seconds: number): string {
  const total = Math.max(0, Math.floor(seconds))
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const secs = total % 60
  return [hours, minutes, secs]
    .map((item) => String(item).padStart(2, '0'))
    .join(':')
}
