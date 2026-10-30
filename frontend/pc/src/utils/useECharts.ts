import { onBeforeUnmount, onMounted, type Ref } from 'vue'
import echarts from './echarts'
import type { EChartsOption } from 'echarts'

/**
 * ECharts 实例生命周期封装：
 * - 容器尺寸变化时自动 resize；
 * - 组件卸载时 disconnect observer 并 dispose；
 * - 数据先于容器挂载时暂存 option，挂载后立即渲染。
 */
export function useECharts(container: Ref<HTMLDivElement | undefined>) {
  let chart: ReturnType<typeof echarts.init> | null = null
  let observer: ResizeObserver | null = null
  let pendingOption: EChartsOption | null = null

  function init(): void {
    if (chart || !container.value) return
    chart = echarts.init(container.value)
    if (pendingOption) {
      chart.setOption(pendingOption, true)
    }
    observer = new ResizeObserver(() => {
      chart?.resize()
    })
    observer.observe(container.value)
  }

  function setOption(option: EChartsOption): void {
    pendingOption = option
    if (!chart) init()
    chart?.setOption(option, true)
  }

  function resize(): void {
    chart?.resize()
  }

  onMounted(init)

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
    chart?.dispose()
    chart = null
    pendingOption = null
  })

  return { setOption, resize }
}
