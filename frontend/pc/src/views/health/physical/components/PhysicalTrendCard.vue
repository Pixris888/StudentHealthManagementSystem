<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  PHYSICAL_METRICS,
  getPhysicalMetric,
  physicalMetricValue,
  physicalTestLabel,
} from '@/utils/health'
import type { PhysicalMetricKey } from '@/utils/health'
import type { PhysicalTest } from '@/types/health'

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  data: PhysicalTest[]
  loading?: boolean
}>()

const selectedMetric = ref<PhysicalMetricKey>('bmi')
const yearRange = ref<[string, string] | null>(null)
const chartRef = ref<HTMLDivElement>()
let chart: ReturnType<typeof echarts.init> | null = null
let resizeObserver: ResizeObserver | null = null

const metric = computed(() => getPhysicalMetric(selectedMetric.value))
const chartData = computed(() => {
  let list = [...props.data]
  if (yearRange.value) {
    const start = Number(yearRange.value[0])
    const end = Number(yearRange.value[1])
    if (!Number.isNaN(start) && !Number.isNaN(end)) {
      list = list.filter((item) => item.testYear >= start && item.testYear <= end)
    }
  }
  return list.sort(
    (a, b) => a.testYear - b.testYear || a.id - b.id,
  )
})
const hasValues = computed(() =>
  chartData.value.some(
    (item) => physicalMetricValue(item, selectedMetric.value) != null,
  ),
)

function renderChart(): void {
  if (!chart) return
  const currentMetric = metric.value
  const data = chartData.value
  const values = data.map((item) =>
    physicalMetricValue(item, selectedMetric.value),
  )

  chart.setOption(
    {
      animationDuration: 400,
      color: [currentMetric.color],
      grid: {
        left: 10,
        right: 20,
        top: 28,
        bottom: 8,
        containLabel: true,
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 74, 64, 0.94)',
        borderWidth: 0,
        padding: [8, 12],
        textStyle: { color: '#eafff7', fontSize: 12 },
        valueFormatter: (value: unknown) =>
          value == null
            ? '未填写'
            : `${value}${currentMetric.unit ? ` ${currentMetric.unit}` : ''}`,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.map(physicalTestLabel),
        axisLine: { lineStyle: { color: '#e3ece8' } },
        axisTick: { show: false },
        axisLabel: {
          color: '#8aa198',
          fontSize: 11,
          hideOverlap: true,
        },
      },
      yAxis: {
        type: 'value',
        scale: true,
        name: currentMetric.unit,
        nameTextStyle: {
          color: '#9ab0a7',
          fontSize: 10,
          padding: [0, 0, 0, 8],
        },
        axisLabel: { color: '#8aa198', fontSize: 11 },
        splitLine: { lineStyle: { color: '#eef4f1' } },
      },
      series: [
        {
          name: currentMetric.label,
          type: 'line',
          data: values,
          smooth: true,
          connectNulls: true,
          symbol: 'circle',
          symbolSize: 7,
          showSymbol: data.length <= 20,
          lineStyle: { width: 3, color: currentMetric.color },
          itemStyle: {
            color: currentMetric.color,
            borderWidth: 2,
            borderColor: '#fff',
          },
          areaStyle: { color: currentMetric.color, opacity: 0.08 },
          emphasis: { focus: 'series' },
        },
      ],
    },
    true,
  )
}

function scheduleRender(): void {
  nextTick(renderChart)
}

watch([chartData, selectedMetric], scheduleRender)

onMounted(() => {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  renderChart()
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartRef.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  chart?.dispose()
  chart = null
})
</script>

<template>
  <section v-loading="loading" class="panel-card trend-card">
    <div class="trend-head">
      <div class="trend-title">
        <h3>健康指标趋势</h3>
        <p>
          观察历次体测指标变化
          <span v-if="metric.lowerIsBetter">，该指标数值越低越好</span>
        </p>
      </div>
      <div class="trend-controls">
        <el-select
          v-model="selectedMetric"
          class="metric-select"
          style="width: 150px"
        >
          <el-option
            v-for="item in PHYSICAL_METRICS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-date-picker
          v-model="yearRange"
          type="yearrange"
          value-format="YYYY"
          range-separator="至"
          start-placeholder="开始年份"
          end-placeholder="结束年份"
          clearable
          style="width: 240px"
        />
      </div>
    </div>

    <div class="trend-chart-wrap">
      <div ref="chartRef" class="trend-chart" />
      <div v-if="!hasValues && !loading" class="chart-empty">
        <el-empty
          :description="chartData.length ? '该指标暂无数据' : '暂无体测数据'"
          :image-size="72"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.trend-card {
  padding: 22px 24px 16px;
}

.trend-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.trend-title h3 {
  margin-bottom: 4px;
  font-size: 16px;
  font-weight: 700;
  color: var(--hp-text);
}

.trend-title p {
  font-size: 12px;
  color: var(--hp-text-muted);
}

.trend-title p span {
  color: #c58a28;
}

.trend-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.trend-chart-wrap {
  position: relative;
  min-height: 340px;
}

.trend-chart {
  width: 100%;
  height: 340px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(2px);
}

@media (max-width: 768px) {
  .trend-card {
    padding: 18px 14px 12px;
  }

  .trend-controls {
    width: 100%;
  }

  .trend-controls :deep(.el-select),
  .trend-controls :deep(.el-date-editor) {
    width: 100% !important;
  }

  .trend-chart {
    height: 280px;
  }
}
</style>
