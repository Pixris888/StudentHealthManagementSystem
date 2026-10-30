import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getUserInfoApi, loginApi, logoutApi, registerApi } from '@/api/auth'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/utils/auth'
import { roleCodeLabel } from '@/utils/dict'
import type {
  LoginForm,
  LoginResult,
  RegisterForm,
  UserInfo,
} from '@/types/system'

const STUDENT_ROLE = 'STUDENT'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(getAccessToken())
  const refreshToken = ref(getRefreshToken())
  const userInfo = ref<UserInfo | null>(null)
  const loading = ref(false)

  const roles = computed(() => userInfo.value?.roles ?? [])
  const permissions = computed(() => userInfo.value?.permissions ?? [])
  const displayName = computed(
    () =>
      userInfo.value?.nickName ||
      userInfo.value?.realName ||
      userInfo.value?.username ||
      '同学',
  )
  const avatarText = computed(() => displayName.value.slice(0, 1))
  const isStudent = computed(() => roles.value.includes(STUDENT_ROLE))
  const roleNames = computed(() => roles.value.map((role) => roleCodeLabel(role)))
  const studentNo = computed(
    () => (userInfo.value as (UserInfo & { studentNo?: string }) | null)?.studentNo || '',
  )

  function applyLoginResult(data: LoginResult): void {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    setTokens(data.accessToken, data.refreshToken)
  }

  function assertStudent(): void {
    if (!roles.value.includes(STUDENT_ROLE)) {
      throw new Error('本 App 仅支持学生账号，校医和管理员请使用电脑端')
    }
  }

  async function fetchUserInfo(): Promise<UserInfo> {
    const data = await getUserInfoApi()
    userInfo.value = data
    assertStudent()
    return data
  }

  async function login(form: LoginForm): Promise<void> {
    loading.value = true
    try {
      const data = await loginApi(form)
      applyLoginResult(data)
      await fetchUserInfo()
    } catch (error) {
      reset()
      throw error
    } finally {
      loading.value = false
    }
  }

  async function register(form: RegisterForm): Promise<void> {
    loading.value = true
    try {
      const data = await registerApi(form)
      applyLoginResult(data)
      await fetchUserInfo()
    } catch (error) {
      reset()
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * App 启动时恢复登录态。
   * 网络异常时保留 Token，避免用户因为一次断网被强制退出。
   */
  async function bootstrap(): Promise<boolean> {
    if (!getAccessToken()) {
      reset()
      return false
    }
    if (userInfo.value) {
      assertStudent()
      return true
    }
    try {
      await fetchUserInfo()
      return true
    } catch (error) {
      const code = (error as { code?: number } | null)?.code
      const message = (error as Error | null)?.message || ''
      if (code === 401 || message.includes('仅支持学生账号')) {
        reset()
      }
      throw error
    }
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 后端会话失效也要完成本地清理
    }
    reset()
  }

  function reset(): void {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    clearTokens()
  }

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
    loading,
    roles,
    permissions,
    displayName,
    avatarText,
    isStudent,
    roleNames,
    studentNo,
    login,
    register,
    fetchUserInfo,
    bootstrap,
    logout,
    reset,
    hasPermission,
  }
})
