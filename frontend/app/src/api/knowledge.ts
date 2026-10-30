import { get } from '@/utils/request'
import type { PageResult } from '@/types/system'
import type { ArticleQuery, ArticleVO, CategoryVO } from '@/types/knowledge'

/** 分页浏览已发布且审核通过的文章（列表不含正文） */
export function pagePublishedArticles(params: ArticleQuery): Promise<PageResult<ArticleVO>> {
  return get<PageResult<ArticleVO>>('/health/article/page', { ...params })
}

/** 热门文章：按浏览量降序 */
export function getHotArticles(limit = 5): Promise<ArticleVO[]> {
  return get<ArticleVO[]>('/health/article/hot', { limit }, { silent: true })
}

/** 学生端文章详情：每次成功查看已发布文章会使浏览量 +1 */
export function getArticleDetail(id: number): Promise<ArticleVO> {
  return get<ArticleVO>(`/health/article/${id}`)
}

/** 启用分类列表（学生端） */
export function listEnabledCategories(): Promise<CategoryVO[]> {
  return get<CategoryVO[]>('/health/article_category/list', undefined, {
    silent: true,
  })
}

/** 启用分类树（学生端） */
export function getEnabledCategoryTree(): Promise<CategoryVO[]> {
  return get<CategoryVO[]>('/health/article_category/tree', undefined, {
    silent: true,
  })
}
