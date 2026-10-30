<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useECharts } from '@/utils/useECharts'
import { EMOTION_COLORS, formatPercent } from '@/utils/report'
import type { EmotionDistributionVO } from '@/types/report'

const props = withDefaults(
  defineProps<{
    emotions: EmotionDistributionVO[]
    loading?: boolean
    height?: number
  }>(),
  {
    loading: false,
    height: 290,
  },
)

const chartRef = ref<HTMLDivElement>()
const { setOption } = useECharts(chartRef)

const hasData = computed(() => props.emotions.length > 0)

function renderChart(): void {
  const data = props.emotions.map((item) => ({
    name: item.emotionName,
    value: item.count,
    itemStyle: {
      color: EMOTION_COLORS[item.emotionType] ?? '#8aa198',
    },
  }))

  setOption({
    animationDuration: 420,
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 74, 64, 0.94)',
      borderWidth: 0,
      padding: [9, 13],
      textStyle: { color: '#eafff7', fontSize: 12 },
      formatter: (params: unknown) => {
        const item = params as { dataIndex: number }
        const record = props.emotions[item.dataIndex]
        if (!record) return ''
        return `${record.emotionName}<br/>${record.count} 条 · ${formatPercent(record.rate)}`
      },
    },
    legend: {
      bottom: 0,
      left: 'center',
      itemWidth: 9,
      itemHeight: 9,
      icon: 'circle',
      textStyle: { color: '#607a72', fontSize: 10.5 },
      formatter: (name: string) => {
        const item = props.emotions.find((emotion) => emotion.emotionName === name)
        return item ? `${name} ${formatPercent(item.rate)}` : name
      },
    },
    series: [
      {
        name: '情绪分布',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 2,
          borderRadius: 4,
        },
        label: { show: false },
        labelLine: { show: false },
        emphasis: {
          scale: true,
          scaleSize: 4,
        },
        data,
      },
    ],
  })
}

watch(
  () => props.emotions,
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
      <el-empty description="暂无情绪分布数据" :image-size="76" />
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
