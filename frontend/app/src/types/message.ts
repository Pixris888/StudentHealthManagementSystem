export type MessageType = 1 | 2 | 3 | 4 | 5
export type ReadStatus = 0 | 1
export type MessageSendType = 1 | 2

export interface MessageVO {
  id: number
  receiverId: number
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

export interface UnreadCountVO {
  count: number
}
