<template>
  <div class="dashboard">
    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6">
        <KpiCard
          label="今日均速"
          :value="summary.todayAvgSpeed?.toFixed(1) || '0'"
          unit="km/h"
          :icon="TrendCharts"
          icon-bg="#67c23a"
        />
      </el-col>
      <el-col :span="6">
        <KpiCard
          label="拥堵路段"
          :value="summary.congestedSegments || 0"
          :icon="Warning"
          icon-bg="#f56c6c"
        />
      </el-col>
      <el-col :span="6">
        <KpiCard
          label="最拥堵路段"
          :value="summary.mostCongestedSegment?.name || '无'"
          :icon="Location"
          icon-bg="#e6a23c"
        />
      </el-col>
      <el-col :span="6">
        <KpiCard
          label="监测路段总数"
          :value="totalSegments"
          :icon="MapLocation"
          icon-bg="#409eff"
        />
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>拥堵等级分布</span>
            </div>
          </template>
          <Chart :option="congestionChartOption" height="350px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>路段速度趋势</span>
              <el-select v-model="selectedSegmentId" placeholder="选择路段" size="small" style="width: 200px">
                <el-option
                  v-for="seg in segments"
                  :key="seg.id"
                  :label="seg.name"
                  :value="seg.id"
                />
              </el-select>
            </div>
          </template>
          <Chart :option="speedChartOption" height="350px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="list-row">
      <el-col :span="24">
        <el-card class="list-card">
          <template #header>
            <div class="card-header">
              <span>拥堵Top5</span>
            </div>
          </template>
          <el-table :data="summary.topCongestedSegments" stripe>
            <el-table-column prop="roadId" label="路段ID" width="120" />
            <el-table-column prop="name" label="路段名称" width="200" />
            <el-table-column prop="avgSpeed" label="平均速度" width="120">
              <template #default="{ row }">
                {{ row.avgSpeed?.toFixed(1) }} km/h
              </template>
            </el-table-column>
            <el-table-column prop="congestionLevel" label="拥堵等级" width="120">
              <template #default="{ row }">
                <el-tag :type="getCongestionTagType(row.congestionLevel)">
                  {{ getCongestionText(row.congestionLevel) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { TrendCharts, Warning, Location, MapLocation } from '@element-plus/icons-vue'
import KpiCard from '@/components/KpiCard.vue'
import Chart from '@/components/Chart.vue'
import api from '@/api'
import type { DashboardSummary, RoadSegment, SpeedRecord, CongestionLevel } from '@/types'
import type { EChartsOption } from 'echarts'

const summary = ref<DashboardSummary>({
  todayAvgSpeed: 0,
  congestedSegments: 0,
  mostCongestedSegment: null as any,
  regionCongestions: [],
  topCongestedSegments: [],
  congestionLevelDistribution: {
    freeCount: 0,
    flowingCount: 0,
    congestedCount: 0
  }
})

const segments = ref<RoadSegment[]>([])
const selectedSegmentId = ref<number>()
const speedRecords = ref<SpeedRecord[]>([])
const totalSegments = ref(0)

const loadSummary = async () => {
  try {
    const data = await api.dashboard.getSummary()
    summary.value = data
  } catch (error) {
    ElMessage.error('加载仪表盘数据失败')
  }
}

const loadSegments = async () => {
  try {
    const data = await api.segments.getList({ page: 0, size: 100 })
    segments.value = data.content
    totalSegments.value = data.totalElements
    if (segments.value.length > 0 && !selectedSegmentId.value) {
      selectedSegmentId.value = segments.value[0].id
    }
  } catch (error) {
    ElMessage.error('加载路段列表失败')
  }
}

const loadSpeedRecords = async () => {
  if (!selectedSegmentId.value) return
  
  try {
    const now = Date.now()
    const oneDayAgo = now - 24 * 60 * 60 * 1000
    speedRecords.value = await api.speeds.getList({
      segmentId: selectedSegmentId.value,
      from: oneDayAgo,
      to: now
    })
  } catch (error) {
    ElMessage.error('加载速度数据失败')
  }
}

const congestionChartOption = computed<EChartsOption>(() => {
  const distribution = summary.value.congestionLevelDistribution
  
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [
      {
        name: '拥堵等级',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          {
            value: distribution.freeCount,
            name: '畅通',
            itemStyle: { color: '#67c23a' }
          },
          {
            value: distribution.flowingCount,
            name: '缓行',
            itemStyle: { color: '#e6a23c' }
          },
          {
            value: distribution.congestedCount,
            name: '拥堵',
            itemStyle: { color: '#f56c6c' }
          }
        ]
      }
    ]
  }
})

const speedChartOption = computed<EChartsOption>(() => {
  const times = speedRecords.value.map(r => {
    const date = new Date(r.ts)
    return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
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

const getCongestionTagType = (level: CongestionLevel) => {
  switch (level) {
    case 'FREE':
      return 'success'
    case 'FLOWING':
      return 'warning'
    case 'CONGESTED':
      return 'danger'
    default:
      return 'info'
  }
}

const getCongestionText = (level: CongestionLevel) => {
  switch (level) {
    case 'FREE':
      return '畅通'
    case 'FLOWING':
      return '缓行'
    case 'CONGESTED':
      return '拥堵'
    default:
      return '未知'
  }
}

watch(selectedSegmentId, loadSpeedRecords)

onMounted(() => {
  loadSummary()
  loadSegments()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.kpi-row {
  margin-bottom: 20px;
}

.chart-row,
.list-row {
  margin-bottom: 20px;
}

.chart-card,
.list-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
</style>
