<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useECharts } from '@/utils/useECharts'
import { formatShortDate } from '@/utils/report'
import type { DailyHealthPointVO } from '@/types/report'

const props = withDefaults(
  defineProps<{
    daily: DailyHealthPointVO[]
    loading?: boolean
    height?: number
  }>(),
  {
    loading: false,
    height: 330,
  },
)

const chartRef = ref<HTMLDivElement>()
const { setOption } = useECharts(chartRef)

const validPoints = computed(() =>
  props.daily.filter(
    (item) =>
      item.dataQuality === 0 &&
      (item.sleepDuration != null ||
        item.exerciseDuration != null ||
        item.moodScore != null),
  ),
)
const hasData = computed(() => validPoints.value.length > 0)
const abnormalCount = computed(
  () => props.daily.filter((item) => item.dataQuality === 1).length,
)

function renderChart(): void {
  const dates = props.daily.map((item) => formatShortDate(item.date))
  const sleepData = props.daily.map((item) =>
    item.dataQuality === 0 ? (item.sleepDuration ?? null) : null,
  )
  const exerciseData = props.daily.map((item) =>
    item.dataQuality === 0 ? (item.exerciseDuration ?? null) : null,
  )
  const moodData = props.daily.map((item) =>
    item.dataQuality === 0 ? (item.moodScore ?? null) : null,
  )

  setOption({
    animationDuration: 420,
    color: ['#3f7ff0', '#1f9d7a', '#d99a2b'],
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
      textStyle: { color: '#607a72', fontSize: 11 },
      data: ['睡眠（小时）', '运动（分钟）', '心情（1-5）'],
    },
    grid: {
      left: 12,
      right: 18,
      top: 44,
      bottom: 8,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: '#e3ece8' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#8aa198',
        fontSize: 10.5,
        hideOverlap: true,
      },
    },
    yAxis: [
      {
        type: 'value',
        scale: true,
        name: '小时 / 分钟',
        nameTextStyle: { color: '#9ab0a7', fontSize: 10 },
        axisLabel: { color: '#8aa198', fontSize: 10.5 },
        splitLine: { lineStyle: { color: '#eef4f1' } },
      },
      {
        type: 'value',
        name: '心情',
        min: 0,
        max: 5,
        nameTextStyle: { color: '#9ab0a7', fontSize: 10 },
        axisLabel: { color: '#8aa198', fontSize: 10.5 },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '睡眠（小时）',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: sleepData,
        connectNulls: false,
        lineStyle: { width: 2.4 },
        areaStyle: { opacity: 0.06 },
        emphasis: { focus: 'series' },
      },
      {
        name: '运动（分钟）',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: exerciseData,
        connectNulls: false,
        lineStyle: { width: 2.4 },
        emphasis: { focus: 'series' },
      },
      {
        name: '心情（1-5）',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: moodData,
        connectNulls: false,
        lineStyle: { width: 2, type: 'dashed' },
        emphasis: { focus: 'series' },
      },
    ],
  })
}

watch(
  () => props.daily,
  () => renderChart(),
  { deep: true, immediate: true },
)
</script>

<template>
  <div
    v-loading="loading"
    class="chart-wrap"
    :style="{ height: `${height}px` }"
  >
    <div ref="chartRef" class="chart-canvas" />
    <div v-if="!hasData && !loading" class="chart-empty">
      <el-empty description="本月暂无可用打卡数据" :image-size="76" />
    </div>
  </div>

  <p v-if="abnormalCount > 0" class="chart-note">
    本月有 {{ abnormalCount }} 天数据质量异常，趋势图已隐藏这些日期的健康指标。
  </p>
</template>

<style scoped>
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

.chart-note {
  margin-top: 6px;
  font-size: 11.5px;
  color: var(--hp-text-muted);
}
</style>
