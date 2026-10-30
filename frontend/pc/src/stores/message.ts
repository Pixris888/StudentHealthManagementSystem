import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getUnreadCount } from '@/api/message'
import { useUserStore } from '@/stores/user'

const POLL_INTERVAL = 60_000

/**
 * 消息未读书与轮询。
 * 登录后由主布局启动；退出登录、密码失效时由主布局停止并清零。
 */
export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)
  let timer: number | null = null
  let visibilityHandler: (() => void) | null = null

  async function refreshUnread(): Promise<void> {
    const userStore = useUserStore()
    if (!userStore.accessToken || !userStore.hasPermission('message:list')) {
      return
    }
    try {
      const data = await getUnreadCount()
      unreadCount.value = data.count ?? 0
    } catch {
      // 401 由 request 拦截器统一处理，这里避免轮询抛出未捕获异常
    }
  }

  function startPolling(): void {
    stopPolling()
    void refreshUnread()
    timer = window.setInterval(() => {
      void refreshUnread()
    }, POLL_INTERVAL)
    visibilityHandler = () => {
      if (document.visibilityState === 'visible') {
        void refreshUnread()
      }
    }
    document.addEventListener('visibilitychange', visibilityHandler)
  }

  function stopPolling(): void {
    if (timer !== null) {
      window.clearInterval(timer)
      timer = null
    }
    if (visibilityHandler) {
      document.removeEventListener('visibilitychange', visibilityHandler)
      visibilityHandler = null
    }
  }

  function setUnreadCount(count: number): void {
    unreadCount.value = count ?? 0
  }

  /** 停止轮询并清空角标，用于退出登录 */
  function clear(): void {
    stopPolling()
    unreadCount.value = 0
  }

  return {
    unreadCount,
    refreshUnread,
    startPolling,
    stopPolling,
    setUnreadCount,
    clear,
  }
})
