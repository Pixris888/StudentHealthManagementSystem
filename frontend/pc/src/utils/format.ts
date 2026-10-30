/** 通用展示格式化工具 */

/** 后端 LocalDateTime 形如 2026-10-24T17:09:44.2420895，统一展示为 YYYY-MM-DD HH:mm */
export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  const text = value.replace('T', ' ')
  return text.length >= 16 ? text.slice(0, 16) : text
}

/** 仅保留日期部分 YYYY-MM-DD */
export function formatDate(value?: string | null): string {
  if (!value) return '—'
  return value.slice(0, 10)
}

/** 当前本地时间，格式与后端 LocalDateTime 一致（YYYY-MM-DDTHH:mm:ss） */
export function nowLocalDateTimeString(): string {
  const date = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

/** 空值兜底 */
export function displayValue(value?: string | number | null): string {
  if (value === null || value === undefined || value === '') return '—'
  return String(value)
}
