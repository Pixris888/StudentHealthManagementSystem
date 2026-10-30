export type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

export interface DictOption<T extends number = number> {
  value: T
  label: string
  tagType?: TagType
}

export const USER_TYPE_OPTIONS: DictOption[] = [
  { value: 1, label: '学生', tagType: 'success' },
  { value: 2, label: '校医科普管理员', tagType: 'warning' },
  { value: 3, label: '超级系统管理员', tagType: 'danger' },
]

export const DATA_SCOPE_OPTIONS: DictOption[] = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '自定义数据' },
  { value: 3, label: '仅本人数据' },
  { value: 4, label: '本部门数据' },
]

export const STATUS_OPTIONS: DictOption[] = [
  { value: 1, label: '正常', tagType: 'success' },
  { value: 0, label: '停用', tagType: 'info' },
]

export const MENU_TYPE_OPTIONS: DictOption[] = [
  { value: 1, label: '目录', tagType: 'info' },
  { value: 2, label: '菜单', tagType: 'success' },
  { value: 3, label: '按钮', tagType: 'warning' },
]

const USER_TYPE_MAP = new Map(USER_TYPE_OPTIONS.map((item) => [item.value, item]))
const DATA_SCOPE_MAP = new Map(DATA_SCOPE_OPTIONS.map((item) => [item.value, item]))
const STATUS_MAP = new Map(STATUS_OPTIONS.map((item) => [item.value, item]))
const MENU_TYPE_MAP = new Map(MENU_TYPE_OPTIONS.map((item) => [item.value, item]))

export const ROLE_CODE_LABELS: Record<string, string> = {
  SUPER_ADMIN: '超级系统管理员',
  DOCTOR_ADMIN: '校医科普管理员',
  STUDENT: '学生',
}

export const BUILT_IN_ROLES = ['SUPER_ADMIN', 'DOCTOR_ADMIN', 'STUDENT']

export function isBuiltInRole(roleCode?: string | null): boolean {
  return Boolean(roleCode && BUILT_IN_ROLES.includes(roleCode))
}

export function userTypeLabel(value?: number | null): string {
  return value == null ? '未知' : (USER_TYPE_MAP.get(value)?.label ?? '未知')
}

export function userTypeTagType(value?: number | null): TagType {
  return value == null ? 'info' : (USER_TYPE_MAP.get(value)?.tagType ?? 'info')
}

export function dataScopeLabel(value?: number | null): string {
  return value == null ? '—' : (DATA_SCOPE_MAP.get(value)?.label ?? '未知')
}

export function statusLabel(value?: number | null): string {
  return value == null ? '未知' : (STATUS_MAP.get(value)?.label ?? '未知')
}

export function statusTagType(value?: number | null): TagType {
  return value == null ? 'info' : (STATUS_MAP.get(value)?.tagType ?? 'info')
}

export function menuTypeLabel(value?: number | null): string {
  return value == null ? '未知' : (MENU_TYPE_MAP.get(value)?.label ?? '未知')
}

export function menuTypeTagType(value?: number | null): TagType {
  return value == null ? 'info' : (MENU_TYPE_MAP.get(value)?.tagType ?? 'info')
}

export function roleCodeLabel(roleCode: string): string {
  return ROLE_CODE_LABELS[roleCode] ?? roleCode
}
