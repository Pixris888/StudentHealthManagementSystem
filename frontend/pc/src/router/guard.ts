import router from './index'
import { registerDynamicRoutes } from './dynamic'
import { useUserStore } from '@/stores/user'

router.beforeEach(async (to) => {
  document.title = to.meta.title
    ? `${to.meta.title} · 大学生健康管理平台`
    : '大学生健康管理平台'

  const userStore = useUserStore()

  if (to.path === '/login' || to.path === '/register') {
    return userStore.accessToken ? { path: '/home' } : true
  }

  if (!userStore.accessToken) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.reset()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (!userStore.routesReady) {
    registerDynamicRoutes(userStore.menus)
    userStore.markRoutesReady()
    // 按 path 重新解析，避免动态路由注册前先匹配到 404 的 name
    return { path: to.path, query: to.query, hash: to.hash, replace: true }
  }

  const roles = to.meta.roles
  if (
    Array.isArray(roles) &&
    roles.length > 0 &&
    !roles.some((role) => userStore.roles.includes(role))
  ) {
    return { path: '/403' }
  }

  if (to.meta.perms && !userStore.hasPermission(to.meta.perms)) {
    return { path: '/403' }
  }

  return true
})
