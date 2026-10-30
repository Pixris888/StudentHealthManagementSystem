<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useECharts } from '@/utils/useECharts'
import { formatDecimal, formatPercent } from '@/utils/report'
import type { CollegeDistributionVO } from '@/types/report'

const props = withDefaults(
  defineProps<{
    colleges: CollegeDistributionVO[]
    loading?: boolean
  }>(),
  { loading: false },
)

const chartRef = ref<HTMLDivElement>()
const { setOption } = useECharts(chartRef)

const hasData = computed(() => props.colleges.length > 0)
const chartHeight = computed(() =>
  Math.max(220, props.colleges.length * 46 + 30),
)

function renderChart(): void {
  const names = props.colleges.map((item) => item.college)
  const rates = props.colleges.map((item) => item.checkinRate ?? null)

  setOption({
    animationDuration: 420,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(15, 74, 64, 0.94)',
      borderWidth: 0,
      padding: [9, 13],
      textStyle: { color: '#eafff7', fontSize: 12 },
      formatter: (params: unknown) => {
        const list = params as Array<{ dataIndex: number }>
        const item = props.colleges[list[0]?.dataIndex ?? 0]
        if (!item) return ''
        return [
          `<strong>${item.college}</strong>`,
          `学生 ${item.studentCount ?? 0} 人 · 打卡 ${item.checkinStudents ?? 0} 人`,
          `打卡率：${formatPercent(item.checkinRate)}`,
          `平均睡眠：${formatDecimal(item.avgSleepDuration)} 小时`,
          `熬夜率：${formatPercent(item.lateNightRate)}`,
          `运动达标率：${formatPercent(item.exerciseSufficientRate)}`,
        ].join('<br/>')
      },
    },
    grid: {
      left: 6,
      right: 44,
      top: 8,
      bottom: 8,
      containLabel: true,
    },
    xAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: {
        color: '#8aa198',
        fontSize: 10,
        formatter: '{value}%',
      },
      splitLine: { lineStyle: { color: '#eef4f1' } },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: names,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#607a72',
        fontSize: 10.5,
        width: 118,
        overflow: 'truncate',
      },
    },
    series: [
      {
        name: '打卡率',
        type: 'bar',
        data: rates,
        barMaxWidth: 18,
        label: {
          show: true,
          position: 'right',
          color: '#8aa198',
          fontSize: 10,
          formatter: (params: unknown) => {
            const value = (params as { value?: number | null }).value
            return value == null ? '—' : formatPercent(value)
          },
        },
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: '#1f9d7a',
        },
        emphasis: {
          itemStyle: { color: '#14745a' },
        },
      },
    ],
  })
}

watch(
  () => props.colleges,
  () => renderChart(),
  { deep: true, immediate: true },
)
</script>

<template>
  <div
    v-loading="loading"
    class="chart-wrap"
    :style="{ height: `${chartHeight}px` }"
  >
    <div ref="chartRef" class="chart-canvas" />
    <div v-if="!hasData && !loading" class="chart-empty">
      <el-empty description="暂无学院分布数据" :image-size="76" />
    </div>
  </div>
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
</style>
