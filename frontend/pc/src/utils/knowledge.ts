import type { TagType } from './dict'

export interface KnowledgeOption<T extends number = number> {
  value: T
  label: string
  tagType: TagType
}

export const ARTICLE_STATUS_OPTIONS: KnowledgeOption[] = [
  { value: 0, label: '草稿', tagType: 'info' },
  { value: 1, label: '已发布', tagType: 'success' },
  { value: 2, label: '已下架', tagType: 'warning' },
]

export const AUDIT_STATUS_OPTIONS: KnowledgeOption[] = [
  { value: 0, label: '待审核', tagType: 'warning' },
  { value: 1, label: '审核通过', tagType: 'success' },
  { value: 2, label: '审核不通过', tagType: 'danger' },
]

/** 分类与 AI 素材的启用状态 */
export const ENABLE_STATUS_OPTIONS: KnowledgeOption[] = [
  { value: 0, label: '停用', tagType: 'info' },
  { value: 1, label: '启用', tagType: 'success' },
]

const ARTICLE_STATUS_MAP = new Map(
  ARTICLE_STATUS_OPTIONS.map((item) => [item.value, item]),
)
const AUDIT_STATUS_MAP = new Map(
  AUDIT_STATUS_OPTIONS.map((item) => [item.value, item]),
)
const ENABLE_STATUS_MAP = new Map(
  ENABLE_STATUS_OPTIONS.map((item) => [item.value, item]),
)

export function articleStatusTagType(value?: number | null): TagType {
  return (value == null ? undefined : ARTICLE_STATUS_MAP.get(value)?.tagType) ?? 'info'
}

export function auditStatusTagType(value?: number | null): TagType {
  return (value == null ? undefined : AUDIT_STATUS_MAP.get(value)?.tagType) ?? 'info'
}

export function enableStatusTagType(value?: number | null): TagType {
  return (value == null ? undefined : ENABLE_STATUS_MAP.get(value)?.tagType) ?? 'info'
}

/** 后端已返回 statusName，缺失时才回退到本地字典 */
export function articleStatusLabel(
  value?: number | null,
  fallback?: string,
): string {
  if (fallback) return fallback
  if (value == null) return '未知'
  return ARTICLE_STATUS_MAP.get(value)?.label ?? '未知'
}

export function auditStatusLabel(
  value?: number | null,
  fallback?: string,
): string {
  if (fallback) return fallback
  if (value == null) return '未知'
  return AUDIT_STATUS_MAP.get(value)?.label ?? '未知'
}

export function enableStatusLabel(
  value?: number | null,
  fallback?: string,
): string {
  if (fallback) return fallback
  if (value == null) return '未知'
  return ENABLE_STATUS_MAP.get(value)?.label ?? '未知'
}
