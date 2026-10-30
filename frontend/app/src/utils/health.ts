export function calcBmi(
  height?: number | null,
  weight?: number | null,
): number | null {
  if (height == null || weight == null || height <= 0 || weight <= 0) return null
  const heightM = height / 100
  const value = weight / (heightM * heightM)
  return Number.isFinite(value) ? Math.round(value * 100) / 100 : null
}

export function bmiLevelOf(bmi?: number | null): string | null {
  if (bmi == null) return null
  if (bmi < 18.5) return '偏瘦'
  if (bmi < 24) return '正常'
  if (bmi < 28) return '超重'
  return '肥胖'
}

export function isFutureDate(value?: string | null): boolean {
  if (!value) return false
  const date = new Date(`${value.slice(0, 10)}T23:59:59`)
  return date.getTime() > Date.now()
}

export type PhysicalMetricKey =
  | 'bmi'
  | 'totalScore'
  | 'vitalCapacity'
  | 'run50m'
  | 'sitAndReach'
  | 'standingJump'
  | 'enduranceRun'
  | 'strengthItem'

export interface PhysicalMetricOption {
  key: PhysicalMetricKey
  label: string
  unit: string
  digits: number
}

export const PHYSICAL_METRIC_OPTIONS: PhysicalMetricOption[] = [
  { key: 'bmi', label: 'BMI', unit: '', digits: 1 },
  { key: 'totalScore', label: '总分', unit: '分', digits: 1 },
  { key: 'vitalCapacity', label: '肺活量', unit: 'ml', digits: 0 },
  { key: 'run50m', label: '50 米跑', unit: '秒', digits: 1 },
  { key: 'sitAndReach', label: '坐位体前屈', unit: 'cm', digits: 1 },
  { key: 'standingJump', label: '立定跳远', unit: 'cm', digits: 1 },
  { key: 'enduranceRun', label: '耐力跑', unit: '秒', digits: 1 },
  { key: 'strengthItem', label: '力量项目', unit: '', digits: 1 },
]
