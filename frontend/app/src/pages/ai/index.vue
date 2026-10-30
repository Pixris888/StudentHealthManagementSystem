<template>
  <view class="ai-page">
    <!-- #ifdef APP-PLUS -->
    <SseBridge :request="sseRequest" @event="onBridgeEvent" />
    <!-- #endif -->

    <view :style="{ height: `${statusBarHeight}px` }" class="status-bar" />
    <view class="ai-header">
      <view class="grow">
        <text class="ai-title">AI 健康助手</text>
        <text class="ai-sub">流式回答 · 单条问题最多 500 字</text>
      </view>
      <view class="header-actions">
        <view class="header-btn" @click="goHistory">
          <uni-icons type="calendar" size="20" color="#475569" />
        </view>
        <view class="header-btn" @click="newConversation">
          <uni-icons type="plusempty" size="21" color="#475569" />
        </view>
      </view>
    </view>

    <view class="ai-toolbar">
      <view class="plan-link" @click="goPlans">
        <uni-icons type="star-filled" size="15" color="#0f9d8a" />
        <text>我的健康计划</text>
        <uni-icons type="arrowright" size="12" color="#0f9d8a" />
      </view>
      <view v-if="sessionId" class="session-chip">
        <view class="session-dot" />
        <text>会话进行中</text>
      </view>
    </view>

    <scroll-view
      class="chat-scroll"
      scroll-y
      :scroll-into-view="scrollIntoId"
      :scroll-with-animation="true"
      :show-scrollbar="false"
    >
      <view class="chat-inner">
        <view v-if="!messages.length" class="welcome fade-in">
          <view class="welcome-visual">
            <view class="welcome-ring" />
            <view class="welcome-icon">
              <uni-icons type="chatbubble-filled" size="30" color="#ffffff" />
            </view>
          </view>
          <text class="welcome-title">你好，我是你的健康助手</text>
          <text class="welcome-desc">
            可以问我作息、运动、饮食和情绪相关的问题。我会结合健康知识库给出参考建议。
          </text>

          <view class="quick-list">
            <view
              v-for="question in quickQuestions"
              :key="question"
              class="quick-question"
              @click="quickFill(question)"
            >
              <text class="quick-question-text">{{ question }}</text>
              <uni-icons type="arrowright" size="14" color="#9fb6ae" />
            </view>
          </view>

          <view class="disclaimer">
            <uni-icons type="info" size="14" color="#b45309" />
            <text>AI 建议不能替代医生诊断；如有紧急或严重不适，请及时就医。</text>
          </view>
        </view>

        <view
          v-for="message in messages"
          :id="`msg-${message.id}`"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <view v-if="message.role === 'assistant'" class="assistant-avatar">
            <uni-icons type="chatbubble-filled" size="17" color="#0f9d8a" />
          </view>

          <view class="bubble-wrap">
            <view class="bubble" :class="message.role">
              <view v-if="message.loading" class="typing">
                <view class="typing-dot dot-1" />
                <view class="typing-dot dot-2" />
                <view class="typing-dot dot-3" />
              </view>

              <view v-else-if="message.error && !message.content" class="error-content">
                <text class="error-text">{{ message.errorMessage }}</text>
                <button class="retry-btn" @click="retry(message)">重新发送</button>
              </view>

              <text
                v-else-if="message.stopped && !message.content"
                class="stopped-bubble-text"
              >
                已停止生成
              </text>
              <rich-text
                v-else-if="message.role === 'assistant'"
                class="markdown-body"
                :nodes="renderMarkdown(message.content)"
              />
              <text v-else class="user-text">{{ message.content }}</text>
            </view>

            <view v-if="message.error && message.content" class="stream-error-note">
              <text class="stream-error-text">{{ message.errorMessage }}</text>
              <button class="retry-btn" @click="retry(message)">重新发送</button>
            </view>

            <view v-if="message.stopped && message.content" class="stopped-tip">
              <uni-icons type="info" size="13" color="#94a3b8" />
              <text>已停止生成</text>
            </view>

            <view v-if="message.answer?.emergency" class="emergency-tip">
              <uni-icons type="info-filled" size="15" color="#dc2626" />
              <text>该问题可能涉及紧急情况，请优先联系校医或拨打急救电话。</text>
            </view>

            <view v-if="message.answer?.fallback" class="fallback-tip">
              <text>本次回答未命中知识库，仅供参考</text>
            </view>

            <view v-if="message.answer?.sources?.length" class="source-list">
              <text class="source-label">参考来源</text>
              <view class="source-chips">
                <text
                  v-for="source in message.answer.sources"
                  :key="source"
                  class="source-chip"
                >
                  {{ source }}
                </text>
              </view>
            </view>

            <view
              v-if="message.role === 'assistant' && (message.loading || message.streaming)"
              class="stream-status-row"
            >
              <text class="stream-status-text">
                {{ message.loading ? 'AI 正在思考…' : '正在生成' }}
                <text v-if="message.streaming" class="stream-cursor">▍</text>
              </text>
              <button class="stop-gen-btn" :disabled="!canStop" @click="stopStreaming">
                停止生成
              </button>
            </view>

            <view
              v-if="message.role === 'assistant' && !message.loading && !message.streaming"
              class="message-meta-row"
            >
              <text class="message-time">{{ formatTime(message.createdAt) }}</text>
              <view
                v-if="!message.error && !message.stopped && (message.answer?.consultationId || message.consultation?.id)"
                class="feedback-row"
              >
                <view
                  class="feedback-btn"
                  :class="{ active: message.feedback === 1 }"
                  @click="handleFeedback(message, true)"
                >
                  <uni-icons type="hand-up" size="13" :color="message.feedback === 1 ? '#0f9d8a' : '#a8b7b2'" />
                  <text>有帮助</text>
                </view>
                <view
                  class="feedback-btn"
                  :class="{ active: message.feedback === 2 }"
                  @click="handleFeedback(message, false)"
                >
                  <uni-icons type="hand-down" size="13" :color="message.feedback === 2 ? '#f59e0b' : '#a8b7b2'" />
                  <text>待改进</text>
                </view>
              </view>
            </view>

          </view>
        </view>

        <view id="chat-bottom" class="chat-bottom" />
      </view>
    </scroll-view>

    <view class="composer">
      <view class="composer-box">
        <textarea
          v-model="input"
          class="composer-input"
          auto-height
          :maxlength="MAX_QUESTION_LENGTH"
          :disabled="sending"
          placeholder="描述你的健康问题…"
          placeholder-class="composer-placeholder"
          :cursor-spacing="20"
          @confirm="sendQuestion"
        />
        <button
          v-if="sending && canStop"
          class="send-btn stop-send-btn"
          @click="stopStreaming"
        >
          <uni-icons type="closeempty" size="20" color="#ffffff" />
        </button>
        <button v-else class="send-btn" :disabled="!canSend" @click="sendQuestion">
          <uni-icons
            type="paperplane-filled"
            size="20"
            :color="canSend ? '#ffffff' : '#b7c9c3'"
          />
        </button>
      </view>
      <view class="composer-foot">
        <text>{{ input.length }} / {{ MAX_QUESTION_LENGTH }}</text>
        <text>AI 建议仅供参考，不能替代医生诊断</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { onShow, onUnload } from '@dcloudio/uni-app'
import { askAi, feedbackConsultation } from '@/api/ai'
// #ifdef H5
import { askAiStreamWeb } from '@/api/aiStream'
// #endif
// #ifdef APP-PLUS
import SseBridge from '@/components/SseBridge.vue'
// #endif
import { API_BASE_URL } from '@/config'
import { useAiStore } from '@/stores/ai'
import { useUserStore } from '@/stores/user'
import { getAccessToken } from '@/utils/auth'
import {
  AI_MAX_QUESTION_LENGTH,
  AI_QUICK_QUESTIONS,
  aiErrorText,
} from '@/utils/ai'
import { formatTime } from '@/utils/format'
import { renderMarkdown } from '@/utils/markdown'
import type {
  AiAnswerVO,
  AiConsultationVO,
  AiStreamError,
  AiStreamHandlers,
  AiStreamMeta,
  AiStreamResult,
  ChatMessage,
  FeedbackStatus,
  SseBridgeEvent,
  SseBridgeRequest,
} from '@/types/ai'

const MAX_QUESTION_LENGTH = AI_MAX_QUESTION_LENGTH
const quickQuestions = AI_QUICK_QUESTIONS
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight ?? 0

const userStore = useUserStore()
const aiStore = useAiStore()

const messages = ref<ChatMessage[]>([])
const sessionId = ref('')
const input = ref('')
const sending = ref(false)
const scrollIntoId = ref('')
const feedbackSubmittingId = ref<string | null>(null)
const sseRequest = ref<SseBridgeRequest | null>(null)
const currentStreamId = ref('')
const currentAbortController = ref<AbortController | null>(null)

interface AppStreamState {
  sawMeta: boolean
  sawDelta: boolean
}

interface StreamResolver {
  resolve: (result: AiStreamResult) => void
  settled: boolean
  timer?: ReturnType<typeof setTimeout>
}

const streamResolvers = new Map<string, StreamResolver>()
const appStreamStates = new Map<string, AppStreamState>()
const streamMessageIds = new Map<string, string>()
const pendingDeltas = new Map<
  string,
  { buffer: string; timer: ReturnType<typeof setTimeout> | null }
>()
let scrollTimer: ReturnType<typeof setTimeout> | null = null

const canSend = computed(() => input.value.trim().length > 0 && !sending.value)
const canStop = computed(
  () =>
    sending.value &&
    (Boolean(currentStreamId.value) || Boolean(currentAbortController.value)),
)

function createId(): string {
  return `m-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

function nowText(): string {
  const date = new Date()
  const pad = (value: number): string => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function scrollToBottom(): Promise<void> {
  scrollIntoId.value = ''
  await nextTick()
  setTimeout(() => {
    scrollIntoId.value = 'chat-bottom'
  }, 30)
}

function scheduleScrollToBottom(): void {
  if (scrollTimer) return
  scrollTimer = setTimeout(() => {
    scrollTimer = null
    scrollToBottom()
  }, 120)
}

function ensureLogin(): boolean {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

function forceLogin(message = '登录状态已失效，请重新登录'): void {
  userStore.reset()
  uni.showToast({ title: message, icon: 'none' })
  setTimeout(() => {
    uni.reLaunch({ url: '/pages/login/index' })
  }, 700)
}

function quickFill(question: string): void {
  if (sending.value) return
  input.value = question
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

function loadPendingSession(): void {
  const pending = aiStore.takePending()
  if (!pending) return
  const sorted = [...pending.records].sort((a, b) => {
    const left = a.consultTime || a.createTime || ''
    const right = b.consultTime || b.createTime || ''
    return left.localeCompare(right)
  })
  messages.value = recordsToMessages(sorted)
  sessionId.value = pending.sessionId || sorted[0]?.sessionId || ''
  scheduleScrollToBottom()
}

function getMessage(messageId: string): ChatMessage | undefined {
  return messages.value.find((item) => item.id === messageId)
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
  message.createdAt = nowText()
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
  message.createdAt = answer.consultTime || nowText()
  sessionId.value = answer.sessionId || sessionId.value
  scheduleScrollToBottom()
}

function applyStreamError(message: ChatMessage, error: AiStreamError): void {
  flushPendingDelta(message)
  message.loading = false
  message.streaming = false
  message.stopped = false
  message.error = true
  message.errorMessage = error.message || '回答失败，请稍后重试'
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

async function runSyncFallback(message: ChatMessage, question: string): Promise<void> {
  // 流式通道已经不可用，避免“停止生成”按钮误导用户
  currentAbortController.value = null
  currentStreamId.value = ''
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
      message: aiErrorText(error),
      partial: false,
    })
  }
}

function isFallbackStreamError(
  status: number,
  code: number,
  message: string,
): boolean {
  if ([404, 405, 501].includes(status) || [404, 405, 501].includes(code)) {
    return true
  }
  return /流式输出未启用|不支持流式|流式.{0,6}(未开启|未启用)|stream.{0,12}disabled/i.test(
    message,
  )
}

function parseBridgeData<T>(raw?: string): T | null {
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

function settleStream(streamId: string, result: AiStreamResult): void {
  const resolver = streamResolvers.get(streamId)
  if (!resolver || resolver.settled) return
  resolver.settled = true
  if (resolver.timer) clearTimeout(resolver.timer)
  streamResolvers.delete(streamId)
  appStreamStates.delete(streamId)
  streamMessageIds.delete(streamId)
  if (currentStreamId.value === streamId) currentStreamId.value = ''
  resolver.resolve(result)
}

function startAppStream(
  message: ChatMessage,
  question: string,
): Promise<AiStreamResult> {
  return new Promise<AiStreamResult>((resolve) => {
    const streamId = createId()
    currentStreamId.value = streamId
    streamMessageIds.set(streamId, message.id)
    appStreamStates.set(streamId, { sawMeta: false, sawDelta: false })

    const resolver: StreamResolver = { resolve, settled: false }
    resolver.timer = setTimeout(() => {
      settleStream(streamId, {
        status: 'fallback',
        code: 0,
        message: '流式通道未响应，已切换为同步回答',
      })
    }, 8000)
    streamResolvers.set(streamId, resolver)

    sseRequest.value = {
      id: streamId,
      action: 'start',
      url: `${API_BASE_URL}/ai/consultation/ask/stream`,
      token: getAccessToken(),
      body: {
        question,
        sessionId: sessionId.value || undefined,
      },
      timeout: 180000,
    }
  })
}

function onBridgeEvent(event: SseBridgeEvent): void {
  if (!event?.id) return
  const resolver = streamResolvers.get(event.id)
  if (!resolver || resolver.settled) return

  const state = appStreamStates.get(event.id)
  const messageId = streamMessageIds.get(event.id)
  const message = messageId ? getMessage(messageId) : undefined

  if (event.event === 'started') {
    if (resolver.timer) {
      clearTimeout(resolver.timer)
      resolver.timer = undefined
    }
    return
  }

  if (event.event === 'meta' && message) {
    if (state) state.sawMeta = true
    const meta = parseBridgeData<AiStreamMeta>(event.data)
    if (meta) applyMeta(message, meta)
    return
  }

  if (event.event === 'delta' && message) {
    if (state) state.sawDelta = true
    const payload = parseBridgeData<{ content?: string }>(event.data)
    if (payload?.content) queueDelta(message, payload.content)
    return
  }

  if (event.event === 'done') {
    const answer = parseBridgeData<AiAnswerVO>(event.data)
    if (answer) {
      settleStream(event.id, { status: 'done', answer })
    } else {
      settleStream(event.id, {
        status: 'error',
        error: {
          code: 500,
          message: '回答数据解析失败，请重试',
          partial: true,
        },
      })
    }
    return
  }

  if (event.event === 'error') {
    const error = parseBridgeData<AiStreamError>(event.data)
    settleStream(event.id, {
      status: 'error',
      error: error || {
        code: 500,
        message: '回答生成中断，请重试',
        partial: Boolean(state?.sawMeta || state?.sawDelta),
      },
    })
    return
  }

  if (event.event === 'http-error') {
    const code = event.code ?? 500
    const messageText = event.message || '流式请求失败'
    if (isFallbackStreamError(event.httpStatus ?? 0, code, messageText)) {
      settleStream(event.id, {
        status: 'fallback',
        code,
        message: messageText,
      })
    } else {
      settleStream(event.id, {
        status: 'error',
        error: {
          code,
          message: messageText,
          partial: Boolean(state?.sawMeta || state?.sawDelta),
        },
      })
    }
    return
  }

  if (event.event === 'network-error') {
    if (state?.sawMeta || state?.sawDelta) {
      settleStream(event.id, {
        status: 'error',
        error: {
          code: 0,
          message: event.message || '流式连接中断，已保留已生成的内容',
          partial: true,
        },
      })
    } else {
      settleStream(event.id, {
        status: 'fallback',
        code: 0,
        message: event.message || '无法建立流式连接',
      })
    }
    return
  }

  if (event.event === 'timeout') {
    settleStream(event.id, {
      status: 'error',
      error: {
        code: 408,
        message: event.message || 'AI 响应超时，请稍后重试',
        partial: Boolean(state?.sawMeta || state?.sawDelta),
      },
    })
    return
  }

  if (event.event === 'aborted') {
    settleStream(event.id, { status: 'aborted' })
    return
  }

  if (event.event === 'ended') {
    settleStream(event.id, {
      status: 'error',
      error: {
        code: 500,
        message: '连接意外结束，请重试',
        partial: Boolean(state?.sawMeta || state?.sawDelta),
      },
    })
  }
}

async function runAssistantAnswer(
  message: ChatMessage,
  question: string,
): Promise<void> {
  sending.value = true
  resetAssistantMessage(message)

  let result: AiStreamResult = {
    status: 'fallback',
    code: 0,
    message: '当前平台不支持流式回答',
  }

  try {
    // #ifdef H5
    const controller = new AbortController()
    currentAbortController.value = controller
    result = await askAiStreamWeb(
      { question, sessionId: sessionId.value || undefined },
      streamHandlers(message),
      controller.signal,
    )
    // #endif

    // #ifdef APP-PLUS
    result = await startAppStream(message, question)
    // #endif

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
      message: aiErrorText(error),
      partial: Boolean(message.content),
    })
  } finally {
    flushPendingDelta(message)
    sending.value = false
    currentAbortController.value = null
    currentStreamId.value = ''
    sseRequest.value = null
    await scrollToBottom()
  }
}

async function sendQuestion(): Promise<void> {
  if (!ensureLogin()) return
  const question = input.value.trim()
  if (!question || sending.value) return
  if (question.length > MAX_QUESTION_LENGTH) {
    uni.showToast({ title: `问题不能超过 ${MAX_QUESTION_LENGTH} 字`, icon: 'none' })
    return
  }

  messages.value.push({
    id: createId(),
    role: 'user',
    content: question,
    question,
    createdAt: nowText(),
  })
  input.value = ''

  messages.value.push({
    id: createId(),
    role: 'assistant',
    content: '',
    loading: true,
    question,
    createdAt: nowText(),
  })
  // 取回数组中的响应式代理，直接修改原始对象不会触发 Vue 更新
  const assistant = messages.value[messages.value.length - 1]!
  await scrollToBottom()
  await runAssistantAnswer(assistant, question)
}

async function retry(message: ChatMessage): Promise<void> {
  if (sending.value || !message.question) return
  if (!ensureLogin()) return
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '重新发送',
      content: '重新发送会再次请求 AI 回答，是否继续？',
      confirmText: '重新发送',
      success: (res) => resolve(Boolean(res.confirm)),
      fail: () => resolve(false),
    })
  })
  if (!confirmed) return
  await runAssistantAnswer(message, message.question)
}

function stopStreaming(): void {
  if (!sending.value) return

  // #ifdef H5
  if (currentAbortController.value) {
    currentAbortController.value.abort()
    return
  }
  // #endif

  // #ifdef APP-PLUS
  if (currentStreamId.value) {
    const streamId = currentStreamId.value
    sseRequest.value = { id: streamId, action: 'abort' }
    const resolver = streamResolvers.get(streamId)
    if (resolver?.timer) {
      clearTimeout(resolver.timer)
      resolver.timer = undefined
    }
    setTimeout(() => {
      settleStream(streamId, { status: 'aborted' })
    }, 1200)
  }
  // #endif
}

async function handleFeedback(message: ChatMessage, helpful: boolean): Promise<void> {
  if (feedbackSubmittingId.value) return
  const id = message.answer?.consultationId ?? message.consultation?.id
  if (!id) return
  const target: FeedbackStatus = helpful ? 1 : 2
  const current = message.feedback ?? message.consultation?.feedback ?? 0
  if (current === target) return

  feedbackSubmittingId.value = message.id
  try {
    const updated = await feedbackConsultation(id, { helpful })
    message.feedback = updated.feedback ?? target
    if (message.consultation) message.consultation.feedback = message.feedback
    uni.showToast({ title: '感谢反馈', icon: 'success' })
  } catch {
    // 请求层已提示
  } finally {
    feedbackSubmittingId.value = null
  }
}

function newConversation(): void {
  if (sending.value) return
  if (!messages.value.length) {
    input.value = ''
    return
  }
  uni.showModal({
    title: '新建对话',
    content: '会清空当前页面中的对话内容，历史记录仍会保留。是否继续？',
    confirmText: '新建',
    success: (res) => {
      if (!res.confirm) return
      messages.value = []
      sessionId.value = ''
      input.value = ''
    },
  })
}

function goHistory(): void {
  if (sending.value) {
    uni.showToast({ title: 'AI 正在回答，请稍候再查看历史', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/ai/history' })
}

function goPlans(): void {
  uni.navigateTo({ url: '/pages/ai/plans' })
}

onShow(async () => {
  if (!ensureLogin()) return
  if (!userStore.userInfo) {
    try {
      await userStore.bootstrap()
    } catch {
      uni.reLaunch({ url: '/pages/login/index' })
      return
    }
  }
  if (sending.value) return
  loadPendingSession()
})

onUnload(() => {
  // #ifdef H5
  currentAbortController.value?.abort()
  // #endif
  // #ifdef APP-PLUS
  if (currentStreamId.value) {
    sseRequest.value = { id: currentStreamId.value, action: 'abort' }
  }
  // #endif
})
</script>

<style scoped>
.ai-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  background: #f4f7f6;
}

/* #ifdef H5 */
.ai-page {
  bottom: var(--window-bottom, 50px);
}
/* #endif */

.status-bar {
  flex-shrink: 0;
  background: #ffffff;
}

.ai-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 28rpx 20rpx;
  background: #ffffff;
  border-bottom: 1rpx solid #eef4f2;
}

.ai-title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #0f172a;
}

.ai-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 20rpx;
  color: #94a3b8;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.header-btn {
  width: 68rpx;
  height: 68rpx;
  border-radius: 20rpx;
  background: #f4f8f6;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 24rpx;
  background: #f4f7f6;
}

.plan-link {
  display: flex;
  align-items: center;
  gap: 6rpx;
  height: 54rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #e6f7f3;
  color: #0f766e;
  font-size: 22rpx;
  font-weight: 600;
}

.session-chip {
  display: flex;
  align-items: center;
  gap: 8rpx;
  height: 48rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #7b9a92;
  font-size: 20rpx;
}

.session-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #10b981;
}

.chat-scroll {
  flex: 1;
  height: 0;
  min-height: 0;
}

.chat-inner {
  padding: 22rpx 24rpx 20rpx;
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 18rpx 20rpx;
  text-align: center;
}

.welcome-visual {
  position: relative;
  width: 150rpx;
  height: 150rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-ring {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 50%;
  background: rgba(15, 157, 138, 0.08);
}

.welcome-icon {
  width: 104rpx;
  height: 104rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, #12b39c, #0b7f70);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 16rpx 34rpx rgba(15, 157, 138, 0.22);
}

.welcome-title {
  margin-top: 26rpx;
  font-size: 34rpx;
  font-weight: 700;
  color: #0f172a;
}

.welcome-desc {
  margin-top: 12rpx;
  max-width: 560rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #7b8f89;
}

.quick-list {
  width: 100%;
  margin-top: 30rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.quick-question {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 22rpx 24rpx;
  border-radius: 22rpx;
  background: #ffffff;
  border: 1rpx solid #edf4f1;
  box-shadow: 0 6rpx 18rpx rgba(15, 118, 110, 0.04);
  text-align: left;
}

.quick-question-text {
  flex: 1;
  font-size: 25rpx;
  line-height: 1.5;
  color: #334155;
}

.disclaimer {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  margin-top: 30rpx;
  padding: 18rpx 20rpx;
  border-radius: 16rpx;
  background: #fff8ea;
  font-size: 20rpx;
  line-height: 1.6;
  color: #a16207;
  text-align: left;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  margin-bottom: 26rpx;
}

.message-row.user {
  justify-content: flex-end;
}

.assistant-avatar {
  flex-shrink: 0;
  width: 62rpx;
  height: 62rpx;
  border-radius: 20rpx;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 18rpx rgba(15, 118, 110, 0.08);
}

.bubble-wrap {
  max-width: 78%;
  display: flex;
  flex-direction: column;
}

.message-row.user .bubble-wrap {
  align-items: flex-end;
}

.bubble {
  padding: 20rpx 24rpx;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 22rpx rgba(15, 118, 110, 0.06);
  word-break: break-word;
}

.bubble.assistant {
  border-top-left-radius: 8rpx;
}

.bubble.user {
  border-top-right-radius: 8rpx;
  background: linear-gradient(135deg, #12b39c, #0c8374);
  box-shadow: 0 10rpx 24rpx rgba(15, 157, 138, 0.2);
}

.user-text {
  font-size: 27rpx;
  line-height: 1.65;
  color: #ffffff;
  white-space: pre-wrap;
}

.markdown-body {
  font-size: 27rpx;
  line-height: 1.7;
  color: #334155;
}

.typing {
  display: flex;
  align-items: center;
  gap: 8rpx;
  height: 40rpx;
  padding: 0 4rpx;
}

.typing-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #9fd9cd;
  animation: typing 1.2s infinite ease-in-out;
}

.dot-2 {
  animation-delay: 0.15s;
}

.dot-3 {
  animation-delay: 0.3s;
}

@keyframes typing {
  0%,
  60%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-5rpx);
  }
}

.stream-cursor {
  display: inline-block;
  margin-left: 2rpx;
  font-size: 20rpx;
  line-height: 1;
  color: #0f9d8a;
  animation: stream-blink 0.9s steps(2, start) infinite;
}

.stopped-bubble-text {
  font-size: 24rpx;
  color: #94a3b8;
}

@keyframes stream-blink {
  50% {
    opacity: 0;
  }
}

.stream-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
  margin-top: 8rpx;
  padding: 0 4rpx;
}

.stream-status-text {
  font-size: 20rpx;
  color: #0f9d8a;
}

.stop-gen-btn {
  height: 48rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #fff1f0;
  color: #dc2626;
  font-size: 20rpx;
  line-height: 48rpx;
  border: none;
}

.stop-gen-btn[disabled] {
  opacity: 0.45;
}

.stop-gen-btn::after {
  border: none;
}

.stream-error-note {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10rpx;
  margin-top: 10rpx;
  padding: 14rpx 18rpx;
  border-radius: 16rpx;
  background: #fef2f2;
}

.stream-error-text {
  font-size: 21rpx;
  line-height: 1.6;
  color: #b91c1c;
}

.stream-error-note .retry-btn {
  height: 48rpx;
  line-height: 48rpx;
  font-size: 20rpx;
}

.stopped-tip {
  display: flex;
  align-items: center;
  gap: 6rpx;
  margin-top: 8rpx;
  padding: 0 4rpx;
  font-size: 20rpx;
  color: #94a3b8;
}

.error-content {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.error-text {
  font-size: 24rpx;
  line-height: 1.6;
  color: #b91c1c;
}

.retry-btn {
  align-self: flex-start;
  height: 56rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #fef2f2;
  color: #dc2626;
  font-size: 22rpx;
  line-height: 56rpx;
}

.retry-btn::after {
  border: none;
}

.message-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 10rpx;
  padding: 0 4rpx;
}

.message-time {
  font-size: 19rpx;
  color: #b1bfba;
}

.feedback-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.feedback-btn {
  display: flex;
  align-items: center;
  gap: 4rpx;
  height: 44rpx;
  padding: 0 12rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #a8b7b2;
  font-size: 19rpx;
}

.feedback-btn.active {
  background: #e6f7f3;
  color: #0f9d8a;
}

.emergency-tip {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  margin-top: 12rpx;
  padding: 16rpx 18rpx;
  border-radius: 14rpx;
  background: #fef2f2;
  font-size: 21rpx;
  line-height: 1.6;
  color: #b91c1c;
}

.fallback-tip {
  margin-top: 10rpx;
  padding: 10rpx 14rpx;
  border-radius: 12rpx;
  background: #fff8ea;
  font-size: 19rpx;
  color: #a16207;
}

.source-list {
  margin-top: 12rpx;
  padding: 14rpx 16rpx;
  border-radius: 14rpx;
  background: #f4f8f6;
}

.source-label {
  display: block;
  font-size: 19rpx;
  color: #94a3b8;
}

.source-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 8rpx;
}

.source-chip {
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #417a70;
  font-size: 19rpx;
}

.chat-bottom {
  height: 16rpx;
}

.composer {
  flex-shrink: 0;
  padding: 14rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  border-top: 1rpx solid #eef4f2;
}

.composer-box {
  display: flex;
  align-items: flex-end;
  gap: 14rpx;
  padding: 12rpx 12rpx 12rpx 22rpx;
  border-radius: 26rpx;
  background: #f5f9f7;
  border: 2rpx solid #e6efeb;
}

.composer-input {
  flex: 1;
  min-height: 52rpx;
  max-height: 190rpx;
  padding: 10rpx 0;
  font-size: 27rpx;
  line-height: 1.55;
  color: #0f172a;
}

.composer-placeholder {
  color: #aabbb5;
}

.send-btn {
  flex-shrink: 0;
  width: 68rpx;
  height: 68rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #12b39c, #0b7f70);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.send-btn[disabled] {
  background: #eef3f1;
}

.stop-send-btn {
  background: linear-gradient(135deg, #f59e0b, #dc2626);
}

.send-btn::after {
  border: none;
}

.composer-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8rpx;
  padding: 0 6rpx;
  font-size: 18rpx;
  color: #b1bfba;
}
</style>
