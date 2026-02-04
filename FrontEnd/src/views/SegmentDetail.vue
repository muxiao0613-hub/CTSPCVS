<template>
  <div class="segment-detail">
    <el-card class="info-card">
      <template #header>
        <div class="card-header">
          <span>路段信息</span>
          <el-button @click="goBack" size="small">返回</el-button>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="路段ID">{{ segment?.segmentId }}</el-descriptions-item>
        <el-descriptions-item label="路段名称">{{ segment?.name }}</el-descriptions-item>
        <el-descriptions-item label="区域">{{ segment?.region }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>历史速度趋势</span>
          <el-radio-group v-model="timeRange" size="small">
            <el-radio-button label="24h">近24小时</el-radio-button>
            <el-radio-button label="7d">近7天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <Chart :option="historyChartOption" height="350px" />
    </el-card>

    <el-card class="prediction-card">
      <template #header>
        <div class="card-header">
          <span>预测</span>
        </div>
      </template>
      <el-form :inline="true" :model="predictForm" class="predict-form">
        <el-form-item label="基准时间">
          <el-date-picker
            v-model="predictForm.baseTime"
            type="datetime"
            placeholder="选择时间"
            format="YYYY-MM-DD HH:mm"
            value-format="X"
          />
        </el-form-item>
        <el-form-item label="预测步数">
          <el-input-number v-model="predictForm.horizonSteps" :min="1" :max="100" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handlePredict" :loading="predicting">
            开始预测
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="predictionResult" class="prediction-result">
        <Chart :option="predictionChartOption" height="350px" />
      </div>
    </el-card>

    <el-card class="jobs-card">
      <template #header>
        <div class="card-header">
          <span>预测任务历史</span>
        </div>
      </template>
      <el-table :data="predictionJobs" stripe>
        <el-table-column prop="id" label="任务ID" width="80" />
        <el-table-column label="基准时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.baseTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="horizonSteps" label="预测步数" width="100" />
        <el-table-column prop="predictorType" label="预测器" width="120" />
        <el-table-column prop="costMs" label="耗时(ms)" width="100" />
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewJob(row)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Chart from '@/components/Chart.vue'
import api from '@/api'
import type { RoadSegment, SpeedRecord, PredictionJob, PredictRequest } from '@/types'
import type { EChartsOption } from 'echarts'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const segmentId = ref<number>(parseInt(route.params.id as string))
const segment = ref<RoadSegment>()
const speedRecords = ref<SpeedRecord[]>([])
const timeRange = ref('24h')
const predictionJobs = ref<PredictionJob[]>([])
const predicting = ref(false)
const predictionResult = ref<PredictionJob>()

const predictForm = ref<PredictRequest>({
  segmentId: segmentId.value,
  baseTime: Math.floor(Date.now() / 1000),
  horizonSteps: 6
})

const loadSegment = async () => {
  try {
    segment.value = await api.segments.getById(segmentId.value)
  } catch (error) {
    ElMessage.error('加载路段信息失败')
  }
}

const loadSpeedRecords = async () => {
  try {
    const now = Date.now()
    let from: number
    
    if (timeRange.value === '24h') {
      from = now - 24 * 60 * 60 * 1000
    } else {
      from = now - 7 * 24 * 60 * 60 * 1000
    }
    
    speedRecords.value = await api.speeds.getList({
      segmentId: segmentId.value,
      from,
      to: now
    })
  } catch (error) {
    ElMessage.error('加载速度数据失败')
  }
}

const loadPredictionJobs = async () => {
  try {
    const data = await api.predict.getJobsBySegment(segmentId.value, { page: 0, size: 10 })
    predictionJobs.value = data.content
  } catch (error) {
    ElMessage.error('加载预测任务失败')
  }
}

const historyChartOption = computed<EChartsOption>(() => {
  const times = speedRecords.value.map(r => {
    const date = new Date(r.ts)
    return timeRange.value === '24h' 
      ? `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
      : dayjs(date).format('MM-DD HH:mm')
  })
  const speeds = speedRecords.value.map(r => r.speed)

  return {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: times
    },
    yAxis: {
      type: 'value',
      name: '速度 (km/h)'
    },
    series: [
      {
        name: '速度',
        type: 'line',
        data: speeds,
        smooth: true,
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
            ]
          }
        },
        lineStyle: { color: '#409eff' }
      }
    ]
  }
})

const predictionChartOption = computed<EChartsOption>(() => {
  if (!predictionResult.value) return {}

  const historicalTimes = speedRecords.value.slice(-12).map(r => {
    const date = new Date(r.ts)
    return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  })
  const historicalSpeeds = speedRecords.value.slice(-12).map(r => r.speed)

  const predictionTimes = predictionResult.value.points.map(p => {
    const date = new Date(p.ts)
    return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  })
  const predictionSpeeds = predictionResult.value.points.map(p => p.predictedSpeed)

  return {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['历史', '预测']
    },
    xAxis: {
      type: 'category',
      data: [...historicalTimes, ...predictionTimes]
    },
    yAxis: {
      type: 'value',
      name: '速度 (km/h)'
    },
    series: [
      {
        name: '历史',
        type: 'line',
        data: [...historicalSpeeds, ...new Array(predictionTimes.length).fill(null)],
        smooth: true,
        lineStyle: { color: '#409eff' }
      },
      {
        name: '预测',
        type: 'line',
        data: [...new Array(historicalTimes.length - 1).fill(null), historicalSpeeds[historicalSpeeds.length - 1], ...predictionSpeeds],
        smooth: true,
        lineStyle: { 
          color: '#67c23a',
          type: 'dashed'
        }
      }
    ]
  }
})

const handlePredict = async () => {
  predicting.value = true
  try {
    const result = await api.predict.create(predictForm.value)
    predictionResult.value = result
    ElMessage.success('预测完成')
    loadPredictionJobs()
  } catch (error) {
    ElMessage.error('预测失败')
  } finally {
    predicting.value = false
  }
}

const viewJob = (job: PredictionJob) => {
  predictionResult.value = job
}

const formatTime = (timestamp: number) => {
  return dayjs(timestamp * 1000).format('YYYY-MM-DD HH:mm')
}

const goBack = () => {
  router.back()
}

const watchTimeRange = () => {
  loadSpeedRecords()
}

onMounted(() => {
  loadSegment()
  loadSpeedRecords()
  loadPredictionJobs()
})
</script>

<style scoped>
.segment-detail {
  padding: 0;
}

.info-card,
.chart-card,
.prediction-card,
.jobs-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.predict-form {
  margin-bottom: 20px;
}

.prediction-result {
  margin-top: 20px;
}
</style>
