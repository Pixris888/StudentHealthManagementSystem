import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getUnreadCount,
  markAllMessagesRead,
  markMessageRead,
} from '@/api/message'
import { getAccessToken } from '@/utils/auth'

export const useMessageStore = defineStore('message', () => {
  const unread = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  async function refresh(): Promise<void> {
    if (!getAccessToken()) {
      unread.value = 0
      return
    }
    try {
      const result = await getUnreadCount()
      unread.value = result.count ?? 0
    } catch {
      // 未读数失败不打扰用户
    }
  }

  async function markRead(id: number): Promise<void> {
    try {
      const result = await markMessageRead(id)
      unread.value = result.count ?? Math.max(0, unread.value - 1)
    } catch {
      // 详情页仍可正常阅读
    }
  }

  async function markAllRead(): Promise<void> {
    const result = await markAllMessagesRead()
    unread.value = result.count ?? 0
  }

  function startPolling(): void {
    if (timer) return
    if (!getAccessToken()) return
    refresh()
    timer = setInterval(() => {
      if (getAccessToken()) refresh()
    }, 60000)
  }

  function stopPolling(): void {
    if (!timer) return
    clearInterval(timer)
    timer = null
  }

  function reset(): void {
    unread.value = 0
    stopPolling()
  }

  return {
    unread,
    refresh,
    markRead,
    markAllRead,
    startPolling,
    stopPolling,
    reset,
  }
})
