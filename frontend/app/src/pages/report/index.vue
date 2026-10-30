<template>
  <view class="page">
    <view class="report-month-row">
      <view>
        <text class="report-title">我的健康报告</text>
        <text class="report-sub">用数据回顾这个月的生活状态</text>
      </view>
      <picker mode="date" fields="month" :value="month" :end="currentMonth" @change="onMonthChange">
        <view class="month-picker">
          <uni-icons type="calendar" size="15" color="#0f766e" />
          <text>{{ formatMonth(month) }}</text>
          <uni-icons type="arrowdown" size="13" color="#0f766e" />
        </view>
      </picker>
    </view>

    <view v-if="loading && !report" class="card loading-card">
      <uni-load-more status="loading" color="#0f9d8a" />
    </view>

    <template v-else-if="report">
      <view class="report-hero">
        <view class="report-hero-top">
          <view>
            <text class="report-hero-label">{{ formatMonth(report.month) }}</text>
            <text class="report-hero-title">{{ report.realName || userStore.displayName }}</text>
            <text class="report-hero-meta">
              {{ [report.college, report.className].filter(Boolean).join(' · ') || '个人健康月报' }}
            </text>
          </view>
          <view class="report-ring">
            <text class="report-ring-value">{{ report.checkinDays }}</text>
            <text class="report-ring-label">打卡天数</text>
          </view>
        </view>
        <view class="report-hero-footer">
          <text>有效天数 {{ report.validDays }} 天</text>
          <text>异常 {{ report.abnormalDays }} 天</text>
          <text>生成于 {{ formatDate(report.generateTime, 'MM-DD HH:mm') }}</text>
        </view>
        <button class="download-btn" :disabled="downloading" @click="handleDownload">
          <uni-icons type="download" size="16" color="#0f766e" />
          <text>{{ downloading ? '下载中…' : '下载 Excel 报告' }}</text>
        </button>
      </view>

      <view class="section-head">
        <view>
          <text class="section-title">关键指标</text>
          <text class="section-desc">本月健康数据一览</text>
        </view>
      </view>

      <view class="kpi-grid">
        <view class="kpi-card">
          <text class="kpi-icon">🌙</text>
          <text class="kpi-value">{{ formatNumber(report.avgSleepDuration, 1) }}<text class="kpi-unit">h</text></text>
          <text class="kpi-label">平均睡眠</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-icon">🌙</text>
          <text class="kpi-value">{{ report.stayUpLateCount ?? 0 }}<text class="kpi-unit">次</text></text>
          <text class="kpi-label">熬夜次数</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-icon">🏃</text>
          <text class="kpi-value">{{ formatNumber(report.avgExerciseDuration, 0) }}<text class="kpi-unit">min</text></text>
          <text class="kpi-label">平均运动</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-icon">🥗</text>
          <text class="kpi-value">{{ formatPercent(report.breakfastRate) }}</text>
          <text class="kpi-label">早餐率</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-icon">😊</text>
          <text class="kpi-value">{{ formatNumber(report.avgMoodScore, 1) }}<text class="kpi-unit">/5</text></text>
          <text class="kpi-label">平均心情</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-icon">💧</text>
          <text class="kpi-value">{{ formatNumber(report.avgWaterIntake, 0) }}<text class="kpi-unit">ml</text></text>
          <text class="kpi-label">平均饮水</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-icon">⚖️</text>
          <text class="kpi-value">{{ formatNumber(report.bmi, 1) }}</text>
          <text class="kpi-label">{{ report.bmiLevel || 'BMI' }}</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-icon">🏅</text>
          <text class="kpi-value">{{ report.latestPhysicalScore ?? '—' }}</text>
          <text class="kpi-label">{{ report.latestPhysicalGradeLevel || '最新体测' }}</text>
        </view>
      </view>

      <view class="card trend-card">
        <view class="between">
          <view>
            <text class="card-title">每日趋势</text>
            <text class="card-subtitle">点击切换指标查看变化</text>
          </view>
        </view>
        <view class="metric-tabs">
          <view
            v-for="option in metricOptions"
            :key="option.key"
            class="metric-tab"
            :class="{ active: metricKey === option.key }"
            @click="metricKey = option.key"
          >
            {{ option.label }}
          </view>
        </view>
        <SimpleLineChart
          :points="chartPoints"
          :unit="selectedMetric.unit"
          :value-digits="selectedMetric.digits"
          :height="210"
          color="#0f9d8a"
        />
      </view>

      <view v-if="report.suggestions?.length" class="card suggestion-card">
        <view class="between">
          <text class="card-title">健康建议</text>
          <text class="tag tag-primary">{{ report.suggestions.length }} 条</text>
        </view>
        <view class="suggestion-list">
          <view
            v-for="(suggestion, index) in report.suggestions"
            :key="index"
            class="suggestion-item"
          >
            <view class="suggestion-index">{{ index + 1 }}</view>
            <text class="suggestion-text">{{ suggestion }}</text>
          </view>
        </view>
      </view>

      <view v-if="report.summary" class="card summary-card">
        <view class="row" style="gap: 10rpx">
          <uni-icons type="info" size="17" color="#0f9d8a" />
          <text class="summary-title">本月总结</text>
        </view>
        <text class="summary-text">{{ report.summary }}</text>
      </view>

      <view v-if="history.length" class="section-head">
        <view>
          <text class="section-title">历史报告</text>
          <text class="section-desc">点击月份查看往期报告</text>
        </view>
      </view>

      <view v-if="history.length" class="card history-card">
        <view
          v-for="(item, index) in history"
          :key="item.id"
          class="history-row"
          :class="{ 'history-row-border': index !== history.length - 1, active: item.month === month }"
          @click="selectHistory(item.month)"
        >
          <view class="grow">
            <text class="history-month">{{ formatMonth(item.month) }}</text>
            <text class="history-meta">
              打卡 {{ item.checkinDays }} 天 · 睡眠 {{ formatNumber(item.avgSleepDuration, 1) }} h · 心情 {{ formatNumber(item.avgMoodScore, 1) }}
            </text>
          </view>
          <view class="tag" :class="item.month === month ? 'tag-primary' : 'tag-info'">
            {{ item.month === month ? '当前' : '查看' }}
          </view>
        </view>
      </view>
    </template>

    <EmptyState
      v-else
      icon="list"
      title="报告生成失败"
      desc="请确认本月有打卡数据，或稍后再试"
      action-text="重新加载"
      @action="loadReport"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import EmptyState from '@/components/EmptyState.vue'
import SimpleLineChart from '@/components/SimpleLineChart.vue'
import {
  downloadMonthlyReport,
  getMonthlyReport,
  getReportHistory,
} from '@/api/report'
import { useUserStore } from '@/stores/user'
import { getAccessToken } from '@/utils/auth'
import { currentMonthString } from '@/utils/checkin'
import {
  formatDate,
  formatMonth,
  formatNumber,
  formatPercent,
} from '@/utils/format'
import type {
  HealthReportHistoryVO,
  MonthlyReportVO,
} from '@/types/report'
import type { ChartPoint } from '@/types/chart'

type MetricKey = 'sleep' | 'exercise' | 'mood'

const metricOptions: Array<{
  key: MetricKey
  label: string
  unit: string
  digits: number
}> = [
  { key: 'sleep', label: '睡眠时长', unit: '小时', digits: 1 },
  { key: 'exercise', label: '运动时长', unit: '分钟', digits: 0 },
  { key: 'mood', label: '心情评分', unit: '/5', digits: 1 },
]

const userStore = useUserStore()
const month = ref(currentMonthString())
const currentMonth = currentMonthString()
const report = ref<MonthlyReportVO | null>(null)
const history = ref<HealthReportHistoryVO[]>([])
const loading = ref(false)
const downloading = ref(false)
const metricKey = ref<MetricKey>('sleep')

const selectedMetric = computed(
  () => metricOptions.find((item) => item.key === metricKey.value) ?? metricOptions[0],
)

const chartPoints = computed<ChartPoint[]>(() => {
  const daily = report.value?.daily ?? []
  return daily.map((point) => {
    let value: number | null = null
    if (metricKey.value === 'sleep') value = point.sleepDuration ?? null
    if (metricKey.value === 'exercise') value = point.exerciseDuration ?? null
    if (metricKey.value === 'mood') value = point.moodScore ?? null
    return {
      label: point.date ? point.date.slice(5) : '',
      value: value != null && !Number.isNaN(Number(value)) ? Number(value) : null,
    }
  })
})

function ensureLogin(): boolean {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

async function loadReport(): Promise<void> {
  if (!ensureLogin()) return
  if (!userStore.userInfo) {
    try {
      await userStore.bootstrap()
    } catch {
      uni.reLaunch({ url: '/pages/login/index' })
      return
    }
  }
  loading.value = true
  try {
    report.value = await getMonthlyReport(month.value)
  } catch {
    report.value = null
  } finally {
    loading.value = false
  }
}

async function loadHistory(): Promise<void> {
  try {
    const result = await getReportHistory({ page: 1, size: 6 })
    history.value = result.records ?? []
  } catch {
    history.value = []
  }
}

function onMonthChange(event: { detail: { value: string } }): void {
  const value = event.detail.value
  if (!value || value > currentMonth) return
  month.value = value
  loadReport()
}

function selectHistory(value: string): void {
  if (!value || value === month.value) return
  month.value = value
  loadReport()
  uni.pageScrollTo({ scrollTop: 0, duration: 200 })
}

async function handleDownload(): Promise<void> {
  if (downloading.value) return
  downloading.value = true
  try {
    await downloadMonthlyReport(month.value)
  } catch {
    // 请求层已提示
  } finally {
    downloading.value = false
  }
}

onShow(async () => {
  if (!ensureLogin()) return
  await Promise.all([loadReport(), loadHistory()])
})

onPullDownRefresh(async () => {
  await Promise.all([loadReport(), loadHistory()])
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.report-month-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 10rpx 6rpx 22rpx;
}

.report-title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #0f172a;
}

.report-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.month-picker {
  display: flex;
  align-items: center;
  gap: 6rpx;
  height: 58rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #e6f7f3;
  color: #0f766e;
  font-size: 22rpx;
}

.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.report-hero {
  position: relative;
  overflow: hidden;
  padding: 30rpx;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #12b39c 0%, #0b7a6b 100%);
  box-shadow: 0 16rpx 38rpx rgba(13, 148, 136, 0.2);
  color: #ffffff;
}

.report-hero::after {
  content: '';
  position: absolute;
  width: 280rpx;
  height: 280rpx;
  border-radius: 50%;
  right: -100rpx;
  top: -130rpx;
  background: rgba(255, 255, 255, 0.08);
}

.report-hero-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.report-hero-label {
  display: block;
  font-size: 21rpx;
  color: rgba(255, 255, 255, 0.68);
}

.report-hero-title {
  display: block;
  margin-top: 8rpx;
  font-size: 40rpx;
  font-weight: 700;
}

.report-hero-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 21rpx;
  color: rgba(255, 255, 255, 0.62);
}

.report-ring {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  border: 5rpx solid rgba(255, 255, 255, 0.24);
  background: rgba(255, 255, 255, 0.12);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.report-ring-value {
  font-size: 42rpx;
  font-weight: 700;
  line-height: 1;
}

.report-ring-label {
  margin-top: 6rpx;
  font-size: 18rpx;
  color: rgba(255, 255, 255, 0.68);
}

.report-hero-footer {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
  margin-top: 26rpx;
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.66);
}

.download-btn {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  height: 68rpx;
  margin-top: 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.92);
  color: #0f766e;
  font-size: 23rpx;
  font-weight: 600;
}

.download-btn::after {
  border: none;
}

.kpi-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.kpi-card {
  width: calc(25% - 12rpx);
  padding: 20rpx 14rpx;
  border-radius: 20rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(15, 118, 110, 0.05);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.kpi-icon {
  font-size: 26rpx;
}

.kpi-value {
  margin-top: 8rpx;
  font-size: 29rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
}

.kpi-unit {
  margin-left: 2rpx;
  font-size: 17rpx;
  font-weight: 400;
  color: #94a3b8;
}

.kpi-label {
  margin-top: 6rpx;
  font-size: 18rpx;
  color: #94a3b8;
}

.trend-card {
  margin-top: 24rpx;
}

.metric-tabs {
  display: flex;
  gap: 10rpx;
  margin-top: 20rpx;
  padding: 5rpx;
  border-radius: 999rpx;
  background: #f2f7f5;
}

.metric-tab {
  flex: 1;
  height: 56rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #64748b;
}

.metric-tab.active {
  background: #ffffff;
  color: #0f766e;
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(15, 118, 110, 0.08);
}

.suggestion-card {
  margin-top: 24rpx;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 20rpx;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}

.suggestion-index {
  width: 38rpx;
  height: 38rpx;
  border-radius: 50%;
  background: #e6f7f3;
  color: #0f766e;
  font-size: 20rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2rpx;
}

.suggestion-text {
  flex: 1;
  font-size: 24rpx;
  line-height: 1.7;
  color: #475569;
}

.summary-card {
  margin-top: 24rpx;
  background: linear-gradient(120deg, #f1fbf8, #e9f8f3);
}

.summary-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #0f766e;
}

.summary-text {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.8;
  color: #417a70;
}

.history-card {
  padding: 6rpx 24rpx;
}

.history-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 22rpx 0;
}

.history-row-border {
  border-bottom: 1rpx solid #f0f5f3;
}

.history-row.active {
  background: #f8fdfb;
}

.history-month {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: #1e293b;
}

.history-meta {
  display: block;
  margin-top: 5rpx;
  font-size: 20rpx;
  color: #94a3b8;
}
</style>
