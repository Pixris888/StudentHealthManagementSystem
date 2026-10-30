import { del, get, post, put } from '@/utils/request'
import type { PageResult } from '@/types/system'
import type { User, UserCreate, UserQuery, UserUpdate } from '@/types/system'

/** 用户分页查询 */
export function pageUsers(params: UserQuery): Promise<PageResult<User>> {
  return get<PageResult<User>>('/system/user/page', params)
}

/** 用户详情 */
export function getUserDetail(id: number): Promise<User> {
  return get<User>(`/system/user/${id}`)
}

/** 查询用户已分配的角色 ID */
export function getUserRoleIds(id: number): Promise<number[]> {
  return get<number[]>(`/system/user/${id}/roles`)
}

/** 新增用户 */
export function createUser(data: UserCreate): Promise<number> {
  return post<number>('/system/user', data)
}

/** 修改用户基础信息 */
export function updateUser(data: UserUpdate): Promise<boolean> {
  return put<boolean>('/system/user', data)
}

/** 删除用户（支持逗号分隔的多个 ID） */
export function deleteUsers(ids: number[]): Promise<boolean> {
  return del<boolean>(`/system/user/${ids.join(',')}`)
}

/** 管理员重置用户密码 */
export function resetUserPassword(
  id: number,
  newPassword: string,
): Promise<boolean> {
  return put<boolean>(`/system/user/${id}/password`, { newPassword })
}

/** 启用/禁用用户 */
export function updateUserStatus(
  id: number,
  status: number,
): Promise<boolean> {
  return put<boolean>(`/system/user/${id}/status/${status}`)
}

/** 分配角色 */
export function assignUserRoles(id: number, ids: number[]): Promise<boolean> {
  return put<boolean>(`/system/user/${id}/roles`, { ids })
}
