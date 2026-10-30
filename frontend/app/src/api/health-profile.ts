import { get, put } from '@/utils/request'
import type { HealthProfile, HealthProfileSave } from '@/types/health'

/** 学生查看自己的档案 */
export function getMyHealthProfile(): Promise<HealthProfile> {
  return get<HealthProfile>('/health/profile/mine', undefined, { silent: true })
}

/** 学生新增/更新自己的档案 */
export function saveMyHealthProfile(data: HealthProfileSave): Promise<HealthProfile> {
  return put<HealthProfile>('/health/profile/mine', data)
}
