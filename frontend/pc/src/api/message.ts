import { get, post } from '@/utils/request'
import type { PageResult } from '@/types/system'
import type {
  MessageQuery,
  MessageReceiverQuery,
  MessageReceiverVO,
  MessageSendDTO,
  MessageVO,
  UnreadCountVO,
  WarningRunResultVO,
} from '@/types/message'

/** 当前用户收件箱（个人消息 + 全体广播） */
export function getMessagePage(
  params: MessageQuery,
): Promise<PageResult<MessageVO>> {
  return get<PageResult<MessageVO>>('/message/page', params)
}

/** 消息详情：不会自动标记已读 */
export function getMessageDetail(id: number): Promise<MessageVO> {
  return get<MessageVO>(`/message/${id}`)
}

/** 当前用户未读消息数 */
export function getUnreadCount(): Promise<UnreadCountVO> {
  return get<UnreadCountVO>('/message/unread-count')
}

/** 标记单条消息已读，返回最新未读数 */
export function markMessageRead(id: number): Promise<UnreadCountVO> {
  return post<UnreadCountVO>(`/message/${id}/read`)
}

/** 全部标记已读，返回最新未读数 */
export function markAllMessagesRead(): Promise<UnreadCountVO> {
  return post<UnreadCountVO>('/message/read-all')
}

/** 发送公告 / 健康关怀 / 其他消息，返回实际生成条数 */
export function sendMessage(data: MessageSendDTO): Promise<number> {
  return post<number>('/message/send', data)
}

/** 当前用户已发送消息分页 */
export function getSentMessagePage(
  params: MessageQuery,
): Promise<PageResult<MessageVO>> {
  return get<PageResult<MessageVO>>('/message/sent/page', params)
}

/** 发送关怀留言时搜索启用的学生接收人 */
export function getReceiverPage(
  params: MessageReceiverQuery,
): Promise<PageResult<MessageReceiverVO>> {
  return get<PageResult<MessageReceiverVO>>('/message/receiver/page', params)
}

/** 超级管理员手动触发指定日期的健康预警（不传默认昨天） */
export function runWarning(date?: string): Promise<WarningRunResultVO> {
  return post<WarningRunResultVO>(
    '/message/warning/run',
    undefined,
    date ? { params: { date } } : undefined,
  )
}
