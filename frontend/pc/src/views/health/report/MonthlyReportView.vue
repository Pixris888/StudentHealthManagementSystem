<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  Bicycle,
  Calendar,
  Clock,
  Download,
  MagicStick,
  Moon,
  Odometer,
  Sunny,
} from '@element-plus/icons-vue'
import {
  downloadMonthlyReport,
  getMonthlyReport,
  getReportHistory,
} from '@/api/report'
import {
  bmiTagType,
  currentMonthValue,
  formatCount,
  formatDecimal,
  formatMonthLabel,
  formatPercent,
  formatText,
  isFutureMonth,
} from '@/utils/report'
import { formatDateTime as formatDateTimeValue } from '@/utils/format'
import type {
  HealthReportHistoryVO,
  MonthlyReportVO,
} from '@/types/report'
import KpiCard from '@/components/report/KpiCard.vue'
import TrendChart from '@/components/report/TrendChart.vue'
import ReportNav from './components/ReportNav.vue'

const route = useRoute()

function initialMonth(): string {
  const value = route.query.month
  if (typeof value === 'string' && /^\d{4}-\d{2}$/.test(value)) {
    const [year, month] = value.split('-').map(Number)
    const now = new Date()
    if (
      year !== undefined &&
      month !== undefined &&
      year * 12 + (month - 1) <= now.getFullYear() * 12 + now.getMonth()
    ) {
      return value
    }
  }
  return currentMonthValue()
}

const selectedMonth = ref(initialMonth())
const report = ref<MonthlyReportVO | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const downloading = ref(false)

const history = ref<HealthReportHistoryVO[]>([])
const historyTotal = ref(0)
const historyLoading = ref(false)
const historyQuery = reactive({ page: 1, size: 10 })

const hasCheckinData = computed(() => (report.value?.checkinDays ?? 0) > 0)
const bmiLevelType = computed(() => bmiTagType(report.value?.bmiLevel))
const bmiTone = computed<'teal' | 'blue' | 'orange' | 'red'>(() => {
  switch (report.value?.bmiLevel) {
    case '肥胖':
      return 'red'
    case '超重':
      return 'orange'
    case '偏瘦':
      return 'blue'
    default:
      return 'teal'
  }
})

async function loadHistory(): Promise<void> {
  historyLoading.value = true
  try {
    const page = await getReportHistory({
      page: historyQuery.page,
      size: historyQuery.size,
    })
    history.value = page.records
    historyTotal.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    historyLoading.value = false
  }
}

async function loadReport(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    report.value = await getMonthlyReport(selectedMonth.value)
    historyQuery.page = 1
    await loadHistory()
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '报告加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function scrollToTop(): void {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleMonthChange(): void {
  loadReport()
  scrollToTop()
}

function handleHistoryView(row: HealthReportHistoryVO): void {
  selectedMonth.value = row.month
  loadReport()
  scrollToTop()
}

function handleHistoryPage(page: number): void {
  historyQuery.page = page
  loadHistory()
}

function handleHistorySize(size: number): void {
  historyQuery.size = size
  historyQuery.page = 1
  loadHistory()
}

async function handleDownload(): Promise<void> {
  downloading.value = true
  try {
    await downloadMonthlyReport(selectedMonth.value)
  } finally {
    downloading.value = false
  }
}

watch(
  () => route.query.month,
  (value) => {
    if (
      typeof value === 'string' &&
      /^\d{4}-\d{2}$/.test(value) &&
      value !== selectedMonth.value
    ) {
      selectedMonth.value = value
      loadReport()
    }
  },
)

onMounted(loadReport)
</script>

<template>
  <div class="monthly-page">
    <div class="page-heading report-heading">
      <div>
        <span class="report-eyebrow">MONTHLY HEALTH REPORT</span>
        <h2>个人健康报告</h2>
        <p>按月查看打卡、睡眠、运动、情绪与体质指标，并下载 Excel 报告。</p>
      </div>
      <div class="heading-actions">
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          value-format="YYYY-MM"
          format="YYYY-MM"
          :disabled-date="isFutureMonth"
          :clearable="false"
          placeholder="选择月份"
          style="width: 150px"
          @change="handleMonthChange"
        />
        <el-button
          type="primary"
          :icon="Download"
          :loading="downloading"
          @click="handleDownload"
        >
          下载 Excel
        </el-button>
      </div>
    </div>

    <ReportNav />

    <div v-if="errorMessage && !report" class="panel-card error-card">
      <el-result
        icon="warning"
        title="报告加载失败"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-button type="primary" @click="loadReport">重新加载</el-button>
        </template>
      </el-result>
    </div>

    <div v-else-if="loading && !report" class="panel-card loading-card">
      <el-skeleton animated :rows="10" />
    </div>

    <template v-else-if="report">
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
        class="page-alert"
      />

      <section class="panel-card profile-card">
        <div class="profile-main">
          <el-avatar :size="54" class="profile-avatar">
            {{ report.realName?.charAt(0) || '学' }}
          </el-avatar>
          <div class="profile-info">
            <div class="profile-name">
              <h3>{{ report.realName || '—' }}</h3>
              <el-tag type="success" effect="plain" size="small">
                {{ formatMonthLabel(report.month) }}
              </el-tag>
            </div>
            <p>
              学号：{{ formatText(report.studentNo) }}
              <span class="meta-sep">·</span>
              {{ formatText(report.college) }}
            </p>
          </div>
        </div>

        <div class="profile-meta">
          <span>专业：{{ formatText(report.major) }}</span>
          <span>年级：{{ formatText(report.grade) }}</span>
          <span>班级：{{ formatText(report.className) }}</span>
          <el-tag
            v-if="report.latestPhysicalGradeLevel"
            :type="
              report.latestPhysicalGradeLevel === '不及格'
                ? 'danger'
                : 'success'
            "
            effect="plain"
            size="small"
          >
            体测 {{ report.latestPhysicalScore ?? '—' }} 分 ·
            {{ report.latestPhysicalGradeLevel }}
          </el-tag>
        </div>
      </section>

      <el-alert
        v-if="!hasCheckinData"
        title="本月暂无可统计的打卡数据，以下指标为空或为 0"
        type="info"
        :closable="false"
        show-icon
        class="page-alert"
      />

      <div v-loading="loading" class="report-content">
        <section class="kpi-grid">
          <KpiCard
            label="打卡天数"
            :value="formatCount(report.checkinDays)"
            unit="天"
            :hint="`有效 ${report.validDays ?? 0} 天 · 异常 ${report.abnormalDays ?? 0} 天`"
            :icon="Calendar"
            tone="teal"
          />
          <KpiCard
            label="平均睡眠"
            :value="formatDecimal(report.avgSleepDuration, 1)"
            unit="小时"
            :hint="`熬夜率 ${formatPercent(report.lateNightRate)}`"
            :icon="Moon"
            tone="blue"
          />
          <KpiCard
            label="熬夜次数"
            :value="formatCount(report.stayUpLateCount)"
            unit="次"
            :hint="`共 ${report.checkinDays ?? 0} 个打卡日`"
            :icon="Clock"
            tone="orange"
          />
          <KpiCard
            label="平均运动"
            :value="formatDecimal(report.avgExerciseDuration, 1)"
            unit="分钟"
            :hint="`未达标 ${report.exerciseInsufficientCount ?? 0} 次`"
            :icon="Bicycle"
            tone="green"
          />
          <KpiCard
            label="平均心情"
            :value="formatDecimal(report.avgMoodScore, 1)"
            unit="分"
            :hint="`早餐率 ${formatPercent(report.breakfastRate)}`"
            :icon="Sunny"
            tone="purple"
          />
          <KpiCard
            label="BMI"
            :value="formatDecimal(report.bmi, 2)"
            :hint="`分级 ${report.bmiLevel || '—'} · 平均饮水 ${formatCount(report.avgWaterIntake)} ml`"
            :icon="Odometer"
            :tone="bmiTone"
          />
        </section>

        <div class="report-main-grid">
          <section class="panel-card section-card trend-card">
            <div class="section-head">
              <div>
                <h3>每日趋势</h3>
                <p>睡眠、运动与心情；数据质量异常的日期不参与曲线展示。</p>
              </div>
              <el-tag :type="bmiLevelType" effect="plain" size="small">
                BMI {{ report.bmiLevel || '—' }}
              </el-tag>
            </div>
            <TrendChart :daily="report.daily" :loading="loading" />
          </section>

          <aside class="report-side">
            <section class="panel-card section-card suggestion-card">
              <div class="section-head compact">
                <div>
                  <h3>健康建议</h3>
                  <p>基于本月的健康数据自动生成</p>
                </div>
              </div>

              <ol v-if="report.suggestions?.length" class="suggestion-list">
                <li
                  v-for="(suggestion, index) in report.suggestions"
                  :key="`${index}-${suggestion.slice(0, 12)}`"
                >
                  <span class="suggestion-index">{{ index + 1 }}</span>
                  <span>{{ suggestion }}</span>
                </li>
              </ol>
              <el-empty
                v-else
                description="本月暂无额外健康建议"
                :image-size="64"
              />
            </section>

            <section class="panel-card section-card summary-card">
              <div class="section-head compact">
                <div>
                  <h3>报告摘要</h3>
                  <p>
                    生成时间
                    {{ formatDateTimeValue(report.generateTime) }}
                  </p>
                </div>
                <el-icon class="summary-icon"><MagicStick /></el-icon>
              </div>

              <el-collapse class="summary-collapse">
                <el-collapse-item title="展开查看完整摘要" name="summary">
                  <div class="summary-content">{{ report.summary }}</div>
                </el-collapse-item>
              </el-collapse>
            </section>
          </aside>
        </div>

        <section class="panel-card table-card">
          <div class="toolbar">
            <div class="toolbar-left">
              <span class="table-title">历史报告</span>
              <el-tag type="info" effect="plain">
                共 {{ historyTotal }} 份
              </el-tag>
            </div>
            <span class="toolbar-hint">点击“查看”可切换月份</span>
          </div>

          <el-table
            v-loading="historyLoading"
            :data="history"
            row-key="id"
            style="width: 100%"
          >
            <el-table-column label="月份" width="110">
              <template #default="{ row }">
                <el-tag
                  :type="row.month === selectedMonth ? 'success' : 'info'"
                  effect="plain"
                  size="small"
                >
                  {{ row.month }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="checkinDays"
              label="打卡天数"
              width="100"
              align="center"
            />
            <el-table-column label="平均睡眠" width="110" align="center">
              <template #default="{ row }">
                {{ formatDecimal(row.avgSleepDuration, 1) }} h
              </template>
            </el-table-column>
            <el-table-column
              prop="stayUpLateCount"
              label="熬夜次数"
              width="100"
              align="center"
            />
            <el-table-column label="平均运动" width="110" align="center">
              <template #default="{ row }">
                {{ formatDecimal(row.avgExerciseDuration, 1) }} min
              </template>
            </el-table-column>
            <el-table-column label="平均心情" width="100" align="center">
              <template #default="{ row }">
                {{ formatDecimal(row.avgMoodScore, 1) }}
              </template>
            </el-table-column>
            <el-table-column label="生成时间" min-width="160">
              <template #default="{ row }">
                <span class="time-cell">
                  {{ formatDateTimeValue(row.generateTime) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="handleHistoryView(row)"
                >
                  查看
                </el-button>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty
                description="暂无历史报告，切换月份生成后会自动保存"
                :image-size="76"
              />
            </template>
          </el-table>

          <div class="table-footer">
            <span class="selection-tip">
              报告在每次查看时重新生成，并更新为当月最新快照。
            </span>
            <el-pagination
              v-if="historyTotal > 0"
              :current-page="historyQuery.page"
              :page-size="historyQuery.size"
              :page-sizes="[10, 20]"
              :total="historyTotal"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="handleHistoryPage"
              @size-change="handleHistorySize"
            />
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.monthly-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.report-eyebrow {
  display: block;
  margin-bottom: 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2.2px;
  color: #9cb6ac;
}

.heading-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.page-alert {
  border-radius: 12px;
}

.loading-card,
.error-card {
  padding: 32px;
}

.profile-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22px;
  flex-wrap: wrap;
  padding: 20px 24px;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 15px;
  min-width: 0;
}

.profile-avatar {
  flex-shrink: 0;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #1f9d7a, #3ddc97);
}

.profile-name {
  display: flex;
  align-items: center;
  gap: 9px;
}

.profile-name h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--hp-text);
}

.profile-info p {
  margin-top: 5px;
  font-size: 12px;
  color: var(--hp-text-muted);
}

.meta-sep {
  margin: 0 7px;
  color: #c8d9d2;
}

.profile-meta {
  display: flex;
  align-items: center;
  gap: 8px 18px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.report-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(165px, 1fr));
  gap: 14px;
  align-items: stretch;
}

.report-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(290px, 1fr);
  gap: 18px;
  align-items: start;
}

.section-card {
  padding: 20px 22px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.section-head.compact {
  margin-bottom: 14px;
}

.section-head h3 {
  margin-bottom: 4px;
  font-size: 15px;
  font-weight: 700;
  color: var(--hp-text);
}

.section-head p {
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.report-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 13px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.suggestion-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--hp-text-secondary);
}

.suggestion-index {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 21px;
  height: 21px;
  margin-top: 1px;
  font-size: 10.5px;
  font-weight: 700;
  color: var(--hp-primary-dark);
  background: #e3f6ef;
  border-radius: 7px;
}

.summary-icon {
  color: var(--hp-primary);
}

.summary-collapse {
  border-top: none;
  border-bottom: none;
}

.summary-collapse :deep(.el-collapse-item__header) {
  height: 34px;
  font-size: 12px;
  color: var(--hp-primary-dark);
  border-bottom: none;
}

.summary-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

.summary-collapse :deep(.el-collapse-item__content) {
  padding-bottom: 4px;
}

.summary-content {
  font-size: 12px;
  line-height: 1.85;
  color: var(--hp-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.table-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text);
}

.toolbar-hint {
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.time-cell {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

@media (max-width: 1100px) {
  .report-main-grid {
    grid-template-columns: 1fr;
  }

  .report-side {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: start;
  }
}

@media (max-width: 760px) {
  .profile-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .report-side {
    grid-template-columns: 1fr;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .heading-actions {
    width: 100%;
  }
}
</style>
