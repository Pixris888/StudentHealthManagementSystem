<template>
  <view class="page">
    <view class="physical-head">
      <view>
        <text class="physical-title">体测成绩</text>
        <text class="physical-sub">记录每次体测，观察身体指标变化</text>
      </view>
      <view class="add-btn" @click="goCreate">
        <uni-icons type="plusempty" size="17" color="#ffffff" />
        <text>新增</text>
      </view>
    </view>

    <view class="tabs-wrap">
      <SegTabs v-model="activeTab" :options="tabOptions" @change="onTabChange" />
    </view>

    <template v-if="activeTab === 'records'">
      <view v-if="loading && !records.length" class="card loading-card">
        <uni-load-more status="loading" color="#0f9d8a" />
      </view>

      <template v-else>
        <view
          v-for="record in records"
          :key="record.id"
          class="card record-card"
          @click="goEdit(record)"
        >
          <view class="between">
            <view class="row" style="gap: 10rpx">
              <text class="record-period">{{ record.testYear }} 年 · {{ record.testTerm }}</text>
              <text v-if="record.gradeLevel" class="tag tag-primary">{{ record.gradeLevel }}</text>
            </view>
            <uni-icons type="arrowright" size="15" color="#c4d0cc" />
          </view>

          <view class="score-row">
            <view class="score-main">
              <text class="score-value">{{ formatNumber(record.totalScore, 1) }}</text>
              <text class="score-label">总分</text>
            </view>
            <view class="score-divider" />
            <view class="score-side">
              <view class="score-side-row">
                <text class="score-side-label">BMI</text>
                <text class="score-side-value">{{ formatNumber(record.bmi, 1) }}</text>
                <text v-if="bmiLevelOf(record.bmi)" class="tag" :class="`tag-${bmiTone(record.bmi)}`">
                  {{ bmiLevelOf(record.bmi) }}
                </text>
              </view>
              <view class="score-side-row">
                <text class="score-side-label">测试日期</text>
                <text class="score-side-value">{{ record.testDate || '—' }}</text>
              </view>
            </view>
          </view>

          <view class="metric-mini-grid">
            <view class="metric-mini">
              <text class="metric-mini-label">肺活量</text>
              <text class="metric-mini-value">{{ record.vitalCapacity ?? '—' }}<text class="metric-mini-unit">ml</text></text>
            </view>
            <view class="metric-mini">
              <text class="metric-mini-label">50 米跑</text>
              <text class="metric-mini-value">{{ formatNumber(record.run50m, 1) }}<text class="metric-mini-unit">s</text></text>
            </view>
            <view class="metric-mini">
              <text class="metric-mini-label">立定跳远</text>
              <text class="metric-mini-value">{{ formatNumber(record.standingJump, 1) }}<text class="metric-mini-unit">cm</text></text>
            </view>
            <view class="metric-mini">
              <text class="metric-mini-label">坐位体前屈</text>
              <text class="metric-mini-value">{{ formatNumber(record.sitAndReach, 1) }}<text class="metric-mini-unit">cm</text></text>
            </view>
          </view>

          <view class="record-tip">
            <uni-icons type="compose" size="13" color="#0f9d8a" />
            <text>点击编辑本次体测记录</text>
          </view>
        </view>

        <EmptyState
          v-if="!records.length"
          icon="medal"
          title="还没有体测记录"
          desc="录入第一次体测成绩，开始观察指标变化"
          action-text="新增体测记录"
          @action="goCreate"
        />

        <view v-if="records.length" class="load-more">
          <uni-load-more
            :status="loading ? 'loading' : hasMore ? 'more' : 'noMore'"
            color="#94a3b8"
          />
        </view>
      </template>
    </template>

    <template v-else>
      <scroll-view scroll-x class="metric-scroll" :show-scrollbar="false">
        <view class="metric-chips">
          <view
            v-for="option in metricOptions"
            :key="option.key"
            class="metric-chip"
            :class="{ active: metricKey === option.key }"
            @click="selectMetric(option.key)"
          >
            {{ option.label }}
          </view>
        </view>
      </scroll-view>

      <view v-if="trendLoading" class="card loading-card">
        <uni-load-more status="loading" color="#0f9d8a" />
      </view>

      <template v-else-if="chartPoints.length">
        <view class="card trend-card">
          <view class="between">
            <view>
              <text class="card-title">{{ selectedMetric.label }}趋势</text>
              <text class="card-subtitle">按测试日期先后排列</text>
            </view>
            <text class="tag tag-primary">{{ selectedMetric.unit || '数值' }}</text>
          </view>

          <view class="trend-stats">
            <view class="trend-stat">
              <text class="trend-stat-value">{{ formatNumber(latestValue, selectedMetric.digits) }}</text>
              <text class="trend-stat-label">最新</text>
            </view>
            <view class="trend-stat">
              <text class="trend-stat-value">{{ formatNumber(averageValue, selectedMetric.digits) }}</text>
              <text class="trend-stat-label">平均</text>
            </view>
            <view class="trend-stat">
              <text class="trend-stat-value">{{ chartPoints.length }}</text>
              <text class="trend-stat-label">数据点</text>
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

        <view class="card detail-card">
          <text class="card-title">数据明细</text>
          <view class="detail-table">
            <view class="detail-row detail-head-row">
              <text class="detail-cell period-cell">测试</text>
              <text class="detail-cell value-cell">数值</text>
              <text class="detail-cell date-cell">日期</text>
            </view>
            <view
              v-for="(record, index) in sortedTrend"
              :key="record.id"
              class="detail-row"
              :class="{ 'detail-row-border': index !== sortedTrend.length - 1 }"
            >
              <text class="detail-cell period-cell">{{ record.testYear }} {{ record.testTerm }}</text>
              <text class="detail-cell value-cell">
                {{ formatNumber(record[selectedMetric.key], selectedMetric.digits) }}
                <text class="detail-unit">{{ selectedMetric.unit }}</text>
              </text>
              <text class="detail-cell date-cell">{{ record.testDate || '—' }}</text>
            </view>
          </view>
        </view>
      </template>

      <EmptyState
        v-else
        icon="medal"
        title="暂无趋势数据"
        desc="至少录入一条体测记录后，才能查看指标趋势"
        action-text="新增体测记录"
        @action="goCreate"
      />
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import SegTabs from '@/components/SegTabs.vue'
import EmptyState from '@/components/EmptyState.vue'
import SimpleLineChart from '@/components/SimpleLineChart.vue'
import { getPhysicalTrend, pageMyPhysicalTests } from '@/api/health-physical'
import { useUserStore } from '@/stores/user'
import { getAccessToken } from '@/utils/auth'
import { formatNumber } from '@/utils/format'
import {
  bmiLevelOf,
  PHYSICAL_METRIC_OPTIONS,
  type PhysicalMetricKey,
} from '@/utils/health'
import type { PhysicalTest } from '@/types/health'
import type { ChartPoint } from '@/types/chart'

const tabOptions = [
  { value: 'records', label: '体测记录' },
  { value: 'trend', label: '指标趋势' },
]
const metricOptions = PHYSICAL_METRIC_OPTIONS

const userStore = useUserStore()
const activeTab = ref<'records' | 'trend'>('records')
const records = ref<PhysicalTest[]>([])
const page = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
const trend = ref<PhysicalTest[]>([])
const trendLoading = ref(false)
const metricKey = ref<PhysicalMetricKey>('bmi')

const hasMore = computed(() => records.value.length < total.value)
const selectedMetric = computed(
  () => metricOptions.find((item) => item.key === metricKey.value) ?? metricOptions[0],
)

const sortedTrend = computed(() =>
  [...trend.value].sort((a, b) => {
    const left = a.testDate || `${a.testYear}`
    const right = b.testDate || `${b.testYear}`
    return left.localeCompare(right)
  }),
)

const chartPoints = computed<ChartPoint[]>(() =>
  sortedTrend.value
    .map((record) => {
      const raw = record[metricKey.value]
      const value = raw == null || Number.isNaN(Number(raw)) ? null : Number(raw)
      const label = record.testDate
        ? record.testDate.slice(2, 7)
        : String(record.testYear)
      return { label, value }
    })
    .filter((item) => item.value != null),
)

const latestValue = computed(() => {
  const points = chartPoints.value
  return points.length ? points[points.length - 1]?.value ?? null : null
})

const averageValue = computed(() => {
  const values = chartPoints.value
    .map((item) => item.value)
    .filter((value): value is number => value != null)
  if (!values.length) return null
  return values.reduce((sum, value) => sum + value, 0) / values.length
})

function bmiTone(bmi?: number | null): string {
  const level = bmiLevelOf(bmi)
  if (level === '正常') return 'success'
  if (level === '偏瘦') return 'info'
  if (level === '超重') return 'warning'
  return 'danger'
}

function ensureLogin(): boolean {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

async function ensureUser(): Promise<boolean> {
  if (!ensureLogin()) return false
  if (!userStore.userInfo) {
    try {
      await userStore.bootstrap()
    } catch {
      uni.reLaunch({ url: '/pages/login/index' })
      return false
    }
  }
  return true
}

async function loadRecords(reset = false): Promise<void> {
  if (loading.value) return
  if (!(await ensureUser())) return
  if (reset) {
    page.value = 1
    total.value = 0
    records.value = []
  }
  loading.value = true
  try {
    const result = await pageMyPhysicalTests({ page: page.value, size })
    const list = result.records ?? []
    records.value = reset ? list : [...records.value, ...list]
    total.value = result.total ?? records.value.length
  } catch {
    // 请求层已提示
  } finally {
    loading.value = false
  }
}

async function loadTrend(): Promise<void> {
  if (trendLoading.value) return
  if (!(await ensureUser())) return
  trendLoading.value = true
  try {
    trend.value = (await getPhysicalTrend()) ?? []
  } catch {
    trend.value = []
  } finally {
    trendLoading.value = false
  }
}

function onTabChange(value: string | number): void {
  if (value === 'trend' && !trend.value.length) {
    loadTrend()
  }
}

function selectMetric(key: PhysicalMetricKey): void {
  metricKey.value = key
}

function goCreate(): void {
  uni.navigateTo({ url: '/pages/physical/edit' })
}

function goEdit(record: PhysicalTest): void {
  uni.navigateTo({ url: `/pages/physical/edit?id=${record.id}` })
}

function loadMore(): void {
  if (activeTab.value !== 'records') return
  if (loading.value || !hasMore.value) return
  page.value += 1
  loadRecords(false)
}

onShow(async () => {
  if (!(await ensureUser())) return
  await Promise.all([loadRecords(true), loadTrend()])
})

onPullDownRefresh(async () => {
  await Promise.all([loadRecords(true), loadTrend()])
  uni.stopPullDownRefresh()
})

onReachBottom(loadMore)
</script>

<style scoped>
.physical-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 10rpx 6rpx 22rpx;
}

.physical-title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #0f172a;
}

.physical-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  height: 64rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #0f9d8a;
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 600;
}

.tabs-wrap {
  margin-bottom: 22rpx;
}

.loading-card {
  min-height: 260rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.record-card {
  margin-bottom: 18rpx;
}

.record-period {
  font-size: 27rpx;
  font-weight: 700;
  color: #0f172a;
}

.score-row {
  display: flex;
  align-items: center;
  margin-top: 22rpx;
  padding: 20rpx 22rpx;
  border-radius: 20rpx;
  background: linear-gradient(120deg, #f1fbf8, #e8f8f2);
}

.score-main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 150rpx;
}

.score-value {
  font-size: 56rpx;
  font-weight: 700;
  line-height: 1;
  color: #0f766e;
}

.score-label {
  margin-top: 6rpx;
  font-size: 20rpx;
  color: #7b9a92;
}

.score-divider {
  width: 1rpx;
  height: 70rpx;
  background: #d4ede6;
  margin: 0 26rpx;
}

.score-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.score-side-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.score-side-label {
  width: 110rpx;
  font-size: 21rpx;
  color: #7b9a92;
}

.score-side-value {
  font-size: 24rpx;
  font-weight: 600;
  color: #334155;
}

.metric-mini-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 18rpx;
}

.metric-mini {
  width: calc(50% - 7rpx);
  padding: 14rpx 18rpx;
  border-radius: 14rpx;
  background: #f7faf9;
}

.metric-mini-label {
  display: block;
  font-size: 19rpx;
  color: #a8b7b2;
}

.metric-mini-value {
  display: block;
  margin-top: 4rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #334155;
}

.metric-mini-unit {
  margin-left: 2rpx;
  font-size: 18rpx;
  font-weight: 400;
  color: #94a3b8;
}

.record-tip {
  display: flex;
  align-items: center;
  gap: 6rpx;
  margin-top: 16rpx;
  padding-top: 14rpx;
  border-top: 1rpx solid #f0f5f3;
  font-size: 19rpx;
  color: #0f9d8a;
}

.metric-scroll {
  width: 100%;
  margin-bottom: 20rpx;
  white-space: nowrap;
}

.metric-chips {
  display: inline-flex;
  gap: 12rpx;
}

.metric-chip {
  height: 58rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #64748b;
  font-size: 23rpx;
  display: inline-flex;
  align-items: center;
  border: 2rpx solid transparent;
}

.metric-chip.active {
  background: #e6f7f3;
  border-color: #a8e5d9;
  color: #0f766e;
  font-weight: 600;
}

.trend-card {
  margin-bottom: 20rpx;
}

.trend-stats {
  display: flex;
  align-items: center;
  margin: 22rpx 0 12rpx;
  padding: 18rpx 0;
  border-radius: 18rpx;
  background: #f7faf9;
}

.trend-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.trend-stat + .trend-stat::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6rpx;
  bottom: 6rpx;
  width: 1rpx;
  background: #e6efeb;
}

.trend-stat-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.trend-stat-label {
  margin-top: 4rpx;
  font-size: 19rpx;
  color: #94a3b8;
}

.detail-card {
  margin-bottom: 20rpx;
}

.detail-table {
  margin-top: 18rpx;
}

.detail-row {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
}

.detail-row-border {
  border-bottom: 1rpx solid #f0f5f3;
}

.detail-head-row {
  padding-top: 0;
  color: #94a3b8;
  font-size: 20rpx;
}

.detail-cell {
  font-size: 22rpx;
  color: #334155;
}

.detail-head-row .detail-cell {
  color: #94a3b8;
}

.period-cell {
  flex: 1.3;
}

.value-cell {
  flex: 1;
}

.date-cell {
  flex: 1;
  text-align: right;
}

.detail-unit {
  margin-left: 2rpx;
  font-size: 18rpx;
  color: #94a3b8;
}

.load-more {
  padding: 10rpx 0 20rpx;
}
</style>
