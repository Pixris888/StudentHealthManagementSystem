/** 健康知识库模块类型定义（字段与后端 VO/DTO 严格对齐） */

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
  auditBy?: number
  auditTime?: string
  auditRemark?: string
  publishBy?: number
  publishTime?: string
  createBy?: number
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

export interface ArticleSaveDTO {
  categoryId: number
  title: string
  coverUrl?: string
  author?: string
  summary?: string
  content: string
  source?: string
  docUrl?: string
  remark?: string
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

export interface CategorySaveDTO {
  categoryName: string
  parentId?: number
  orderNum?: number
  status?: EnableStatus
  remark?: string
}

export interface MaterialVO {
  id: number
  categoryId: number
  categoryName?: string
  title: string
  content?: string
  keyword?: string
  source?: string
  status: EnableStatus
  statusName: string
  auditStatus: AuditStatus
  auditStatusName: string
  auditBy?: number
  auditTime?: string
  auditRemark?: string
  createBy?: number
  createTime?: string
  updateTime?: string
  remark?: string
}

export interface MaterialQuery {
  page?: number
  size?: number
  categoryId?: number
  keyword?: string
  status?: EnableStatus
  auditStatus?: AuditStatus
}

export interface MaterialSaveDTO {
  categoryId: number
  title: string
  content: string
  keyword?: string
  source?: string
  status?: EnableStatus
  remark?: string
}

export interface AuditDTO {
  approved: boolean
  remark?: string
}
