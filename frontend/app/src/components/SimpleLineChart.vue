<template>
  <view class="chart-wrap" :style="{ height: `${height}px` }">
    <canvas
      :id="canvasId"
      :canvas-id="canvasId"
      class="chart-canvas"
      :style="{ height: `${height}px` }"
    />
  </view>
</template>

<script setup lang="ts">
import { getCurrentInstance, nextTick, onMounted, watch } from 'vue'
import { onReady } from '@dcloudio/uni-app'
import type { ChartPoint } from '@/types/chart'

const props = withDefaults(
  defineProps<{
    points: ChartPoint[]
    color?: string
    unit?: string
    height?: number
    valueDigits?: number
    emptyText?: string
  }>(),
  {
    color: '#0f9d8a',
    unit: '',
    height: 190,
    valueDigits: 1,
    emptyText: '暂无数据',
  },
)

let sequence = 0
const canvasId = `health-line-${Date.now()}-${sequence++}`
const instance = getCurrentInstance()
let retryCount = 0

function formatValue(value: number): string {
  const text = value.toFixed(props.valueDigits)
  return text.replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1')
}

function drawEmpty(ctx: UniApp.CanvasContext, width: number, height: number): void {
  ctx.setFillStyle('#94a3b8')
  ctx.setFontSize(12)
  ctx.setTextAlign('center')
  ctx.fillText(props.emptyText, width / 2, height / 2)
  ctx.draw()
}

function draw(): void {
  const query = uni.createSelectorQuery().in(instance?.proxy as never)
  query
    .select(`#${canvasId}`)
    .boundingClientRect((rect) => {
      const size = rect as UniApp.NodeInfo | null
      if (!size || !size.width || !size.height) {
        if (retryCount < 3) {
          retryCount += 1
          setTimeout(draw, 120)
        }
        return
      }

      const width = size.width
      const height = size.height
      const ctx = uni.createCanvasContext(
        canvasId,
        instance?.proxy as never,
      ) as unknown as UniApp.CanvasContext
      ctx.clearRect(0, 0, width, height)

      const values = props.points
        .map((item) => item.value)
        .filter((value): value is number => value != null && !Number.isNaN(Number(value)))

      if (!values.length) {
        drawEmpty(ctx, width, height)
        return
      }

      const padding = { top: 18, right: 14, bottom: 28, left: 42 }
      const chartWidth = width - padding.left - padding.right
      const chartHeight = height - padding.top - padding.bottom

      let min = Math.min(...values)
      let max = Math.max(...values)
      if (min === max) {
        const offset = Math.abs(min) * 0.1 || 1
        min -= offset
        max += offset
      } else {
        const range = max - min
        min -= range * 0.12
        max += range * 0.12
      }

      ctx.setFontSize(10)
      ctx.setTextAlign('right')
      ctx.setStrokeStyle('#eef2f1')
      ctx.setLineWidth(1)

      const gridCount = 4
      for (let index = 0; index <= gridCount; index += 1) {
        const y = padding.top + (chartHeight * index) / gridCount
        const labelValue = max - ((max - min) * index) / gridCount
        ctx.beginPath()
        ctx.moveTo(padding.left, y)
        ctx.lineTo(width - padding.right, y)
        ctx.stroke()
        ctx.setFillStyle('#94a3b8')
        ctx.fillText(formatValue(labelValue), padding.left - 8, y + 3)
      }

      const count = props.points.length
      const step = count > 1 ? chartWidth / (count - 1) : 0
      const xFor = (index: number): number =>
        count > 1 ? padding.left + step * index : padding.left + chartWidth / 2
      const yFor = (value: number): number =>
        padding.top + ((max - value) / (max - min)) * chartHeight

      // 折线（遇到空值断开）
      ctx.setStrokeStyle(props.color)
      ctx.setLineWidth(2.5)
      ctx.setLineCap('round')
      ctx.setLineJoin('round')
      let started = false
      props.points.forEach((point, index) => {
        if (point.value == null || Number.isNaN(Number(point.value))) {
          started = false
          return
        }
        const x = xFor(index)
        const y = yFor(Number(point.value))
        if (!started) {
          ctx.beginPath()
          ctx.moveTo(x, y)
          started = true
        } else {
          ctx.lineTo(x, y)
        }
      })
      ctx.stroke()

      // 数据点
      ctx.setFillStyle(props.color)
      props.points.forEach((point, index) => {
        if (point.value == null || Number.isNaN(Number(point.value))) return
        if (count > 20 && index % 3 !== 0 && index !== count - 1) return
        ctx.beginPath()
        ctx.arc(xFor(index), yFor(Number(point.value)), 3, 0, Math.PI * 2)
        ctx.fill()
      })

      // X 轴标签
      const labelIndexes = new Set<number>()
      if (count <= 4) {
        for (let index = 0; index < count; index += 1) labelIndexes.add(index)
      } else {
        labelIndexes.add(0)
        labelIndexes.add(Math.round((count - 1) / 3))
        labelIndexes.add(Math.round(((count - 1) * 2) / 3))
        labelIndexes.add(count - 1)
      }
      ctx.setFillStyle('#94a3b8')
      ctx.setFontSize(10)
      ctx.setTextAlign('center')
      labelIndexes.forEach((index) => {
        const point = props.points[index]
        if (!point) return
        const label = point.label.length > 6 ? point.label.slice(-5) : point.label
        ctx.fillText(label, xFor(index), height - 8)
      })

      if (props.unit) {
        ctx.setTextAlign('left')
        ctx.setFillStyle('#b0bfba')
        ctx.fillText(props.unit, padding.left, 10)
      }

      ctx.draw()
    })
    .exec()
}

function scheduleDraw(): void {
  retryCount = 0
  nextTick(() => {
    setTimeout(draw, 60)
  })
}

onMounted(scheduleDraw)
onReady(scheduleDraw)

watch(
  () => props.points,
  () => scheduleDraw(),
  { deep: true },
)
</script>

<style scoped>
.chart-wrap {
  width: 100%;
  position: relative;
}

.chart-canvas {
  width: 100%;
}
</style>
