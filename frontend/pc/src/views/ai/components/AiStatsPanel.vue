<script setup lang="ts">
import { computed, onMounted, ref, watch, type Component } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell,
  ChatDotRound,
  QuestionFilled,
  Refresh,
  Star,
  Sunny,
  Timer,
  Warning,
} from '@element-plus/icons-vue'
import { getAiStats, listUnanswered } from '@/api/ai'
import { useUserStore } from '@/stores/user'
import {
  feedbackLabel,
  feedbackTagType,
  formatLatency,
  formatRate,
} from '@/utils/ai'
import { formatDateTime } from '@/utils/format'
import type { AiConsultationVO, AiStatsVO } from '@/types/ai'
import AiConsultationDetailDrawer from './AiConsultationDetailDrawer.vue'
import AiStatusTag from './AiStatusTag.vue'

interface KpiCard {
  key: string
  label: string
  value: string
  hint: string
  icon: Component
  tone: string
  progress?: number
  progressColor?: string
}

const router = useRouter()
const userStore = useUserStore()
const canManage = computed(() => userStore.hasPermission('ai:consult:manage'))

const days = ref(7)
const stats = ref<AiStatsVO | null>(null)
const loading = ref(false)
const updatedAt = ref('')

const unanswered = ref<AiConsultationVO[]>([])
const unansweredLoading = ref(false)
const detailVisible = ref(false)
const detailId = ref<number | null>(null)

function clampRate(value?: number | null): number {
  const rate = Number(value ?? 0)
  if (Number.isNaN(rate)) return 0
  return Math.max(0, Math.min(100, rate))
}

const cards = computed<KpiCard[]>(() => {
  const source = stats.value
  return [
    {
      key: 'total',
      label: '总提问数',
      value: source ? String(source.total) : '—',
      hint: source ? `最近 ${source.days} 天累计` : '最近 N 天累计',
      icon: ChatDotRound,
      tone: 'green',
    },
    {
      key: 'today',
      label: '今日提问',
      value: source ? String(source.todayCount) : '—',
      hint: '统计周期内的今天',
      icon: Sunny,
      tone: 'blue',
    },
    {
      key: 'fallbackRate',
      label: '降级率',
      value: source ? formatRate(source.fallbackRate) : '—',
      hint: source
        ? `降级 ${source.fallbackCount} 次 / 共 ${source.total} 次`
        : '使用备用方案的比例',
      icon: Warning,
      tone: 'amber',
      progress: source ? clampRate(source.fallbackRate) : 0,
      progressColor: '#d99a2b',
    },
    {
      key: 'satisfactionRate',
      label: '满意度',
      value: source ? formatRate(source.satisfactionRate) : '—',
      hint: source
        ? `有帮助 ${source.helpfulCount} / 有反馈 ${source.feedbackCount}`
        : '有帮助反馈占比',
      icon: Star,
      tone: 'rose',
      progress: source ? clampRate(source.satisfactionRate) : 0,
      progressColor: '#1f9d7a',
    },
    {
      key: 'avgLatency',
      label: '平均耗时',
      value: source ? formatLatency(source.avgLatencyMs) : '—',
      hint: '端到端平均响应时间',
      icon: Timer,
      tone: 'violet',
    },
    {
      key: 'emergency',
      label: '紧急问题',
      value: source ? String(source.emergencyCount) : '—',
      hint: '请优先关注相关学生',
      icon: Bell,
      tone: 'danger',
    },
    {
      key: 'unanswered',
      label: '未命中 / 差评',
      value: source ? String(source.unansweredCount) : '—',
      hint: '降级、失败或“没帮助”',
      icon: QuestionFilled,
      tone: 'slate',
    },
  ]
})

async function loadStats(): Promise<void> {
  loading.value = true
  try {
    stats.value = await getAiStats(days.value)
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', {
      hour12: false,
    })
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

async function loadUnanswered(): Promise<void> {
  if (!canManage.value) {
    unanswered.value = []
    return
  }
  unansweredLoading.value = true
  try {
    unanswered.value = await listUnanswered(5)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    unansweredLoading.value = false
  }
}

async function refreshAll(): Promise<void> {
  await Promise.all([loadStats(), loadUnanswered()])
}

function goUnanswered(): void {
  router.push({
    path: '/ai/manage/consultations',
    query: { tab: 'unanswered' },
  })
}

function openDetail(row: AiConsultationVO): void {
  detailId.value = row.id
  detailVisible.value = true
}

watch(days, () => {
  refreshAll()
})

onMounted(refreshAll)
</script>

<template>
  <div class="stats-panel">
    <section class="panel-card stats-card">
      <div class="stats-head">
        <div>
          <div class="stats-title">
            <span>使用概览</span>
            <el-tag type="info" effect="plain" size="small">
              最近 {{ days }} 天
            </el-tag>
          </div>
          <p class="stats-sub">
            AI 问答使用情况汇总，不包含时间趋势序列。
            <span v-if="updatedAt">更新于 {{ updatedAt }}</span>
          </p>
        </div>
        <div class="stats-controls">
          <el-radio-group v-model="days" size="small">
            <el-radio-button :value="7">7 天</el-radio-button>
            <el-radio-button :value="30">30 天</el-radio-button>
            <el-radio-button :value="90">90 天</el-radio-button>
          </el-radio-group>
          <el-button
            :icon="Refresh"
            circle
            size="small"
            title="刷新统计"
            :loading="loading"
            @click="refreshAll"
          />
        </div>
      </div>

      <div v-loading="loading" class="kpi-grid">
        <article
          v-for="card in cards"
          :key="card.key"
          class="kpi-card"
          :class="`tone-${card.tone}`"
        >
          <div class="kpi-top">
            <span class="kpi-label">{{ card.label }}</span>
            <span class="kpi-icon">
              <el-icon><component :is="card.icon" /></el-icon>
            </span>
          </div>
          <div class="kpi-value">{{ card.value }}</div>
          <el-progress
            v-if="card.progress !== undefined"
            :percentage="card.progress"
            :show-text="false"
            :stroke-width="6"
            :color="card.progressColor"
            class="kpi-progress"
          />
          <p class="kpi-hint">{{ card.hint }}</p>
        </article>
      </div>

      <p class="stats-note">
        统计口径：最近 {{ days }} 天总提问数为 {{ stats?.total ?? '—' }} 条；
        降级率与满意度为百分比，平均耗时为端到端响应时间。
      </p>
    </section>

    <section v-if="canManage" class="panel-card unanswered-preview">
      <div class="preview-head">
        <div>
          <strong>未命中 / 差评问题</strong>
          <p>优先补充知识素材，可减少后续同类问题的备用方案与差评。</p>
        </div>
        <el-button link type="primary" @click="goUnanswered">
          查看全部
        </el-button>
      </div>

      <div v-loading="unansweredLoading" class="preview-list">
        <button
          v-for="row in unanswered"
          :key="row.id"
          type="button"
          class="preview-item"
          @click="openDetail(row)"
        >
          <span class="preview-dot" />
          <span class="preview-question">{{ row.question }}</span>
          <span class="preview-time">
            {{ formatDateTime(row.consultTime) }}
          </span>
          <AiStatusTag :status="row.answerStatus" effect="plain" />
          <el-tag
            :type="feedbackTagType(row.feedback)"
            effect="plain"
            size="small"
          >
            {{ feedbackLabel(row.feedback) }}
          </el-tag>
        </button>

        <el-empty
          v-if="!unanswered.length && !unansweredLoading"
          description="暂无未命中或差评问题"
          :image-size="76"
        />
      </div>
    </section>

    <el-alert
      v-else
      type="info"
      :closable="false"
      show-icon
      title="未命中问题明细需要问答管理权限"
      description="当前账号可以查看统计数据；如需查看具体问题，请联系管理员开通 ai:consult:manage 权限。"
    />

    <AiConsultationDetailDrawer
      v-model="detailVisible"
      :consultation-id="detailId"
    />
  </div>
</template>

<style scoped>
.stats-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-card {
  padding: 18px 20px 14px;
}

.stats-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.stats-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--hp-text);
}

.stats-title :deep(.el-tag) {
  flex-shrink: 0;
}

.stats-sub {
  margin-top: 5px;
  font-size: 12px;
  color: var(--hp-text-muted);
}

.stats-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  min-height: 120px;
}

.kpi-card {
  --kpi-color: #1f9d7a;
  --kpi-bg: #eaf7f2;
  padding: 14px 15px 12px;
  background:
    linear-gradient(160deg, var(--kpi-bg), #ffffff 72%);
  border: 1px solid #e7f0ec;
  border-radius: 12px;
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.kpi-label {
  font-size: 11.5px;
  color: var(--hp-text-secondary);
}

.kpi-icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  font-size: 14px;
  color: var(--kpi-color);
  background: rgba(255, 255, 255, 0.72);
  border-radius: 9px;
}

.kpi-value {
  margin: 6px 0 4px;
  font-size: 25px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--kpi-color);
  letter-spacing: 0.3px;
}

.kpi-progress {
  margin: 6px 0 2px;
}

.kpi-hint {
  font-size: 10.5px;
  line-height: 1.5;
  color: var(--hp-text-muted);
}

.tone-green {
  --kpi-color: #1f9d7a;
  --kpi-bg: #eaf7f2;
}

.tone-blue {
  --kpi-color: #3f7ff0;
  --kpi-bg: #eef4ff;
}

.tone-amber {
  --kpi-color: #c78a1f;
  --kpi-bg: #fff7e6;
}

.tone-rose {
  --kpi-color: #d95d8e;
  --kpi-bg: #fdf0f5;
}

.tone-violet {
  --kpi-color: #7a63d9;
  --kpi-bg: #f2efff;
}

.tone-danger {
  --kpi-color: #d9534f;
  --kpi-bg: #fdf0ef;
}

.tone-slate {
  --kpi-color: #5f7a72;
  --kpi-bg: #f2f7f5;
}

.stats-note {
  padding-top: 12px;
  margin-top: 14px;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--hp-text-muted);
  border-top: 1px dashed #eef4f1;
}

.unanswered-preview {
  padding: 16px 18px;
}

.preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.preview-head strong {
  font-size: 14px;
  color: var(--hp-text);
}

.preview-head p {
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.preview-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 70px;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 8px;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 8px;
  transition: background 0.18s ease;
}

.preview-item:hover {
  background: #f5faf8;
}

.preview-dot {
  flex-shrink: 0;
  width: 6px;
  height: 6px;
  background: var(--hp-warning);
  border-radius: 50%;
}

.preview-question {
  flex: 1;
  overflow: hidden;
  font-size: 12.5px;
  color: var(--hp-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-time {
  flex-shrink: 0;
  font-size: 10.5px;
  color: var(--hp-text-muted);
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .stats-card {
    padding: 14px 12px;
  }

  .stats-controls {
    width: 100%;
    justify-content: space-between;
  }

  .kpi-value {
    font-size: 21px;
  }

  .preview-item {
    flex-wrap: wrap;
  }

  .preview-time {
    order: 3;
    width: 100%;
    padding-left: 16px;
  }
}
</style>
