import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { AiConsultationVO } from '@/types/ai'

/**
 * 历史会话页 -> AI 对话页的数据中转。
 * 不直接把整段会话塞进 URL，避免长度和编码问题。
 */
export const useAiStore = defineStore('ai', () => {
  const pendingSessionId = ref('')
  const pendingRecords = ref<AiConsultationVO[]>([])

  function setPending(sessionId: string, records: AiConsultationVO[]): void {
    pendingSessionId.value = sessionId
    pendingRecords.value = records
  }

  function takePending(): { sessionId: string; records: AiConsultationVO[] } | null {
    if (!pendingRecords.value.length) return null
    const result = {
      sessionId: pendingSessionId.value,
      records: pendingRecords.value,
    }
    pendingSessionId.value = ''
    pendingRecords.value = []
    return result
  }

  function clear(): void {
    pendingSessionId.value = ''
    pendingRecords.value = []
  }

  return { pendingSessionId, pendingRecords, setPending, takePending, clear }
})
