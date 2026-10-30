import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getUserInfoApi, loginApi, logoutApi, registerApi } from '@/api/auth'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/utils/auth'
import { countMenuTypes } from '@/utils/menu'
import { roleCodeLabel } from '@/utils/dict'
import type {
  LoginForm,
  LoginResult,
  MenuTree,
  RegisterForm,
  UserInfo,
} from '@/types/system'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(getAccessToken())
  const refreshToken = ref(getRefreshToken())
  const userInfo = ref<UserInfo | null>(null)
  const routesReady = ref(false)

  const roles = computed(() => userInfo.value?.roles ?? [])
  const permissions = computed(() => userInfo.value?.permissions ?? [])
  const menus = computed<MenuTree[]>(() => userInfo.value?.menus ?? [])
  const pageMenuCount = computed(() => countMenuTypes(menus.value).pages)
  const displayName = computed(
    () =>
      userInfo.value?.nickName ||
      userInfo.value?.realName ||
      userInfo.value?.username ||
      '未登录',
  )
  const avatarText = computed(() => displayName.value.charAt(0).toUpperCase())
  const roleNames = computed(() => roles.value.map((role) => roleCodeLabel(role)))

  function applyLoginResult(data: LoginResult): void {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    setTokens(data.accessToken, data.refreshToken)
  }

  async function loadUserInfoAfterAuth(): Promise<void> {
    try {
      await fetchUserInfo()
    } catch (error) {
      // 认证成功但用户信息加载失败时，清理已写入的令牌，避免半登录状态
      reset()
      throw error
    }
  }

  async function login(form: LoginForm): Promise<void> {
    const data = await loginApi(form)
    applyLoginResult(data)
    await loadUserInfoAfterAuth()
  }

  async function register(form: RegisterForm): Promise<void> {
    const data = await registerApi(form)
    applyLoginResult(data)
    await loadUserInfoAfterAuth()
  }

  async function fetchUserInfo(): Promise<UserInfo> {
    const data = await getUserInfoApi()
    userInfo.value = data
    return data
  }

  function markRoutesReady(): void {
    routesReady.value = true
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 即使后端会话已经失效，前端也必须完成本地清理
    }
    reset()
  }

  function reset(): void {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    routesReady.value = false
    clearTokens()
  }

  /**
   * 权限判断：超级管理员拥有全部权限；数组表示满足任意一项即可。
   */
  function hasPermission(value: string | string[] | null | undefined): boolean {
    if (!value || (Array.isArray(value) && value.length === 0)) return true
    if (roles.value.includes('SUPER_ADMIN')) return true
    const required = Array.isArray(value) ? value : [value]
    return required.some((perm) => permissions.value.includes(perm))
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    routesReady,
    roles,
    permissions,
    menus,
    pageMenuCount,
    displayName,
    avatarText,
    roleNames,
    login,
    register,
    fetchUserInfo,
    markRoutesReady,
    logout,
    reset,
    hasPermission,
  }
})
