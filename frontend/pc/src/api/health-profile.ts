import { get, put } from '@/utils/request'
import type { PageResult } from '@/types/system'
import type {
  HealthProfile,
  HealthProfileQuery,
  HealthProfileSave,
} from '@/types/health'

/** 学生查看自己的档案 */
export function getMyHealthProfile(): Promise<HealthProfile> {
  return get<HealthProfile>('/health/profile/mine')
}

/** 学生新增/更新自己的档案 */
export function saveMyHealthProfile(
  data: HealthProfileSave,
): Promise<HealthProfile> {
  return put<HealthProfile>('/health/profile/mine', data)
}

/** 校医/超级管理员分页查询学生档案 */
export function pageHealthProfiles(
  params: HealthProfileQuery,
): Promise<PageResult<HealthProfile>> {
  return get<PageResult<HealthProfile>>('/health/profile/page', params)
}

/** 校医/超级管理员查看指定学生档案 */
export function getHealthProfileDetail(userId: number): Promise<HealthProfile> {
  return get<HealthProfile>(`/health/profile/${userId}`)
}
