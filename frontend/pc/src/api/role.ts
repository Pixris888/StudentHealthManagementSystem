import { del, get, post, put } from '@/utils/request'
import type { PageResult, Role, RoleDetail, RoleForm, RoleQuery } from '@/types/system'

export function pageRoles(params: RoleQuery): Promise<PageResult<Role>> {
  return get<PageResult<Role>>('/system/role/page', params)
}

export function listEnabledRoles(): Promise<Role[]> {
  return get<Role[]>('/system/role/enabled')
}

export function getRoleDetail(id: number): Promise<RoleDetail> {
  return get<RoleDetail>(`/system/role/${id}`)
}

export function getRoleMenuIds(id: number): Promise<number[]> {
  return get<number[]>(`/system/role/${id}/menus`)
}

export function createRole(data: RoleForm): Promise<number> {
  return post<number>('/system/role', data)
}

export function updateRole(data: RoleForm): Promise<boolean> {
  return put<boolean>('/system/role', data)
}

export function deleteRoles(ids: number[]): Promise<boolean> {
  return del<boolean>(`/system/role/${ids.join(',')}`)
}

export function assignRoleMenus(id: number, ids: number[]): Promise<boolean> {
  return put<boolean>(`/system/role/${id}/menus`, { ids })
}
