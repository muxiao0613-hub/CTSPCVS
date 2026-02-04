<template>
  <div ref="chartRef" class="chart-container" :style="{ height: height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

interface Props {
  option: EChartsOption
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  height: '400px'
})

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(props.option)
  
  window.addEventListener('resize', handleResize)
}

const handleResize = () => {
  chartInstance?.resize()
}

const updateChart = () => {
  chartInstance?.setOption(props.option, true)
}

watch(() => props.option, updateChart, { deep: true })

onMounted(() => {
  initChart()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.chart-container {
  width: 100%;
}
</style>
