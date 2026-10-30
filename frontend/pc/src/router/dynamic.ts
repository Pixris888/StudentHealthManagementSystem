import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import router from './index'
import { joinMenuPath } from '@/utils/menu'
import type { MenuTree } from '@/types/system'
import PlaceholderView from '@/views/placeholder/index.vue'

type AsyncView = () => Promise<{ default: Component }>
type ViewComponent = Component | AsyncView

/**
 * 约定：后端 component 字段对应 src/views 下的文件路径，例如
 * system/role/index -> src/views/system/role/index.vue
 */
const viewModules = import.meta.glob([
  '../views/**/*.vue',
  '!../views/placeholder/**',
]) as Record<string, AsyncView>
const addedRouteNames: string[] = []

function resolveView(component?: string | null): ViewComponent {
  if (!component || component === 'Layout') return PlaceholderView
  const normalized = component.replace(/^\/+/, '').replace(/\.vue$/, '')
  return viewModules[`../views/${normalized}.vue`] ?? PlaceholderView
}

function buildMenuRoutes(
  menus: MenuTree[],
  parentPath = '',
  result: RouteRecordRaw[] = [],
): RouteRecordRaw[] {
  for (const menu of menus) {
    if (menu.menuType === 3) continue
    const fullPath = joinMenuPath(parentPath, menu.path)

    if (menu.menuType === 2) {
      result.push({
        path: fullPath,
        name: `Menu_${menu.id}`,
        component: resolveView(menu.component),
        meta: {
          title: menu.menuName,
          icon: menu.icon ?? undefined,
          perms: menu.perms,
          menuId: menu.id,
        },
      })
    }

    if (menu.children?.length) {
      buildMenuRoutes(menu.children, fullPath, result)
    }
  }
  return result
}

/** 登录后按后端菜单注册动态路由 */
export function registerDynamicRoutes(menus: MenuTree[]): void {
  resetDynamicRoutes()
  const routes = buildMenuRoutes(menus)
  for (const route of routes) {
    router.addRoute('Layout', route)
    if (route.name) addedRouteNames.push(String(route.name))
  }
}

/** 清理动态路由，切换账号时避免权限残留 */
export function resetDynamicRoutes(): void {
  for (const name of addedRouteNames.splice(0)) {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  }
}
