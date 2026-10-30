<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useECharts } from '@/utils/useECharts'
import { formatShortDate } from '@/utils/report'
import type { TrendPointVO } from '@/types/report'

type MetricKey = 'sleep' | 'exercise' | 'mood'

const props = withDefaults(
  defineProps<{
    trend: TrendPointVO[]
    loading?: boolean
    height?: number
  }>(),
  {
    loading: false,
    height: 310,
  },
)

const metric = ref<MetricKey>('sleep')
const chartRef = ref<HTMLDivElement>()
const { setOption } = useECharts(chartRef)

const METRICS: Record<
  MetricKey,
  {
    label: string
    unit: string
    color: string
    field: 'avgSleepDuration' | 'avgExerciseDuration' | 'avgMoodScore'
  }
> = {
  sleep: {
    label: '平均睡眠',
    unit: '小时',
    color: '#3f7ff0',
    field: 'avgSleepDuration',
  },
  exercise: {
    label: '平均运动',
    unit: '分钟',
    color: '#1f9d7a',
    field: 'avgExerciseDuration',
  },
  mood: {
    label: '平均心情',
    unit: '分',
    color: '#d99a2b',
    field: 'avgMoodScore',
  },
}

const hasData = computed(
  () =>
    props.trend.length > 0 &&
    props.trend.some(
      (item) => item.checkinRecords > 0 || item.checkinStudents > 0,
    ),
)

function renderChart(): void {
  const current = METRICS[metric.value]
  const dates = props.trend.map((item) => formatShortDate(item.date))
  const students = props.trend.map((item) => item.checkinStudents ?? 0)
  const records = props.trend.map((item) => item.checkinRecords ?? 0)
  const metricData = props.trend.map((item) => {
    const value = item[current.field]
    return value === null || value === undefined ? null : value
  })
  const metricName = `${current.label}（${current.unit}）`

  setOption({
    animationDuration: 420,
    color: ['#8fcebd', '#1f9d7a', current.color],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 74, 64, 0.94)',
      borderWidth: 0,
      padding: [8, 12],
      textStyle: { color: '#eafff7', fontSize: 12 },
    },
    legend: {
      top: 2,
      right: 8,
      itemWidth: 10,
      itemHeight: 8,
      textStyle: { color: '#607a72', fontSize: 10.5 },
      data: ['打卡学生数', '打卡记录数', metricName],
    },
    grid: {
      left: 12,
      right: 16,
      top: 44,
      bottom: 8,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e3ece8' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#8aa198',
        fontSize: 10,
        hideOverlap: true,
      },
    },
    yAxis: [
      {
        type: 'value',
        name: '人数 / 条数',
        nameTextStyle: { color: '#9ab0a7', fontSize: 10 },
        axisLabel: { color: '#8aa198', fontSize: 10 },
        splitLine: { lineStyle: { color: '#eef4f1' } },
      },
      {
        type: 'value',
        name: current.unit,
        min: metric.value === 'mood' ? 0 : undefined,
        max: metric.value === 'mood' ? 5 : undefined,
        scale: metric.value !== 'mood',
        nameTextStyle: { color: '#9ab0a7', fontSize: 10 },
        axisLabel: { color: '#8aa198', fontSize: 10 },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '打卡学生数',
        type: 'bar',
        data: students,
        barMaxWidth: 16,
        itemStyle: { borderRadius: [3, 3, 0, 0], opacity: 0.7 },
      },
      {
        name: '打卡记录数',
        type: 'bar',
        data: records,
        barMaxWidth: 16,
        itemStyle: { borderRadius: [3, 3, 0, 0] },
      },
      {
        name: metricName,
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: metricData,
        connectNulls: false,
        lineStyle: { width: 2.4 },
        emphasis: { focus: 'series' },
      },
    ],
  })
}

watch(
  [() => props.trend, metric],
  () => renderChart(),
  { deep: true, immediate: true },
)
</script>

<template>
  <div class="trend-head">
    <span class="trend-label">健康指标</span>
    <el-radio-group v-model="metric" size="small">
      <el-radio-button value="sleep">睡眠</el-radio-button>
      <el-radio-button value="exercise">运动</el-radio-button>
      <el-radio-button value="mood">心情</el-radio-button>
    </el-radio-group>
  </div>

  <div
    v-loading="loading"
    class="chart-wrap"
    :style="{ height: `${height}px` }"
  >
    <div ref="chartRef" class="chart-canvas" />
    <div v-if="!hasData && !loading" class="chart-empty">
      <el-empty description="当前筛选条件下暂无趋势数据" :image-size="76" />
    </div>
  </div>
</template>

<style scoped>
.trend-head {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 6px;
}

.trend-label {
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.chart-wrap {
  position: relative;
  width: 100%;
}

.chart-canvas {
  width: 100%;
  height: 100%;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: #fff;
}
</style>
