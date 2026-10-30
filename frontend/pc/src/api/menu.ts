import { del, get, post, put } from '@/utils/request'
import type { MenuDetail, MenuForm, MenuTree } from '@/types/system'

export function getMenuTree(): Promise<MenuTree[]> {
  return get<MenuTree[]>('/system/menu/tree')
}

export function getMenuDetail(id: number): Promise<MenuDetail> {
  return get<MenuDetail>(`/system/menu/${id}`)
}

export function createMenu(data: MenuForm): Promise<number> {
  return post<number>('/system/menu', data)
}

export function updateMenu(data: MenuForm): Promise<boolean> {
  return put<boolean>('/system/menu', data)
}

export function deleteMenu(id: number): Promise<boolean> {
  return del<boolean>(`/system/menu/${id}`)
}
