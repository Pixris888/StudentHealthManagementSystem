import { del, get, post, put } from '@/utils/request'
import type { PageResult } from '@/types/system'
import type {
  ArticleQuery,
  ArticleSaveDTO,
  ArticleVO,
  AuditDTO,
  CategorySaveDTO,
  CategoryVO,
  MaterialQuery,
  MaterialSaveDTO,
  MaterialVO,
} from '@/types/knowledge'

/* ---------------- 文章：学生端 ---------------- */

/** 分页浏览已发布且审核通过的文章（列表不含正文） */
export function pagePublishedArticles(
  params: ArticleQuery,
): Promise<PageResult<ArticleVO>> {
  return get<PageResult<ArticleVO>>('/health/article/page', params)
}

/** 热门文章：按浏览量降序 */
export function getHotArticles(limit = 5): Promise<ArticleVO[]> {
  return get<ArticleVO[]>('/health/article/hot', { limit })
}

/** 学生端文章详情：每次成功查看已发布文章会使浏览量 +1 */
export function getArticleDetail(id: number): Promise<ArticleVO> {
  return get<ArticleVO>(`/health/article/${id}`)
}

/* ---------------- 文章：管理端 ---------------- */

/** 管理端分页：可筛选文章状态与审核状态（列表不含正文） */
export function pageManageArticles(
  params: ArticleQuery,
): Promise<PageResult<ArticleVO>> {
  return get<PageResult<ArticleVO>>('/health/article/manage/page', params)
}

/** 管理端详情：包含正文，不增加浏览量 */
export function getManageArticleDetail(id: number): Promise<ArticleVO> {
  return get<ArticleVO>(`/health/article/manage/${id}`)
}

/** 新增文章：保存后为草稿 / 待审核 */
export function createArticle(data: ArticleSaveDTO): Promise<ArticleVO> {
  return post<ArticleVO>('/health/article', data)
}

/** 编辑文章：保存后重置为草稿 / 待审核 */
export function updateArticle(
  id: number,
  data: ArticleSaveDTO,
): Promise<ArticleVO> {
  return put<ArticleVO>(`/health/article/${id}`, data)
}

/** 批量删除文章：已发布文章需先下架 */
export function deleteArticles(ids: number[]): Promise<boolean> {
  return del<boolean>(`/health/article/${ids.join(',')}`)
}

/** 发布文章：前置条件为审核通过且未发布 */
export function publishArticle(id: number): Promise<ArticleVO> {
  return post<ArticleVO>(`/health/article/${id}/publish`)
}

/** 下架文章 */
export function offlineArticle(id: number): Promise<ArticleVO> {
  return post<ArticleVO>(`/health/article/${id}/offline`)
}

/** 审核文章（仅超级管理员） */
export function auditArticle(
  id: number,
  data: AuditDTO,
): Promise<ArticleVO> {
  return post<ArticleVO>(`/health/article/${id}/audit`, data)
}

/* ---------------- 文章分类 ---------------- */

/** 启用分类列表（学生端） */
export function listEnabledCategories(): Promise<CategoryVO[]> {
  return get<CategoryVO[]>('/health/article_category/list')
}

/** 启用分类树（学生端） */
export function getEnabledCategoryTree(): Promise<CategoryVO[]> {
  return get<CategoryVO[]>('/health/article_category/tree')
}

/** 全部分类列表（管理端，含停用分类） */
export function listManageCategories(): Promise<CategoryVO[]> {
  return get<CategoryVO[]>('/health/article_category/manage/list')
}

/** 全部分类树（管理端，含停用分类） */
export function getManageCategoryTree(): Promise<CategoryVO[]> {
  return get<CategoryVO[]>('/health/article_category/manage/tree')
}

export function createCategory(
  data: CategorySaveDTO,
): Promise<CategoryVO> {
  return post<CategoryVO>('/health/article_category', data)
}

export function updateCategory(
  id: number,
  data: CategorySaveDTO,
): Promise<CategoryVO> {
  return put<CategoryVO>(`/health/article_category/${id}`, data)
}

/** 删除分类：存在子分类 / 文章 / 素材时后端返回 409 */
export function deleteCategory(id: number): Promise<null> {
  return del<null>(`/health/article_category/${id}`)
}

/* ---------------- AI 知识素材 ---------------- */

/** 素材分页（列表不含正文） */
export function pageMaterials(
  params: MaterialQuery,
): Promise<PageResult<MaterialVO>> {
  return get<PageResult<MaterialVO>>('/health/knowledge_material/page', params)
}

/** 素材详情（包含正文） */
export function getMaterialDetail(id: number): Promise<MaterialVO> {
  return get<MaterialVO>(`/health/knowledge_material/${id}`)
}

/** 新增素材：保存后进入待审核 */
export function createMaterial(
  data: MaterialSaveDTO,
): Promise<MaterialVO> {
  return post<MaterialVO>('/health/knowledge_material', data)
}

/** 编辑素材：保存后重新进入待审核 */
export function updateMaterial(
  id: number,
  data: MaterialSaveDTO,
): Promise<MaterialVO> {
  return put<MaterialVO>(`/health/knowledge_material/${id}`, data)
}

/** 批量删除素材 */
export function deleteMaterials(ids: number[]): Promise<boolean> {
  return del<boolean>(`/health/knowledge_material/${ids.join(',')}`)
}

/** 审核素材（仅超级管理员） */
export function auditMaterial(
  id: number,
  data: AuditDTO,
): Promise<MaterialVO> {
  return post<MaterialVO>(`/health/knowledge_material/${id}/audit`, data)
}

/** 启用 / 停用素材：启用前必须审核通过 */
export function changeMaterialStatus(
  id: number,
  status: 0 | 1,
): Promise<MaterialVO> {
  return put<MaterialVO>(`/health/knowledge_material/${id}/status`, undefined, {
    params: { status },
  })
}
