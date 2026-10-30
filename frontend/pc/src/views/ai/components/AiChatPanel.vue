<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Clock,
  MagicStick,
  Plus,
  Promotion,
  VideoPause,
} from '@element-plus/icons-vue'
import { askAi, feedbackConsultation } from '@/api/ai'
import { askAiStreamWeb } from '@/api/aiStream'
import { useUserStore } from '@/stores/user'
import { resolveAiError } from '@/utils/ai'
import { nowLocalDateTimeString } from '@/utils/format'
import type {
  AiAnswerVO,
  AiConsultationVO,
  AiStreamError,
  AiStreamHandlers,
  AiStreamMeta,
  AiStreamResult,
  ChatMessage,
  FeedbackStatus,
} from '@/types/ai'
import AiHistoryDrawer from './AiHistoryDrawer.vue'
import AiMessageBubble from './AiMessageBubble.vue'

const MAX_QUESTION_LENGTH = 500

const QUICK_QUESTIONS = [
  '最近总是熬夜，怎么调整作息？',
  '一周运动怎么安排比较合理？',
  '食堂怎么吃更健康？',
  '最近心情低落，有什么自我调节方法？',
]

const router = useRouter()
const userStore = useUserStore()

const messages = ref<ChatMessage[]>([])
const sessionId = ref('')
const input = ref('')
const sending = ref(false)
const canStop = ref(false)
const feedbackSubmittingId = ref<string | null>(null)
const historyVisible = ref(false)
const scrollRef = ref<HTMLElement | null>(null)

const pendingDeltas = new Map<
  string,
  { buffer: string; timer: ReturnType<typeof setTimeout> | null }
>()
let streamController: AbortController | null = null
let scrollTimer: ReturnType<typeof setTimeout> | null = null

const canSend = computed(
  () => input.value.trim().length > 0 && !sending.value,
)

function createId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `m-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

async function scrollToBottom(): Promise<void> {
  await nextTick()
  const element = scrollRef.value
  if (element) {
    element.scrollTop = element.scrollHeight
  }
}

function scheduleScrollToBottom(): void {
  if (scrollTimer) return
  scrollTimer = setTimeout(() => {
    scrollTimer = null
    void scrollToBottom()
  }, 120)
}

function errorText(error: unknown): string {
  const code = (error as { code?: string } | null)?.code
  if (code === 'ECONNABORTED' || code === 'ETIMEDOUT') {
    return 'AI 响应超时，请稍后重试。你也可以点击“重新发送”再次尝试。'
  }
  return resolveAiError(error, '回答失败，请稍后重试。')
}

function forceLogin(message = '登录状态已失效，请重新登录'): void {
  userStore.reset()
  ElMessage.error(message)
  void router.replace('/login')
}

function flushPendingDelta(message: ChatMessage): void {
  const entry = pendingDeltas.get(message.id)
  if (!entry) return
  if (entry.timer) {
    clearTimeout(entry.timer)
    entry.timer = null
  }
  if (entry.buffer) {
    message.content += entry.buffer
    entry.buffer = ''
  }
  pendingDeltas.delete(message.id)
}

function queueDelta(message: ChatMessage, chunk: string): void {
  if (!chunk) return
  message.loading = false
  message.streaming = true

  let entry = pendingDeltas.get(message.id)
  if (!entry) {
    entry = { buffer: '', timer: null }
    pendingDeltas.set(message.id, entry)
  }
  entry.buffer += chunk

  if (!entry.timer) {
    entry.timer = setTimeout(() => {
      const current = pendingDeltas.get(message.id)
      if (!current) return
      current.timer = null
      flushPendingDelta(message)
    }, 45)
  }
  scheduleScrollToBottom()
}

function resetAssistantMessage(message: ChatMessage): void {
  flushPendingDelta(message)
  message.content = ''
  message.loading = true
  message.streaming = false
  message.stopped = false
  message.error = false
  message.errorMessage = ''
  message.answer = undefined
  message.feedback = undefined
  message.createdAt = nowLocalDateTimeString()
}

function applyMeta(message: ChatMessage, meta: AiStreamMeta): void {
  sessionId.value = meta.sessionId || sessionId.value
  message.loading = false
  message.streaming = true
  message.answer = {
    consultationId: meta.consultationId,
    sessionId: meta.sessionId,
    answer: '',
    sources: meta.sources ?? [],
    knowledgeSource: meta.knowledgeSource,
    emergency: Boolean(meta.emergency),
    fallback: false,
    answerStatus: 0,
    consultTime: meta.consultTime,
  }
  scheduleScrollToBottom()
}

function applyDone(message: ChatMessage, answer: AiAnswerVO): void {
  flushPendingDelta(message)
  message.content = answer.answer || message.content
  message.answer = answer
  message.loading = false
  message.streaming = false
  message.stopped = false
  message.error = false
  message.errorMessage = ''
  message.feedback = 0
  message.createdAt = answer.consultTime || nowLocalDateTimeString()
  sessionId.value = answer.sessionId || sessionId.value
  scheduleScrollToBottom()
}

function applyStreamError(message: ChatMessage, error: AiStreamError): void {
  flushPendingDelta(message)
  message.loading = false
  message.streaming = false
  message.stopped = false
  message.error = true
  message.errorMessage = error.message || '回答失败，请稍后重试。'
  if (error.code === 401) {
    forceLogin(error.message || '登录状态已失效，请重新登录')
  }
  scheduleScrollToBottom()
}

function applyAborted(message: ChatMessage): void {
  flushPendingDelta(message)
  message.loading = false
  message.streaming = false
  message.error = false
  message.stopped = true
  scheduleScrollToBottom()
}

function streamHandlers(message: ChatMessage): AiStreamHandlers {
  return {
    onMeta(meta) {
      applyMeta(message, meta)
    },
    onDelta(content) {
      queueDelta(message, content)
    },
  }
}

async function runSyncFallback(
  message: ChatMessage,
  question: string,
): Promise<void> {
  canStop.value = false
  resetAssistantMessage(message)
  try {
    const answer = await askAi({
      question,
      sessionId: sessionId.value || undefined,
    })
    applyDone(message, answer)
  } catch (error) {
    applyStreamError(message, {
      code: (error as { code?: number } | null)?.code ?? 0,
      message: errorText(error),
      partial: false,
    })
  }
}

async function runAssistantAnswer(
  message: ChatMessage,
  question: string,
): Promise<void> {
  sending.value = true
  resetAssistantMessage(message)

  streamController = new AbortController()
  canStop.value = true

  try {
    const result: AiStreamResult = await askAiStreamWeb(
      { question, sessionId: sessionId.value || undefined },
      streamHandlers(message),
      streamController.signal,
    )

    if (result.status === 'fallback') {
      await runSyncFallback(message, question)
      return
    }
    if (result.status === 'error') {
      applyStreamError(message, result.error)
      return
    }
    if (result.status === 'aborted') {
      applyAborted(message)
      return
    }
    applyDone(message, result.answer)
  } catch (error) {
    applyStreamError(message, {
      code: (error as { code?: number } | null)?.code ?? 0,
      message: errorText(error),
      partial: Boolean(message.content),
    })
  } finally {
    flushPendingDelta(message)
    sending.value = false
    canStop.value = false
    streamController = null
    await scrollToBottom()
  }
}

async function sendQuestion(): Promise<void> {
  const question = input.value.trim()
  if (!question || sending.value) return
  if (question.length > MAX_QUESTION_LENGTH) {
    ElMessage.warning(`问题不能超过 ${MAX_QUESTION_LENGTH} 字`)
    return
  }

  messages.value.push({
    id: createId(),
    role: 'user',
    content: question,
    question,
    createdAt: nowLocalDateTimeString(),
  })
  input.value = ''

  messages.value.push({
    id: createId(),
    role: 'assistant',
    content: '',
    loading: true,
    question,
    createdAt: nowLocalDateTimeString(),
  })
  // 取回数组中的响应式代理，直接修改原始对象不会触发 Vue 更新
  const assistant = messages.value[messages.value.length - 1]!
  await scrollToBottom()
  await runAssistantAnswer(assistant, question)
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key !== 'Enter' || event.shiftKey) return
  if (event.isComposing) return
  event.preventDefault()
  void sendQuestion()
}

function quickFill(question: string): void {
  if (sending.value) return
  input.value = question
}

async function newConversation(): Promise<void> {
  if (sending.value) return
  if (messages.value.length) {
    try {
      await ElMessageBox.confirm(
        '新建对话会清空当前页面中的对话内容，历史记录仍会保留。是否继续？',
        '新建对话',
        {
          type: 'warning',
          confirmButtonText: '新建对话',
          cancelButtonText: '取消',
        },
      )
    } catch {
      return
    }
  }
  messages.value = []
  sessionId.value = ''
  input.value = ''
  feedbackSubmittingId.value = null
}

function recordsToMessages(records: AiConsultationVO[]): ChatMessage[] {
  const result: ChatMessage[] = []
  for (const item of records) {
    const time = item.consultTime || item.createTime || ''
    result.push({
      id: `u-${item.id}`,
      role: 'user',
      content: item.question,
      question: item.question,
      consultation: item,
      createdAt: time,
    })
    result.push({
      id: `a-${item.id}`,
      role: 'assistant',
      content: item.answer ?? '',
      consultation: item,
      feedback: item.feedback,
      createdAt: time,
    })
  }
  return result
}

async function handleHistorySelect(
  selectedSessionId: string,
  records: AiConsultationVO[],
): Promise<void> {
  const sorted = [...records].sort((a, b) => {
    const left = a.consultTime || a.createTime || ''
    const right = b.consultTime || b.createTime || ''
    return left.localeCompare(right)
  })
  messages.value = recordsToMessages(sorted)
  sessionId.value = selectedSessionId || sorted[0]?.sessionId || ''
  await scrollToBottom()
}

async function handleFeedback(
  message: ChatMessage,
  helpful: boolean,
): Promise<void> {
  const id = message.answer?.consultationId ?? message.consultation?.id
  if (!id) return
  const target: FeedbackStatus = helpful ? 1 : 2
  const current = message.feedback ?? message.consultation?.feedback ?? 0
  if (current === target) return

  feedbackSubmittingId.value = message.id
  try {
    const updated = await feedbackConsultation(id, { helpful })
    message.feedback = updated.feedback ?? target
    if (message.consultation) {
      message.consultation.feedback = message.feedback
    }
    ElMessage.success('反馈已提交')
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    feedbackSubmittingId.value = null
  }
}

async function handleRetry(message: ChatMessage): Promise<void> {
  if (sending.value || !message.question) return
  try {
    await ElMessageBox.confirm(
      '重新发送会再次请求 AI 回答，是否继续？',
      '重新发送',
      {
        type: 'warning',
        confirmButtonText: '重新发送',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  await runAssistantAnswer(message, message.question)
}

function stopStreaming(): void {
  if (!canStop.value) return
  streamController?.abort()
}

onBeforeUnmount(() => {
  streamController?.abort()
  if (scrollTimer) {
    clearTimeout(scrollTimer)
    scrollTimer = null
  }
})
</script>

<template>
  <section class="chat-panel panel-card">
    <header class="chat-head">
      <div class="chat-title">
        <span class="chat-title-icon">
          <el-icon><MagicStick /></el-icon>
        </span>
        <div class="chat-title-text">
          <strong>健康问答</strong>
          <small>流式回答 · 单条问题最多 500 字</small>
        </div>
        <el-tag
          v-if="sessionId"
          type="success"
          effect="plain"
          size="small"
          class="session-tag"
        >
          会话进行中
        </el-tag>
      </div>

      <div class="chat-actions">
        <el-button
          :icon="Clock"
          :disabled="sending"
          @click="historyVisible = true"
        >
          历史记录
        </el-button>
        <el-button :icon="Plus" :disabled="sending" @click="newConversation">
          新建对话
        </el-button>
      </div>
    </header>

    <div ref="scrollRef" class="chat-scroll">
      <div v-if="!messages.length" class="chat-welcome">
        <div class="welcome-icon">
          <el-icon><MagicStick /></el-icon>
        </div>
        <h3>你好，我是校园健康助手</h3>
        <p class="welcome-text">
          你可以问我睡眠、运动、饮食、情绪等健康问题。我会结合健康知识库给出建议，
          但我的回答不能替代医生诊断。
        </p>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="医疗免责声明"
          description="AI 建议仅供参考。如有持续或加重的身体不适，请及时到校医院就诊；紧急情况请立即拨打 120。"
          class="welcome-alert"
        />
        <div class="welcome-tips">
          <span>睡眠作息</span>
          <span>运动安排</span>
          <span>饮食营养</span>
          <span>情绪调节</span>
        </div>
      </div>

      <AiMessageBubble
        v-for="message in messages"
        :key="message.id"
        :message="message"
        :feedback-submitting="feedbackSubmittingId === message.id"
        @retry="handleRetry"
        @feedback="handleFeedback"
      />
    </div>

    <footer class="chat-composer">
      <div class="quick-questions">
        <span class="quick-label">快捷提问</span>
        <button
          v-for="question in QUICK_QUESTIONS"
          :key="question"
          type="button"
          class="quick-chip"
          :disabled="sending"
          @click="quickFill(question)"
        >
          {{ question }}
        </button>
      </div>

      <div class="composer-row">
        <el-input
          v-model="input"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 5 }"
          maxlength="500"
          show-word-limit
          resize="none"
          placeholder="输入你的健康问题，按 Enter 发送，Shift + Enter 换行"
          :disabled="sending"
          @keydown="handleKeydown"
        />
        <el-button
          v-if="sending && canStop"
          type="danger"
          plain
          :icon="VideoPause"
          class="send-button"
          @click="stopStreaming"
        >
          停止生成
        </el-button>
        <el-button
          v-else
          type="primary"
          :icon="Promotion"
          :loading="sending"
          :disabled="!canSend"
          class="send-button"
          @click="sendQuestion"
        >
          {{ sending ? 'AI 思考中' : '发送' }}
        </el-button>
      </div>

      <p class="composer-tip">
        AI 回答由模型生成，仅供健康参考，不能替代专业医疗诊断。
      </p>
    </footer>
  </section>

  <AiHistoryDrawer
    v-model="historyVisible"
    :disabled="sending"
    @select="handleHistorySelect"
  />
</template>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: clamp(480px, calc(100vh - 268px), 880px);
  overflow: hidden;
}

.chat-head {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 14px 20px;
  border-bottom: 1px solid var(--hp-border);
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.chat-title-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 36px;
  height: 36px;
  color: #fff;
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
  border-radius: 11px;
  box-shadow: 0 6px 14px rgba(31, 157, 122, 0.2);
}

.chat-title-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.chat-title-text strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.chat-title-text small {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.session-tag {
  flex-shrink: 0;
}

.chat-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.chat-scroll {
  flex: 1;
  padding: 20px 22px;
  overflow-y: auto;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 100% 0%, rgba(61, 220, 151, 0.05), transparent 45%),
    #fff;
}

.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  max-width: 620px;
  min-height: 100%;
  padding: 24px 16px;
  margin: 0 auto;
  text-align: center;
}

.welcome-icon {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  font-size: 28px;
  color: #fff;
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
  border-radius: 20px;
  box-shadow: 0 12px 28px rgba(31, 157, 122, 0.22);
}

.chat-welcome h3 {
  margin-bottom: 8px;
  font-size: 20px;
  font-weight: 700;
  color: var(--hp-text);
}

.welcome-text {
  max-width: 520px;
  margin-bottom: 16px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--hp-text-secondary);
}

.welcome-alert {
  width: 100%;
  max-width: 540px;
}

.welcome-tips {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 18px;
}

.welcome-tips span {
  padding: 5px 12px;
  font-size: 11.5px;
  color: var(--hp-primary-dark);
  background: var(--hp-mint);
  border-radius: 999px;
}

.chat-composer {
  flex-shrink: 0;
  padding: 12px 20px 14px;
  background: #fff;
  border-top: 1px solid var(--hp-border);
}

.quick-questions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 9px;
}

.quick-label {
  flex-shrink: 0;
  font-size: 11.5px;
  color: var(--hp-text-muted);
  white-space: nowrap;
}

.quick-chip {
  padding: 4px 10px;
  font-size: 11.5px;
  color: var(--hp-text-secondary);
  cursor: pointer;
  background: #f5faf8;
  border: 1px solid #e4efe9;
  border-radius: 999px;
  transition: all 0.18s ease;
}

.quick-chip:hover:not(:disabled) {
  color: var(--hp-primary-dark);
  background: var(--hp-mint);
  border-color: #c8e6da;
}

.quick-chip:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.composer-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.composer-row :deep(.el-textarea__inner) {
  min-height: 62px !important;
  padding: 10px 12px;
  line-height: 1.7;
  border-radius: 10px;
}

.send-button {
  flex-shrink: 0;
  width: 110px;
  height: 62px;
  border-radius: 10px;
}

.composer-tip {
  margin-top: 8px;
  font-size: 10.5px;
  color: var(--hp-text-muted);
}

/* 1366×768 等较矮屏幕：隐藏装饰元素，保证免责声明可见 */
@media (min-width: 769px) and (max-height: 820px) {
  .chat-welcome {
    padding: 12px;
  }

  .welcome-icon {
    display: none;
  }

  .welcome-tips {
    display: none;
  }

  .chat-welcome h3 {
    margin-bottom: 6px;
    font-size: 18px;
  }

  .chat-welcome .welcome-text {
    margin-bottom: 12px;
    font-size: 12.5px;
  }

  .welcome-alert :deep(.el-alert__title) {
    font-size: 12px;
  }

  .welcome-alert :deep(.el-alert__description) {
    margin-top: 2px;
    font-size: 11.5px;
    line-height: 1.5;
  }
}

@media (max-width: 768px) {
  .chat-panel {
    height: clamp(420px, calc(100dvh - 220px), 760px);
  }

  .chat-head {
    padding: 12px 14px;
  }

  .chat-title small {
    display: none;
  }

  .chat-scroll {
    padding: 16px 14px;
  }

  .chat-welcome {
    padding: 10px 6px;
  }

  .welcome-icon {
    width: 46px;
    height: 46px;
    margin-bottom: 10px;
    font-size: 21px;
    border-radius: 14px;
  }

  .chat-welcome h3 {
    margin-bottom: 6px;
    font-size: 17px;
  }

  .chat-welcome .welcome-text {
    margin-bottom: 10px;
    font-size: 12px;
    line-height: 1.65;
  }

  .welcome-alert :deep(.el-alert__title) {
    font-size: 12px;
  }

  .welcome-alert :deep(.el-alert__description) {
    margin-top: 2px;
    font-size: 11.5px;
    line-height: 1.5;
  }

  .welcome-tips {
    display: none;
  }

  .chat-composer {
    padding: 10px 14px 12px;
  }

  .quick-questions {
    flex-wrap: nowrap;
    padding-bottom: 4px;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .quick-questions::-webkit-scrollbar {
    display: none;
  }

  .quick-chip {
    flex-shrink: 0;
    white-space: nowrap;
  }

  .composer-row {
    flex-direction: column;
    align-items: stretch;
  }

  .send-button {
    width: 100%;
    height: 40px;
  }
}
</style>
