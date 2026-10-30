import type { TagType } from './dict'
import type { MessageType, SendMessageType } from '@/types/message'

export interface MessageTypeOption {
  value: MessageType
  label: string
  tagType: TagType
  /** 是否只能由系统预警生成 */
  systemOnly: boolean
}

export const MESSAGE_TYPE_OPTIONS: MessageTypeOption[] = [
  { value: 1, label: '熬夜预警', tagType: 'warning', systemOnly: true },
  { value: 2, label: '运动不足预警', tagType: 'warning', systemOnly: true },
  { value: 3, label: '校园公告', tagType: 'primary', systemOnly: false },
  { value: 4, label: '健康关怀', tagType: 'success', systemOnly: false },
  { value: 5, label: '其他消息', tagType: 'info', systemOnly: false },
]

/** 发送弹窗只允许手工消息类型 */
export const SEND_MESSAGE_TYPE_OPTIONS = MESSAGE_TYPE_OPTIONS.filter(
  (item): item is MessageTypeOption & { value: SendMessageType } =>
    item.value >= 3,
)

const MESSAGE_TYPE_MAP = new Map(
  MESSAGE_TYPE_OPTIONS.map((item) => [item.value, item]),
)

export function messageTypeTagType(value?: number | null): TagType {
  return (
    (value == null ? undefined : MESSAGE_TYPE_MAP.get(value as MessageType)?.tagType) ??
    'info'
  )
}

export function messageTypeLabel(
  value?: number | null,
  fallback?: string,
): string {
  if (fallback) return fallback
  if (value == null) return '未知'
  return MESSAGE_TYPE_MAP.get(value as MessageType)?.label ?? '未知'
}
