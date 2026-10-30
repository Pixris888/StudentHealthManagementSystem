import type { TagType } from './dict'
import type {
  BackupStatus,
  ConfigType,
  OperLogStatus,
} from '@/types/system'

export interface SystemOption<T extends number = number> {
  value: T
  label: string
  tagType: TagType
}

export const CONFIG_TYPE_OPTIONS: SystemOption<ConfigType>[] = [
  { value: 0, label: '系统内置', tagType: 'warning' },
  { value: 1, label: '自定义', tagType: 'primary' },
]

export const BACKUP_STATUS_OPTIONS: SystemOption<BackupStatus>[] = [
  { value: 0, label: '失败', tagType: 'danger' },
  { value: 1, label: '成功', tagType: 'success' },
]

/** 操作日志状态：0-成功，1-失败（与其它模块相反） */
export const OPER_LOG_STATUS_OPTIONS: SystemOption<OperLogStatus>[] = [
  { value: 0, label: '成功', tagType: 'success' },
  { value: 1, label: '失败', tagType: 'danger' },
]

export const BUSINESS_TYPE_OPTIONS: SystemOption[] = [
  { value: 0, label: '其他', tagType: 'info' },
  { value: 1, label: '新增', tagType: 'success' },
  { value: 2, label: '修改', tagType: 'primary' },
  { value: 3, label: '删除', tagType: 'danger' },
  { value: 4, label: '授权', tagType: 'warning' },
  { value: 5, label: '导出', tagType: 'info' },
  { value: 6, label: '导入', tagType: 'info' },
  { value: 7, label: '强退', tagType: 'warning' },
  { value: 8, label: '重置密码', tagType: 'warning' },
]

export const OPERATOR_TYPE_OPTIONS: SystemOption[] = [
  { value: 0, label: '其他', tagType: 'info' },
  { value: 1, label: '后台用户', tagType: 'primary' },
  { value: 2, label: '手机端', tagType: 'success' },
]

const CONFIG_TYPE_MAP = new Map(
  CONFIG_TYPE_OPTIONS.map((item) => [item.value, item]),
)
const BACKUP_STATUS_MAP = new Map(
  BACKUP_STATUS_OPTIONS.map((item) => [item.value, item]),
)
const OPER_LOG_STATUS_MAP = new Map(
  OPER_LOG_STATUS_OPTIONS.map((item) => [item.value, item]),
)
const BUSINESS_TYPE_MAP = new Map(
  BUSINESS_TYPE_OPTIONS.map((item) => [item.value, item]),
)
const OPERATOR_TYPE_MAP = new Map(
  OPERATOR_TYPE_OPTIONS.map((item) => [item.value, item]),
)

export function configTypeLabel(value?: number | null): string {
  if (value == null) return '未知'
  return CONFIG_TYPE_MAP.get(value as ConfigType)?.label ?? '未知'
}

export function configTypeTagType(value?: number | null): TagType {
  if (value == null) return 'info'
  return CONFIG_TYPE_MAP.get(value as ConfigType)?.tagType ?? 'info'
}

export function backupStatusLabel(
  value?: number | null,
  fallback?: string,
): string {
  if (fallback) return fallback
  if (value == null) return '未知'
  return BACKUP_STATUS_MAP.get(value as BackupStatus)?.label ?? '未知'
}

export function backupStatusTagType(value?: number | null): TagType {
  if (value == null) return 'info'
  return BACKUP_STATUS_MAP.get(value as BackupStatus)?.tagType ?? 'info'
}

export function operLogStatusLabel(value?: number | null): string {
  if (value == null) return '未知'
  return OPER_LOG_STATUS_MAP.get(value as OperLogStatus)?.label ?? '未知'
}

export function operLogStatusTagType(value?: number | null): TagType {
  if (value == null) return 'info'
  return OPER_LOG_STATUS_MAP.get(value as OperLogStatus)?.tagType ?? 'info'
}

export function businessTypeLabel(value?: number | null): string {
  if (value == null) return '其他'
  return BUSINESS_TYPE_MAP.get(value)?.label ?? '其他'
}

export function businessTypeTagType(value?: number | null): TagType {
  if (value == null) return 'info'
  return BUSINESS_TYPE_MAP.get(value)?.tagType ?? 'info'
}

export function operatorTypeLabel(value?: number | null): string {
  if (value == null) return '其他'
  return OPERATOR_TYPE_MAP.get(value)?.label ?? '其他'
}

/** 请求方式标签颜色 */
export function requestMethodTagType(method?: string | null): TagType {
  switch ((method ?? '').toUpperCase()) {
    case 'GET':
      return 'success'
    case 'POST':
      return 'primary'
    case 'PUT':
      return 'warning'
    case 'DELETE':
      return 'danger'
    default:
      return 'info'
  }
}

/** 文件大小格式化 */
export function formatFileSize(value?: number | null): string {
  if (value == null || Number.isNaN(Number(value))) return '—'
  const bytes = Number(value)
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

/** 数字千分位展示 */
export function formatCount(value?: number | null): string {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return Number(value).toLocaleString('zh-CN')
}

/** 尝试格式化 JSON 文本，解析失败时原样展示 */
export function prettyJson(value?: string | null): string {
  if (!value || !value.trim()) return '—'
  const text = value.trim()
  if (!text.startsWith('{') && !text.startsWith('[')) return text
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}
