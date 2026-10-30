<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import MarkdownView from '@/components/markdown/MarkdownView.vue'
import { getAdminConsultation } from '@/api/ai'
import { useIsMobile } from '@/utils/useIsMobile'
import {
  feedbackLabel,
  feedbackTagType,
  formatLatency,
  providerLabel,
  resolveAiError,
} from '@/utils/ai'
import { formatDateTime } from '@/utils/format'
import type { AiConsultationVO } from '@/types/ai'
import AiSourceList from './AiSourceList.vue'
import AiStatusTag from './AiStatusTag.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    consultationId?: number | null
  }>(),
  {
    consultationId: null,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const isMobile = useIsMobile()
const drawerSize = computed(() => (isMobile.value ? '94%' : '720px'))

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const detail = ref<AiConsultationVO | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const modelText = computed(() => {
  if (!detail.value) return '—'
  const parts = [
    providerLabel(detail.value.provider),
    detail.value.modelName,
  ].filter(Boolean)
  return parts.length ? parts.join(' · ') : '—'
})

const studentText = computed(() => {
  if (!detail.value) return '—'
  const name = detail.value.realName?.trim()
  const no = detail.value.studentNo?.trim()
  if (name && no) return `${name} · ${no}`
  if (name) return name
  if (no) return no
  if (detail.value.userId) return `用户 #${detail.value.userId}`
  return '—'
})

const tokenText = computed(() => {
  if (!detail.value) return '—'
  return `${detail.value.promptTokens ?? 0} / ${detail.value.completionTokens ?? 0}`
})

const errorAlertType = computed<'warning' | 'error'>(() =>
  detail.value?.answerStatus === 3 ? 'warning' : 'error',
)

const errorAlertTitle = computed(() =>
  detail.value?.answerStatus === 3 ? '降级原因' : '错误信息',
)

async function loadDetail(): Promise<void> {
  if (!props.consultationId) {
    detail.value = null
    return
  }
  loading.value = true
  errorMessage.value = ''
  detail.value = null
  try {
    detail.value = await getAdminConsultation(props.consultationId)
  } catch (error) {
    errorMessage.value = resolveAiError(error, '问答详情加载失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.consultationId] as const,
  ([open]) => {
    if (open) loadDetail()
  },
)
</script>

<template>
  <el-drawer
    v-model="visible"
    title="AI 问答详情"
    :size="drawerSize"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading" class="consult-detail-body">
      <div v-if="errorMessage" class="detail-error">
        <el-result
          icon="warning"
          title="详情无法查看"
          :sub-title="errorMessage"
        >
          <template #extra>
            <el-button type="primary" @click="loadDetail">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <template v-else-if="detail">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="隐私提示"
          description="问答内容包含学生健康信息，请仅用于校园健康服务，不要外传。"
          class="privacy-alert"
        />

        <div class="detail-tags">
          <AiStatusTag :status="detail.answerStatus" />
          <el-tag
            :type="feedbackTagType(detail.feedback)"
            effect="plain"
          >
            {{ feedbackLabel(detail.feedback) }}
          </el-tag>
          <el-tag v-if="detail.emergency === 1" type="danger" effect="dark">
            紧急问题
          </el-tag>
          <el-tag v-if="detail.fallback" type="warning" effect="light">
            使用备用方案
          </el-tag>
          <el-tag v-if="detail.sessionId" type="info" effect="plain">
            会话 {{ detail.sessionId.slice(0, 8) }}…
          </el-tag>
        </div>

        <el-alert
          v-if="detail.emergency === 1"
          type="error"
          :closable="false"
          show-icon
          title="该问题被识别为紧急健康问题"
          description="系统已返回安全指引。请确认学生是否已获得必要的线下医疗帮助。"
          class="emergency-alert"
        />

        <section class="question-section">
          <span class="section-eyebrow">STUDENT QUESTION</span>
          <h3>{{ detail.question }}</h3>
          <p class="question-meta">
            {{ studentText }} · {{ formatDateTime(detail.consultTime) }}
          </p>
        </section>

        <el-descriptions
          :column="isMobile ? 1 : 2"
          border
          class="detail-meta"
        >
          <el-descriptions-item label="学生">
            {{ studentText }}
          </el-descriptions-item>
          <el-descriptions-item label="提问时间">
            {{ formatDateTime(detail.consultTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="模型">
            {{ modelText }}
          </el-descriptions-item>
          <el-descriptions-item label="耗时">
            {{ formatLatency(detail.latencyMs) }}
          </el-descriptions-item>
          <el-descriptions-item label="Token">
            {{ tokenText }}
          </el-descriptions-item>
          <el-descriptions-item label="反馈">
            <span>{{ feedbackLabel(detail.feedback) }}</span>
            <span
              v-if="detail.feedbackRemark"
              class="feedback-remark"
            >
              · {{ detail.feedbackRemark }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="命中素材 ID">
            {{ detail.retrievedIds || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="记录 ID">
            {{ detail.id }}
          </el-descriptions-item>
          <el-descriptions-item label="会话 ID">
            {{ detail.sessionId || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(detail.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="detail.errorMsg"
          :type="errorAlertType"
          :closable="false"
          show-icon
          :title="errorAlertTitle"
          :description="detail.errorMsg"
          class="detail-error-alert"
        />

        <section class="answer-section">
          <div class="section-head">
            <h4>AI 回答</h4>
            <AiStatusTag
              :status="detail.answerStatus"
              effect="plain"
            />
          </div>
          <div class="answer-body">
            <MarkdownView
              v-if="detail.answer"
              :content="detail.answer"
            />
            <el-empty
              v-else
              description="该条记录暂无回答内容"
              :image-size="80"
            />
          </div>

          <AiSourceList
            :knowledge-source="detail.knowledgeSource"
            title="知识来源"
          />
        </section>
      </template>
    </div>

    <template v-if="detail && !errorMessage" #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.consult-detail-body {
  min-height: 240px;
}

.privacy-alert,
.emergency-alert,
.detail-error-alert {
  margin-bottom: 14px;
}

.detail-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.question-section {
  padding: 16px 18px;
  margin-bottom: 18px;
  background:
    linear-gradient(135deg, #f7fbf9, #fbfdfc);
  border: 1px solid #e4efe9;
  border-radius: 12px;
}

.section-eyebrow {
  display: block;
  margin-bottom: 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #9cb6ac;
}

.question-section h3 {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--hp-text);
  word-break: break-word;
}

.question-meta {
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.detail-meta {
  margin-bottom: 18px;
}

.detail-meta :deep(.el-descriptions__label) {
  width: 102px;
  color: var(--hp-text-secondary);
}

.feedback-remark {
  margin-left: 4px;
  color: var(--hp-text-muted);
}

.answer-section {
  padding: 18px 20px;
  background: #fff;
  border: 1px solid var(--hp-border);
  border-radius: 12px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.section-head h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.answer-body {
  padding: 4px 0;
}

.detail-error {
  padding-top: 30px;
}

@media (max-width: 520px) {
  .answer-section {
    padding: 14px;
  }
}
</style>
