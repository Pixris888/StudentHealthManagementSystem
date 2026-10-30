import type { TagType } from './dict'

/** 后端比率字段都是 0-100 的百分数，这里只做格式化，不再乘 100 */
export function formatPercent(
  value?: number | null,
  digits = 2,
): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '—'
  }
  const text = Number(value).toFixed(digits)
  return `${text.replace(/\.?0+$/, '')}%`
}

/** 平均值等保留固定小数位 */
export function formatDecimal(
  value?: number | null,
  digits = 1,
): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '—'
  }
  return Number(value).toFixed(digits)
}

/** 整数统计值，空值显示 -- */
export function formatCount(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '—'
  }
  return String(Math.round(Number(value)))
}

/** 姓名/学号等文本空值兜底 */
export function formatText(value?: string | null): string {
  return value && value.trim() ? value : '—'
}

export function riskTagType(level?: string | null): TagType {
  if (level === '较高') return 'danger'
  if (level === '需关注') return 'warning'
  return 'info'
}

export function bmiTagType(level?: string | null): TagType {
  if (level === '正常') return 'success'
  if (level === '偏瘦') return 'info'
  if (level === '超重') return 'warning'
  if (level === '肥胖') return 'danger'
  return 'info'
}

export const EMOTION_COLORS: Record<number, string> = {
  1: '#1f9d7a',
  2: '#3f7ff0',
  3: '#d99a2b',
  4: '#8a63e8',
  5: '#e05b5b',
}

/** 当前月份 yyyy-MM */
export function currentMonthValue(): string {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${now.getFullYear()}-${month}`
}

/** 禁止选择未来月份 */
export function isFutureMonth(date: Date): boolean {
  const now = new Date()
  return (
    date.getFullYear() * 12 + date.getMonth() >
    now.getFullYear() * 12 + now.getMonth()
  )
}

/** 月份展示为 2026年10月 */
export function formatMonthLabel(month?: string | null): string {
  if (!month) return '—'
  const [year, monthValue] = month.split('-')
  if (!year || !monthValue) return month
  return `${year}年${Number(monthValue)}月`
}

/** 日期展示为 MM-dd */
export function formatShortDate(date?: string | null): string {
  if (!date) return ''
  return date.length >= 10 ? date.slice(5, 10) : date
}
