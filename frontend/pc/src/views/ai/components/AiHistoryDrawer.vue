<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  ArrowUp,
  ChatLineRound,
  Delete,
  Refresh,
} from '@element-plus/icons-vue'
import {
  deleteMyConsultations,
  getSessionMessages,
  pageMyConsultations,
} from '@/api/ai'
import { useIsMobile } from '@/utils/useIsMobile'
import { feedbackLabel, feedbackTagType } from '@/utils/ai'
import { formatDateTime } from '@/utils/format'
import type { AiConsultationVO } from '@/types/ai'
import AiStatusTag from './AiStatusTag.vue'

interface SessionGroup {
  key: string
  sessionId: string
  title: string
  lastTime?: string
  records: AiConsultationVO[]
}

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    /** AI 正在回答时禁止切换会话，避免丢失当前请求结果 */
    disabled?: boolean
  }>(),
  {
    disabled: false,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', sessionId: string, records: AiConsultationVO[]): void
}>()

const isMobile = useIsMobile()
const drawerSize = computed(() => (isMobile.value ? '94%' : '480px'))

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const records = ref<AiConsultationVO[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 20 })
const expandedKeys = ref<string[]>([])
const sessionLoadingKey = ref('')

const sessions = computed<SessionGroup[]>(() => {
  const map = new Map<string, AiConsultationVO[]>()
  for (const item of records.value) {
    const key = item.sessionId || `single-${item.id}`
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(item)
  }
  return Array.from(map.entries())
    .map(([key, list]) => {
      const sorted = [...list].sort((a, b) =>
        (b.consultTime ?? '').localeCompare(a.consultTime ?? ''),
      )
      return {
        key,
        sessionId: sorted[0]?.sessionId || '',
        title: sorted[0]?.question || '未命名会话',
        lastTime: sorted[0]?.consultTime,
        records: sorted,
      }
    })
    .sort((a, b) => (b.lastTime ?? '').localeCompare(a.lastTime ?? ''))
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await pageMyConsultations({
      page: query.page,
      size: query.size,
    })
    records.value = page.records
    total.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

function isExpanded(key: string): boolean {
  return expandedKeys.value.includes(key)
}

function toggle(key: string): void {
  if (isExpanded(key)) {
    expandedKeys.value = expandedKeys.value.filter((item) => item !== key)
  } else {
    expandedKeys.value = [...expandedKeys.value, key]
  }
}

async function openSession(session: SessionGroup): Promise<void> {
  if (props.disabled) {
    ElMessage.warning('AI 正在回答，请稍候再切换会话')
    return
  }
  if (!session.sessionId) {
    emit('select', '', session.records)
    emit('update:modelValue', false)
    return
  }
  sessionLoadingKey.value = session.key
  try {
    const list = await getSessionMessages(session.sessionId)
    emit('select', session.sessionId, list)
    emit('update:modelValue', false)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    sessionLoadingKey.value = ''
  }
}

async function handleDelete(record: AiConsultationVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除这条问答记录吗？删除单条记录可能使会话内容不完整，且删除后不可恢复。`,
      '删除问答记录',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }
  try {
    await deleteMyConsultations([record.id])
    ElMessage.success('记录已删除')
    if (records.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    await load()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

function handlePageChange(page: number): void {
  query.page = page
  expandedKeys.value = []
  load()
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    query.page = 1
    expandedKeys.value = []
    load()
  },
)
</script>

<template>
  <el-drawer
    v-model="visible"
    title="历史记录"
    :size="drawerSize"
    append-to-body
    destroy-on-close
  >
    <div class="history-body">
      <div class="history-head">
        <div>
          <strong>我的问答历史</strong>
          <p>按会话分组（基于当前页记录），点击会话可加载完整对话。</p>
        </div>
        <el-button
          :icon="Refresh"
          circle
          :loading="loading"
          title="刷新"
          @click="load"
        />
      </div>

      <div v-loading="loading" class="history-list">
        <template v-if="sessions.length">
          <div
            v-for="session in sessions"
            :key="session.key"
            class="session-card"
            :class="{ expanded: isExpanded(session.key) }"
          >
            <div class="session-main">
              <button
                type="button"
                class="session-open"
                :disabled="disabled || sessionLoadingKey === session.key"
                @click="openSession(session)"
              >
                <span class="session-icon">
                  <el-icon><ChatLineRound /></el-icon>
                </span>
                <span class="session-text">
                  <strong>{{ session.title }}</strong>
                  <small>
                    {{ formatDateTime(session.lastTime) }} ·
                    {{ session.records.length }} 条问答
                  </small>
                </span>
              </button>

              <div class="session-actions">
                <el-button
                  text
                  circle
                  size="small"
                  :icon="isExpanded(session.key) ? ArrowUp : ArrowDown"
                  :title="isExpanded(session.key) ? '收起记录' : '展开记录'"
                  @click="toggle(session.key)"
                />
                <el-button
                  text
                  type="primary"
                  size="small"
                  :loading="sessionLoadingKey === session.key"
                  :disabled="disabled"
                  @click="openSession(session)"
                >
                  打开
                </el-button>
              </div>
            </div>

            <div v-if="isExpanded(session.key)" class="session-records">
              <div
                v-for="record in session.records"
                :key="record.id"
                class="record-row"
              >
                <div class="record-main">
                  <div class="record-meta">
                    <span>{{ formatDateTime(record.consultTime) }}</span>
                    <AiStatusTag
                      :status="record.answerStatus"
                      effect="plain"
                    />
                    <el-tag
                      :type="feedbackTagType(record.feedback)"
                      effect="plain"
                      size="small"
                    >
                      {{ feedbackLabel(record.feedback) }}
                    </el-tag>
                  </div>
                  <p class="record-question">{{ record.question }}</p>
                </div>
                <el-button
                  link
                  type="danger"
                  :icon="Delete"
                  @click="handleDelete(record)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </template>

        <el-empty
          v-else-if="!loading"
          description="暂无历史问答，去问第一个健康问题吧"
          :image-size="90"
        />
      </div>

      <div v-if="total > query.size" class="history-footer">
        <el-pagination
          :current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          background
          small
          @current-change="handlePageChange"
        />
      </div>

      <p class="history-tip">
        系统暂不支持删除整个会话；删除单条记录后，会话内容可能不完整。
      </p>
    </div>
  </el-drawer>
</template>

<style scoped>
.history-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 100%;
}

.history-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.history-head strong {
  font-size: 14px;
  color: var(--hp-text);
}

.history-head p {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--hp-text-muted);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 160px;
}

.session-card {
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--hp-border);
  border-radius: 12px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.session-card:hover,
.session-card.expanded {
  border-color: #c8e6da;
  box-shadow: 0 6px 18px rgba(31, 157, 122, 0.06);
}

.session-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 10px 10px 12px;
}

.session-open {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 0;
  text-align: left;
  cursor: pointer;
  background: none;
  border: none;
}

.session-open:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.session-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--hp-primary);
  background: #eaf7f2;
  border-radius: 10px;
}

.session-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.session-text strong {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  color: var(--hp-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-text small {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.session-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 2px;
}

.session-records {
  padding: 2px 12px 10px;
  border-top: 1px dashed #e6efeb;
}

.record-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 0;
  border-bottom: 1px dashed #eef4f1;
}

.record-row:last-child {
  border-bottom: none;
}

.record-main {
  flex: 1;
  min-width: 0;
}

.record-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 11px;
  color: var(--hp-text-muted);
}

.record-question {
  overflow: hidden;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--hp-text-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-footer {
  display: flex;
  justify-content: center;
}

.history-tip {
  margin-top: auto;
  padding: 10px 12px;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--hp-text-muted);
  background: #f7faf9;
  border-radius: 8px;
}

@media (max-width: 520px) {
  .session-actions .el-button:first-child {
    display: none;
  }
}
</style>
