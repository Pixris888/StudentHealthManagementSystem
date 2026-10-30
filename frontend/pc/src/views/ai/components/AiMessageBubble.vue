<script setup lang="ts">
import { computed } from 'vue'
import {
  MagicStick,
  RefreshRight,
  UserFilled,
} from '@element-plus/icons-vue'
import MarkdownView from '@/components/markdown/MarkdownView.vue'
import AiFeedbackButtons from './AiFeedbackButtons.vue'
import AiSourceList from './AiSourceList.vue'
import AiStatusTag from './AiStatusTag.vue'
import {
  fallbackLabel,
  formatLatency,
  providerLabel,
} from '@/utils/ai'
import { formatDateTime } from '@/utils/format'
import type { ChatMessage } from '@/types/ai'

const props = withDefaults(
  defineProps<{
    message: ChatMessage
    feedbackSubmitting?: boolean
  }>(),
  {
    feedbackSubmitting: false,
  },
)

const emit = defineEmits<{
  (e: 'retry', message: ChatMessage): void
  (e: 'feedback', message: ChatMessage, helpful: boolean): void
}>()

const isAssistant = computed(() => props.message.role === 'assistant')
const isUser = computed(() => props.message.role === 'user')

const answer = computed(() => props.message.answer)
const consultation = computed(() => props.message.consultation)

const content = computed(() => {
  const fromAnswer = answer.value?.answer?.trim()
    ? answer.value.answer
    : ''
  const fromConsultation = consultation.value?.answer?.trim()
    ? consultation.value.answer
    : ''
  return fromAnswer || fromConsultation || props.message.content || ''
})

const hasContent = computed(() => Boolean(content.value.trim()))
const status = computed(
  () => answer.value?.answerStatus ?? consultation.value?.answerStatus,
)
const failed = computed(
  () =>
    Boolean(props.message.error) ||
    (status.value === 2 && !hasContent.value),
)
const errorText = computed(
  () =>
    props.message.errorMessage ||
    consultation.value?.errorMsg ||
    '回答失败，请稍后重试。',
)

const emergency = computed(
  () =>
    answer.value?.emergency ??
    Boolean(consultation.value?.emergency),
)

const fallback = computed(
  () => answer.value?.fallback ?? consultation.value?.fallback ?? false,
)
const provider = computed(
  () => answer.value?.provider ?? consultation.value?.provider,
)
const modelName = computed(
  () => answer.value?.modelName ?? consultation.value?.modelName,
)

const showFallback = computed(
  () => fallback.value || status.value === 3,
)

const modelText = computed(() => {
  const providerText = providerLabel(provider.value)
  const parts = [providerText, modelName.value].filter(Boolean)
  return parts.join(' · ')
})

const latencyText = computed(() => {
  const latency = consultation.value?.latencyMs
  return latency == null ? '' : `耗时 ${formatLatency(latency)}`
})

const sources = computed(() => answer.value?.sources)
const knowledgeSource = computed(
  () =>
    answer.value?.knowledgeSource ??
    consultation.value?.knowledgeSource ??
    '',
)

const consultationId = computed(
  () => answer.value?.consultationId ?? consultation.value?.id,
)
const feedback = computed(
  () => props.message.feedback ?? consultation.value?.feedback ?? 0,
)
const showFeedback = computed(
  () =>
    isAssistant.value &&
    Boolean(consultationId.value) &&
    hasContent.value &&
    !props.message.loading &&
    !props.message.streaming &&
    !props.message.stopped &&
    !failed.value,
)

const canRetry = computed(
  () =>
    Boolean(props.message.error) &&
    Boolean(props.message.question) &&
    Boolean(props.message.question?.trim()),
)

const timeText = computed(() => formatDateTime(props.message.createdAt))

function handleFeedback(helpful: boolean): void {
  emit('feedback', props.message, helpful)
}
</script>

<template>
  <div class="message-row" :class="{ 'is-user': isUser, 'is-assistant': isAssistant }">
    <div v-if="isAssistant" class="msg-avatar assistant-avatar">
      <el-icon><MagicStick /></el-icon>
    </div>

    <div class="message-main">
      <div class="bubble" :class="{ 'user-bubble': isUser, 'assistant-bubble': isAssistant }">
        <template v-if="isUser">
          <p class="user-text">{{ message.content }}</p>
        </template>

        <template v-else>
          <div v-if="message.loading" class="thinking">
            <span class="thinking-dots">
              <i />
              <i />
              <i />
            </span>
            <span>AI 正在思考，请稍候……</span>
          </div>

          <template v-else>
            <el-alert
              v-if="emergency"
              type="error"
              :closable="false"
              show-icon
              title="紧急健康提示"
              description="如出现胸痛、呼吸困难、意识模糊或自伤风险等紧急情况，请立即联系校医院或拨打 120，不要只依赖 AI 建议。"
              class="emergency-alert"
            />

            <div
              v-if="showFallback || (status === 0 && !message.error && !message.stopped)"
              class="bubble-tags"
            >
              <el-tooltip
                v-if="showFallback"
                content="主模型暂不可用，已使用备用方案，建议仅供参考"
                placement="top"
              >
                <el-tag type="warning" effect="light" size="small">
                  {{ fallbackLabel(provider) }}
                </el-tag>
              </el-tooltip>
              <AiStatusTag
                v-if="status === 0 && !message.error && !message.stopped"
                :status="status"
                effect="plain"
              />
            </div>

            <el-alert
              v-if="status === 2 && !message.error"
              type="error"
              :closable="false"
              show-icon
              :title="consultation?.errorMsg || '该条 AI 回答失败'"
              class="error-alert"
            />

            <MarkdownView v-if="hasContent" :content="content" />

            <div v-if="message.streaming" class="streaming-line">
              <span class="stream-cursor">▍</span>
              <span class="streaming-text">
                {{ hasContent ? '正在生成…' : '正在生成，请稍候…' }}
              </span>
            </div>

            <div v-if="message.stopped" class="stopped-line">
              <span class="stopped-dot" />
              <span>已停止生成</span>
            </div>

            <el-alert
              v-if="failed"
              type="error"
              :closable="false"
              show-icon
              :title="errorText"
              class="error-alert"
            />

            <div v-if="canRetry" class="retry-row">
              <el-button
                size="small"
                type="primary"
                plain
                :icon="RefreshRight"
                @click="emit('retry', message)"
              >
                重新发送
              </el-button>
            </div>

            <AiSourceList
              :sources="sources"
              :knowledge-source="knowledgeSource"
            />
          </template>
        </template>
      </div>

      <div class="message-footer" :class="{ 'footer-user': isUser }">
        <div class="footer-meta">
          <span>{{ timeText }}</span>
          <span v-if="isAssistant && modelText" class="model-text">
            {{ modelText }}
          </span>
          <span v-if="isAssistant && latencyText">{{ latencyText }}</span>
        </div>
        <AiFeedbackButtons
          v-if="showFeedback"
          :feedback="feedback"
          :disabled="feedbackSubmitting"
          @feedback="handleFeedback"
        />
      </div>
    </div>

    <div v-if="isUser" class="msg-avatar user-avatar">
      <el-icon><UserFilled /></el-icon>
    </div>
  </div>
</template>

<style scoped>
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 22px;
}

.message-row.is-user {
  flex-direction: row-reverse;
}

.msg-avatar {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 34px;
  height: 34px;
  color: #fff;
  border-radius: 11px;
  box-shadow: 0 4px 12px rgba(31, 157, 122, 0.18);
}

.assistant-avatar {
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
}

.user-avatar {
  background: linear-gradient(135deg, #5b8def, #8fb2ff);
  box-shadow: 0 4px 12px rgba(91, 141, 239, 0.18);
}

.message-main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  max-width: min(82%, 780px);
  min-width: 0;
}

.is-user .message-main {
  align-items: flex-end;
}

.bubble {
  max-width: 100%;
  padding: 12px 16px;
  border-radius: 14px;
  word-break: break-word;
}

.assistant-bubble {
  background: #f6faf8;
  border: 1px solid #e4efe9;
  border-top-left-radius: 5px;
}

.user-bubble {
  color: #fff;
  background: linear-gradient(135deg, #1f9d7a, #2eb98c);
  border-top-right-radius: 5px;
  box-shadow: 0 6px 18px rgba(31, 157, 122, 0.18);
}

.user-text {
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.thinking {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 0;
  font-size: 13px;
  color: var(--hp-text-secondary);
}

.thinking-dots {
  display: inline-flex;
  gap: 4px;
}

.thinking-dots i {
  width: 6px;
  height: 6px;
  background: var(--hp-primary);
  border-radius: 50%;
  opacity: 0.35;
  animation: thinking-pulse 1.1s infinite ease-in-out;
}

.thinking-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.thinking-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes thinking-pulse {
  0%,
  70%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  35% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.bubble-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.emergency-alert,
.error-alert {
  margin-bottom: 10px;
}

.retry-row {
  margin-top: 10px;
}

.streaming-line {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--hp-primary);
}

.stream-cursor {
  display: inline-block;
  line-height: 1;
  animation: stream-blink 0.9s steps(2, start) infinite;
}

.streaming-text {
  color: var(--hp-text-secondary);
}

.stopped-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.stopped-dot {
  width: 6px;
  height: 6px;
  background: #c0ccc7;
  border-radius: 50%;
}

@keyframes stream-blink {
  50% {
    opacity: 0;
  }
}

.message-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  width: 100%;
  padding: 0 2px;
}

.message-footer.footer-user {
  justify-content: flex-end;
}

.footer-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  color: var(--hp-text-muted);
}

.model-text {
  padding: 1px 7px;
  background: #f2f7f5;
  border-radius: 999px;
}

@media (max-width: 768px) {
  .message-main {
    max-width: 88%;
  }

  .bubble {
    padding: 11px 13px;
  }

  .message-footer {
    align-items: flex-start;
  }
}
</style>
