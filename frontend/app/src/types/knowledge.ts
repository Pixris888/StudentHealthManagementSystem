export type ArticleStatus = 0 | 1 | 2
export type AuditStatus = 0 | 1 | 2
export type EnableStatus = 0 | 1

export interface ArticleVO {
  id: number
  categoryId: number
  categoryName?: string
  title: string
  coverUrl?: string
  author?: string
  summary?: string
  content?: string
  source?: string
  docUrl?: string
  viewCount: number
  likeCount: number
  status: ArticleStatus
  statusName: string
  auditStatus: AuditStatus
  auditStatusName: string
  publishTime?: string
  createTime?: string
  updateTime?: string
  remark?: string
}

export interface ArticleQuery {
  page?: number
  size?: number
  categoryId?: number
  keyword?: string
  status?: ArticleStatus
  auditStatus?: AuditStatus
}

export interface CategoryVO {
  id: number
  categoryName: string
  parentId: number
  orderNum: number
  status: EnableStatus
  statusName: string
  articleCount: number
  children?: CategoryVO[]
  createTime?: string
  remark?: string
}
