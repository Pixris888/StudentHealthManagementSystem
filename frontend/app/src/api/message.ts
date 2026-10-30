import { get, post } from '@/utils/request'
import type { PageResult } from '@/types/system'
import type {
  MessageQuery,
  MessageVO,
  UnreadCountVO,
} from '@/types/message'

/** 当前用户收件箱（个人消息 + 全体广播） */
export function getMessagePage(params: MessageQuery): Promise<PageResult<MessageVO>> {
  return get<PageResult<MessageVO>>('/message/page', { ...params })
}

/** 消息详情：不会自动标记已读 */
export function getMessageDetail(id: number): Promise<MessageVO> {
  return get<MessageVO>(`/message/${id}`)
}

/** 当前用户未读消息数 */
export function getUnreadCount(): Promise<UnreadCountVO> {
  return get<UnreadCountVO>('/message/unread-count', undefined, { silent: true })
}

/** 标记单条消息已读，返回最新未读数 */
export function markMessageRead(id: number): Promise<UnreadCountVO> {
  return post<UnreadCountVO>(`/message/${id}/read`)
}

/** 全部标记已读，返回最新未读数 */
export function markAllMessagesRead(): Promise<UnreadCountVO> {
  return post<UnreadCountVO>('/message/read-all')
}
