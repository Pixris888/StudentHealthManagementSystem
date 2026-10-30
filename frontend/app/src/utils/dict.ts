export const USER_TYPE_OPTIONS = [
  { value: 1, label: '学生' },
  { value: 2, label: '校医科普管理员' },
  { value: 3, label: '超级系统管理员' },
]

export const GENDER_OPTIONS = [
  { value: 1, label: '男' },
  { value: 2, label: '女' },
  { value: 0, label: '未知' },
]

export const BLOOD_TYPE_OPTIONS = ['A型', 'B型', 'AB型', 'O型', '未知']

export const TERM_OPTIONS = ['上学期', '下学期']

export const GRADE_LEVEL_OPTIONS = ['优秀', '良好', '及格', '不及格']

export const ROLE_CODE_LABELS: Record<string, string> = {
  SUPER_ADMIN: '超级系统管理员',
  DOCTOR_ADMIN: '校医科普管理员',
  STUDENT: '学生',
}

export function roleCodeLabel(roleCode: string): string {
  return ROLE_CODE_LABELS[roleCode] ?? roleCode
}

export function userTypeLabel(value?: number | null): string {
  return USER_TYPE_OPTIONS.find((item) => item.value === value)?.label ?? '未知'
}

export function genderLabel(value?: number | null): string {
  if (value == null) return '未填写'
  return GENDER_OPTIONS.find((item) => item.value === value)?.label ?? '未知'
}

export function bmiLevelTone(level?: string | null): 'success' | 'warning' | 'danger' | 'info' {
  if (!level) return 'info'
  if (level.includes('正常')) return 'success'
  if (level.includes('偏瘦') || level.includes('超重')) return 'warning'
  if (level.includes('肥胖')) return 'danger'
  return 'info'
}
