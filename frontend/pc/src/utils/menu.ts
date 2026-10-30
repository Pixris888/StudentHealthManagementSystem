import type { MenuTree } from '@/types/system'

export interface MenuCounts {
  total: number
  directories: number
  pages: number
  buttons: number
}

/** 递归统计菜单树中各类节点数量 */
export function countMenuTypes(menus: MenuTree[]): MenuCounts {
  const result: MenuCounts = { total: 0, directories: 0, pages: 0, buttons: 0 }
  const walk = (nodes: MenuTree[]) => {
    for (const node of nodes) {
      result.total += 1
      if (node.menuType === 1) result.directories += 1
      if (node.menuType === 2) result.pages += 1
      if (node.menuType === 3) result.buttons += 1
      if (node.children?.length) walk(node.children)
    }
  }
  walk(menus)
  return result
}

/** 拼接父子菜单的完整路由地址 */
export function joinMenuPath(parentPath: string, path?: string | null): string {
  const child = (path ?? '').trim()
  if (!child) return parentPath || '/'
  if (child.startsWith('/')) return child.replace(/\/+/g, '/')
  const base = parentPath && parentPath !== '/' ? parentPath.replace(/\/+$/, '') : ''
  return `${base}/${child}`.replace(/\/+/g, '/')
}

/** 判断节点自身或后代是否存在满足条件的节点 */
export function someMenu(
  node: MenuTree,
  predicate: (menu: MenuTree) => boolean,
): boolean {
  if (predicate(node)) return true
  return Boolean(node.children?.some((child) => someMenu(child, predicate)))
}

/** 收集节点及其所有后代 ID（编辑菜单时排除父级选项） */
export function collectMenuIds(menus: MenuTree[]): number[] {
  const ids: number[] = []
  const walk = (nodes: MenuTree[]) => {
    for (const node of nodes) {
      ids.push(node.id)
      if (node.children?.length) walk(node.children)
    }
  }
  walk(menus)
  return ids
}
