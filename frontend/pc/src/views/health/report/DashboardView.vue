<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Bicycle,
  Calendar,
  Download,
  Moon,
  Refresh,
  Search,
  Sunny,
  Tickets,
  TrendCharts,
  UserFilled,
  Warning,
} from '@element-plus/icons-vue'
import {
  downloadPlatformReport,
  getDashboard,
  getLedgerFilters,
} from '@/api/report'
import { useUserStore } from '@/stores/user'
import {
  currentMonthValue,
  formatCount,
  formatDecimal,
  formatPercent,
  isFutureMonth,
} from '@/utils/report'
import type {
  DashboardVO,
  FilterOptionsVO,
  ReportQuery,
} from '@/types/report'
import KpiCard from '@/components/report/KpiCard.vue'
import DashboardTrendChart from '@/components/report/DashboardTrendChart.vue'
import CollegeChart from '@/components/report/CollegeChart.vue'
import EmotionChart from '@/components/report/EmotionChart.vue'
import RiskTable from '@/components/report/RiskTable.vue'
import ReportNav from './components/ReportNav.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function initialMonth(): string {
  const value = route.query.month
  return typeof value === 'string' && /^\d{4}-\d{2}$/.test(value)
    ? value
    : currentMonthValue()
}

const selectedMonth = ref(initialMonth())
const college = ref<string | undefined>(
  typeof route.query.college === 'string' ? route.query.college : undefined,
)
const grade = ref<string | undefined>(
  typeof route.query.grade === 'string' ? route.query.grade : undefined,
)

const filterOptions = ref<FilterOptionsVO>({
  colleges: [],
  grades: [],
  classes: [],
})
const filtersLoading = ref(false)

const dashboard = ref<DashboardVO | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const exporting = ref(false)

const isSuperAdmin = computed(() => userStore.roles.includes('SUPER_ADMIN'))
const canExport = computed(
  () => isSuperAdmin.value && userStore.hasPermission('report:export'),
)

const overview = computed(() => dashboard.value?.overview)
const hasData = computed(
  () =>
    (dashboard.value?.overview.studentTotal ?? 0) > 0 &&
    (dashboard.value?.overview.checkinRecords ?? 0) > 0,
)

const miniStats = computed(() => [
  {
    label: '打卡记录',
    value: formatCount(overview.value?.checkinRecords),
    unit: '条',
  },
  {
    label: '熬夜次数',
    value: formatCount(overview.value?.stayUpLateCount),
    unit: '次',
  },
  {
    label: '平均运动',
    value: formatDecimal(overview.value?.avgExerciseDuration, 1),
    unit: '分钟',
  },
  {
    label: '平均心情',
    value: formatDecimal(overview.value?.avgMoodScore, 1),
    unit: '分',
  },
  {
    label: '早餐率',
    value: formatPercent(overview.value?.breakfastRate),
    unit: '',
  },
  {
    label: '异常记录',
    value: formatCount(overview.value?.abnormalRecords),
    unit: '条',
  },
])

const riskCount = computed(() => dashboard.value?.riskStudents?.length ?? 0)

function buildQuery(): ReportQuery {
  return {
    month: selectedMonth.value,
    college: college.value || undefined,
    grade: grade.value || undefined,
  }
}

async function loadFilters(): Promise<void> {
  filtersLoading.value = true
  try {
    filterOptions.value = await getLedgerFilters()
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    filtersLoading.value = false
  }
}

async function loadDashboard(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    dashboard.value = await getDashboard(buildQuery())
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '看板加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  loadDashboard()
}

function resetFilters(): void {
  selectedMonth.value = currentMonthValue()
  college.value = undefined
  grade.value = undefined
  loadDashboard()
}

function openLedger(): void {
  const query: Record<string, string> = { month: selectedMonth.value }
  if (college.value) query.college = college.value
  if (grade.value) query.grade = grade.value
  router.push({ path: '/health/report/ledger', query })
}

async function handlePlatformExport(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `将按 ${selectedMonth.value} 的当前筛选条件导出全平台多 Sheet 报表，确定继续吗？`,
      '全平台导出',
      {
        type: 'warning',
        confirmButtonText: '开始导出',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  exporting.value = true
  try {
    await downloadPlatformReport(buildQuery())
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadFilters()
  loadDashboard()
})
</script>

<template>
  <div class="dashboard-page">
    <div class="page-heading report-heading">
      <div>
        <span class="report-eyebrow">HEALTH DASHBOARD</span>
        <h2>学生健康看板</h2>
        <p>汇总全体学生的打卡、睡眠、运动与情绪指标，识别需要重点关注的学生。</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="Tickets" @click="openLedger">健康台账</el-button>
        <el-button
          v-if="canExport"
          type="primary"
          :icon="Download"
          :loading="exporting"
          @click="handlePlatformExport"
        >
          全平台导出
        </el-button>
      </div>
    </div>

    <ReportNav />

    <section class="panel-card search-card">
      <el-form :model="{ selectedMonth, college, grade }" inline class="search-form">
        <el-form-item label="月份">
          <el-date-picker
            v-model="selectedMonth"
            type="month"
            value-format="YYYY-MM"
            format="YYYY-MM"
            :disabled-date="isFutureMonth"
            :clearable="false"
            placeholder="选择月份"
            style="width: 146px"
          />
        </el-form-item>
        <el-form-item label="学院">
          <el-select
            v-model="college"
            placeholder="全部学院"
            clearable
            filterable
            :loading="filtersLoading"
            style="width: 210px"
          >
            <el-option
              v-for="item in filterOptions.colleges"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select
            v-model="grade"
            placeholder="全部年级"
            clearable
            filterable
            :loading="filtersLoading"
            style="width: 150px"
          >
            <el-option
              v-for="item in filterOptions.grades"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            查询
          </el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
      class="page-alert"
    />

    <div v-if="loading && !dashboard" class="panel-card loading-card">
      <el-skeleton animated :rows="10" />
    </div>

    <template v-else-if="dashboard">
      <el-alert
        v-if="!hasData"
        title="当前筛选条件下暂无统计数据，请调整月份或筛选条件"
        type="info"
        :closable="false"
        show-icon
        class="page-alert"
      />

      <div v-loading="loading" class="dashboard-content">
        <section class="kpi-grid">
          <KpiCard
            label="学生总数"
            :value="formatCount(overview?.studentTotal)"
            unit="人"
            :hint="`统计月份 ${dashboard.month}`"
            :icon="UserFilled"
            tone="teal"
          />
          <KpiCard
            label="打卡学生"
            :value="formatCount(overview?.checkinStudents)"
            unit="人"
            :hint="`打卡记录 ${formatCount(overview?.checkinRecords)} 条`"
            :icon="Calendar"
            tone="blue"
          />
          <KpiCard
            label="打卡率"
            :value="formatPercent(overview?.checkinRate)"
            :hint="`${formatCount(overview?.checkinStudents)} / ${formatCount(overview?.studentTotal)} 人`"
            :icon="TrendCharts"
            tone="green"
          />
          <KpiCard
            label="平均睡眠"
            :value="formatDecimal(overview?.avgSleepDuration, 1)"
            unit="小时"
            :hint="`熬夜率 ${formatPercent(overview?.lateNightRate)}`"
            :icon="Moon"
            tone="purple"
          />
          <KpiCard
            label="熬夜率"
            :value="formatPercent(overview?.lateNightRate)"
            :hint="`熬夜 ${formatCount(overview?.stayUpLateCount)} 次`"
            :icon="Warning"
            tone="orange"
          />
          <KpiCard
            label="运动达标率"
            :value="formatPercent(overview?.exerciseSufficientRate)"
            :hint="`平均运动 ${formatDecimal(overview?.avgExerciseDuration, 1)} 分钟`"
            :icon="Bicycle"
            tone="teal"
          />
        </section>

        <section class="panel-card mini-stats">
          <div
            v-for="item in miniStats"
            :key="item.label"
            class="mini-stat"
          >
            <span class="mini-label">{{ item.label }}</span>
            <strong class="mini-value">
              {{ item.value }}
              <small v-if="item.unit">{{ item.unit }}</small>
            </strong>
          </div>
          <div class="mini-caption">
            <el-icon><Sunny /></el-icon>
            <span>比率字段均为百分数，学生总数按启用状态的学生账号统计。</span>
          </div>
        </section>

        <div class="chart-row trend-row">
          <section class="panel-card section-card">
            <div class="section-head">
              <div>
                <h3>每日趋势</h3>
                <p>打卡人数/条数，以及可切换的健康指标趋势</p>
              </div>
              <el-tag type="info" effect="plain" size="small">
                {{ dashboard.month }}
              </el-tag>
            </div>
            <DashboardTrendChart
              :trend="dashboard.trend"
              :loading="loading"
            />
          </section>

          <section class="panel-card section-card">
            <div class="section-head">
              <div>
                <h3>学院分布</h3>
                <p>各学院打卡率，悬停查看完整指标</p>
              </div>
            </div>
            <CollegeChart
              :colleges="dashboard.collegeDistribution"
              :loading="loading"
            />
          </section>
        </div>

        <div class="chart-row risk-row">
          <section class="panel-card section-card">
            <div class="section-head">
              <div>
                <h3>情绪分布</h3>
                <p>本月打卡记录中的情绪占比</p>
              </div>
            </div>
            <EmotionChart
              :emotions="dashboard.emotionDistribution"
              :loading="loading"
            />
          </section>

          <section class="panel-card section-card risk-card">
            <div class="section-head">
              <div>
                <h3>需关注学生</h3>
                <p>根据熬夜、运动不足与异常数据综合判定</p>
              </div>
              <div class="risk-head-tags">
                <el-tag type="info" effect="plain" size="small">
                  {{ riskCount }} 人
                </el-tag>
                <el-tag
                  v-if="!isSuperAdmin"
                  type="warning"
                  effect="plain"
                  size="small"
                >
                  姓名/学号已脱敏
                </el-tag>
              </div>
            </div>
            <RiskTable
              :students="dashboard.riskStudents"
              :loading="loading"
            />
          </section>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dashboard-page {
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

.search-card {
  padding: 18px 20px 4px;
}

.page-alert {
  border-radius: 12px;
}

.loading-card {
  padding: 32px;
}

.dashboard-content {
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

.mini-stats {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  align-items: center;
  padding: 14px 20px;
}

.mini-stat {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 4px 12px;
  border-left: 1px solid #edf3f0;
}

.mini-stat:first-child {
  border-left: none;
}

.mini-label {
  font-size: 11px;
  color: var(--hp-text-muted);
}

.mini-value {
  font-size: 17px;
  font-weight: 700;
  color: var(--hp-text);
}

.mini-value small {
  margin-left: 2px;
  font-size: 10px;
  font-weight: 500;
  color: var(--hp-text-muted);
}

.mini-caption {
  display: flex;
  align-items: center;
  gap: 6px;
  grid-column: 1 / -1;
  padding-top: 10px;
  font-size: 10.5px;
  color: var(--hp-text-muted);
  border-top: 1px dashed #e9f1ed;
}

.mini-caption .el-icon {
  color: var(--hp-primary);
}

.chart-row {
  display: grid;
  gap: 18px;
  align-items: start;
}

.trend-row {
  grid-template-columns: minmax(0, 1.65fr) minmax(320px, 1fr);
}

.risk-row {
  grid-template-columns: minmax(300px, 0.95fr) minmax(0, 2fr);
}

.section-card {
  padding: 20px 22px;
  min-width: 0;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
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

.risk-head-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.risk-card {
  overflow: hidden;
}

@media (max-width: 1180px) {
  .trend-row,
  .risk-row {
    grid-template-columns: 1fr;
  }

  .mini-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    row-gap: 14px;
  }

  .mini-stat:nth-child(4) {
    border-left: none;
  }
}

@media (max-width: 700px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .mini-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .mini-stat:nth-child(3),
  .mini-stat:nth-child(5) {
    border-left: none;
  }

  .mini-stat:nth-child(4) {
    border-left: 1px solid #edf3f0;
  }

  .search-card {
    padding: 14px 12px 0;
  }

  .heading-actions {
    width: 100%;
  }
}
</style>
