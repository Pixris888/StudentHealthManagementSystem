import type { PhysicalTest } from '@/types/health'
import type { TagType } from './dict'

export const GENDER_OPTIONS = [
  { value: 0, label: '未知' },
  { value: 1, label: '男' },
  { value: 2, label: '女' },
]

export const BLOOD_TYPE_OPTIONS = ['A型', 'B型', 'AB型', 'O型', '未知']

export const TEST_TERM_OPTIONS = [
  { value: '上学期', label: '上学期' },
  { value: '下学期', label: '下学期' },
]

export const GRADE_LEVEL_OPTIONS = ['优秀', '良好', '及格', '不及格']

const GENDER_MAP = new Map(GENDER_OPTIONS.map((item) => [item.value, item.label]))

export function genderLabel(value?: number | null): string {
  if (value == null) return '未知'
  return GENDER_MAP.get(value) ?? '未知'
}

export function genderTagType(value?: number | null): TagType {
  if (value === 1) return 'primary'
  if (value === 2) return 'danger'
  return 'info'
}

export function bmiLevelTagType(level?: string | null): TagType {
  switch (level) {
    case '正常':
      return 'success'
    case '偏瘦':
      return 'info'
    case '超重':
      return 'warning'
    case '肥胖':
      return 'danger'
    default:
      return 'info'
  }
}

export function gradeLevelTagType(level?: string | null): TagType {
  switch (level) {
    case '优秀':
      return 'success'
    case '良好':
      return 'primary'
    case '及格':
      return 'warning'
    case '不及格':
      return 'danger'
    default:
      return 'info'
  }
}

/** 保留最多 digits 位小数，并去掉末尾多余的 0 */
export function formatNumber(
  value?: number | string | null,
  digits = 2,
): string {
  if (value == null || value === '') return '—'
  const num = Number(value)
  if (Number.isNaN(num)) return '—'
  const text = num.toFixed(digits)
  return text.replace(/\.?0+$/, '')
}

export function formatValue(
  value?: number | string | null,
  unit = '',
): string {
  if (value == null || value === '') return '—'
  return unit ? `${value} ${unit}` : String(value)
}

export function formatDate(value?: string | null): string {
  if (!value) return '—'
  return value.slice(0, 10)
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 19)
}

/** 与后端一致的 BMI 计算：体重(kg) / 身高(m)^2 */
export function calcBmi(
  height?: number | null,
  weight?: number | null,
): number | null {
  if (height == null || weight == null || height <= 0 || weight <= 0) {
    return null
  }
  const heightM = height / 100
  return Math.round((weight / (heightM * heightM)) * 100) / 100
}

export function bmiLevelOf(bmi?: number | null): string | null {
  if (bmi == null) return null
  if (bmi < 18.5) return '偏瘦'
  if (bmi < 24) return '正常'
  if (bmi < 28) return '超重'
  return '肥胖'
}

export function isFutureDate(date: Date): boolean {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date.getTime() > today.getTime()
}

export type PhysicalMetricKey =
  | 'height'
  | 'weight'
  | 'bmi'
  | 'vitalCapacity'
  | 'run50m'
  | 'sitAndReach'
  | 'standingJump'
  | 'enduranceRun'
  | 'strengthItem'
  | 'totalScore'

export interface PhysicalMetricOption {
  value: PhysicalMetricKey
  label: string
  unit: string
  color: string
  lowerIsBetter?: boolean
}

export const PHYSICAL_METRICS: PhysicalMetricOption[] = [
  { value: 'bmi', label: 'BMI', unit: '', color: '#1f9d7a' },
  { value: 'totalScore', label: '体测总分', unit: '分', color: '#3f7ff0' },
  { value: 'vitalCapacity', label: '肺活量', unit: 'ml', color: '#8a63e8' },
  {
    value: 'run50m',
    label: '50 米跑',
    unit: '秒',
    color: '#d9932f',
    lowerIsBetter: true,
  },
  { value: 'sitAndReach', label: '坐位体前屈', unit: 'cm', color: '#16a085' },
  { value: 'standingJump', label: '立定跳远', unit: 'cm', color: '#d966a8' },
  {
    value: 'enduranceRun',
    label: '耐力跑',
    unit: '秒',
    color: '#e05b5b',
    lowerIsBetter: true,
  },
  { value: 'strengthItem', label: '力量项目', unit: '个', color: '#2f9d9d' },
  { value: 'height', label: '身高', unit: 'cm', color: '#5a8f7b' },
  { value: 'weight', label: '体重', unit: 'kg', color: '#c58a28' },
]

const METRIC_MAP = new Map(
  PHYSICAL_METRICS.map((item) => [item.value, item]),
)

export function getPhysicalMetric(key: PhysicalMetricKey): PhysicalMetricOption {
  return METRIC_MAP.get(key) ?? PHYSICAL_METRICS[0]!
}

export function physicalMetricValue(
  record: PhysicalTest,
  key: PhysicalMetricKey,
): number | null {
  const value = record[key]
  if (value == null) return null
  const num = Number(value)
  return Number.isNaN(num) ? null : num
}

/** 图表横轴标签，如 2025 上学期 */
export function physicalTestLabel(record: PhysicalTest): string {
  return `${record.testYear} ${record.testTerm ?? ''}`.trim()
}
