/** 消息通知模块类型定义（与后端 MessageVO / DTO 严格对齐） */

export type MessageType = 1 | 2 | 3 | 4 | 5
/** 手工可发送的消息类型：校园公告、健康关怀、其他 */
export type SendMessageType = 3 | 4 | 5
export type ReadStatus = 0 | 1
export type MessageSendType = 1 | 2

export interface MessageVO {
  id: number
  receiverId: number
  /** 已发送列表展示用：接收人姓名；广播为“全体学生” */
  receiverName?: string
  messageType: MessageType
  messageTypeName: string
  title: string
  content: string
  senderId: number
  senderName: string
  sendType: MessageSendType
  sendTypeName: string
  publishTime?: string
  readStatus?: ReadStatus
  readTime?: string
  createTime?: string
}

export interface MessageQuery {
  page?: number
  size?: number
  messageType?: MessageType
  readStatus?: ReadStatus
}

export interface MessageSendDTO {
  messageType: SendMessageType
  title: string
  content: string
  receiverIds: number[]
}

export interface MessageReceiverVO {
  id: number
  username: string
  realName: string
  studentNo?: string
  deptName?: string
}

export interface MessageReceiverQuery {
  page?: number
  size?: number
  keyword?: string
}

export interface UnreadCountVO {
  count: number
}

export interface WarningRunResultVO {
  date: string
  scannedCount: number
  generatedCount: number
  skippedCount: number
  locked: boolean
  message: string
}
