import { del, get, post, put } from '@/utils/request'
import type {
  PageResult,
  SysConfig,
  SysConfigForm,
  SysConfigQuery,
} from '@/types/system'

/** 参数分页查询 */
export function pageConfigs(
  params: SysConfigQuery,
): Promise<PageResult<SysConfig>> {
  return get<PageResult<SysConfig>>('/system/config/page', params)
}

/** 参数详情 */
export function getConfig(id: number): Promise<SysConfig> {
  return get<SysConfig>(`/system/config/${id}`)
}

/** 按键名查询参数 */
export function getConfigByKey(configKey: string): Promise<SysConfig> {
  return get<SysConfig>(
    `/system/config/key/${encodeURIComponent(configKey)}`,
  )
}

/** 新增参数 */
export function createConfig(data: SysConfigForm): Promise<boolean> {
  return post<boolean>('/system/config', data)
}

/** 修改参数 */
export function updateConfig(data: SysConfigForm): Promise<boolean> {
  return put<boolean>('/system/config', data)
}

/** 批量删除自定义参数 */
export function deleteConfigs(ids: number[]): Promise<boolean> {
  return del<boolean>(`/system/config/${ids.join(',')}`)
}
