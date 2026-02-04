<template>
  <div class="jobs">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预测任务</span>
        </div>
      </template>

      <el-table :data="jobs" stripe v-loading="loading">
        <el-table-column prop="id" label="任务ID" width="80" />
        <el-table-column prop="segmentName" label="路段名称" width="150" />
        <el-table-column label="基准时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.baseTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="horizonSteps" label="预测步数" width="100" />
        <el-table-column prop="predictorType" label="预测器" width="120" />
        <el-table-column prop="costMs" label="耗时(ms)" width="100" />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="120">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        class="pagination"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="预测任务详情" width="80%">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务ID">{{ currentJob?.id }}</el-descriptions-item>
        <el-descriptions-item label="路段名称">{{ currentJob?.segmentName }}</el-descriptions-item>
        <el-descriptions-item label="基准时间">{{ formatTime(currentJob?.baseTime) }}</el-descriptions-item>
        <el-descriptions-item label="预测步数">{{ currentJob?.horizonSteps }}</el-descriptions-item>
        <el-descriptions-item label="预测器">{{ currentJob?.predictorType }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentJob?.costMs }} ms</el-descriptions-item>
      </el-descriptions>

      <el-divider>预测结果</el-divider>

      <el-table :data="currentJob?.points" stripe max-height="400">
        <el-table-column label="时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.ts) }}
          </template>
        </el-table-column>
        <el-table-column prop="predictedSpeed" label="预测速度" width="120">
          <template #default="{ row }">
            {{ row.predictedSpeed?.toFixed(1) }} km/h
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

      <template #footer>
        <el-button @click="exportResult">导出CSV</el-button>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import type { PredictionJob } from '@/types'
import dayjs from 'dayjs'

const jobs = ref<PredictionJob[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const currentJob = ref<PredictionJob>()

const pagination = ref({
  page: 0,
  size: 10,
  total: 0
})

const loadJobs = async () => {
  loading.value = true
  try {
    const data = await api.predict.getJobs({
      page: pagination.value.page,
      size: pagination.value.size
    })
    jobs.value = data.content
    pagination.value.total = data.totalElements
  } catch (error) {
    ElMessage.error('加载预测任务失败')
  } finally {
    loading.value = false
  }
}

const handleView = (job: PredictionJob) => {
  currentJob.value = job
  detailVisible.value = true
}

const exportResult = () => {
  if (!currentJob.value) return

  const headers = ['时间', '预测速度', '拥堵等级']
  const rows = currentJob.value.points.map(p => [
    formatTime(p.ts),
    p.predictedSpeed.toFixed(1),
    getCongestionText(p.congestionLevel)
  ])

  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `prediction_${currentJob.value.id}.csv`
  link.click()
  URL.revokeObjectURL(link.href)

  ElMessage.success('导出成功')
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
  pagination.value.page = 0
  loadJobs()
}

const handlePageChange = (page: number) => {
  pagination.value.page = page - 1
  loadJobs()
}

const formatTime = (timestamp?: number) => {
  if (!timestamp) return '-'
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
}

const getCongestionTagType = (level: string) => {
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

const getCongestionText = (level: string) => {
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

onMounted(() => {
  loadJobs()
})
</script>

<style scoped>
.jobs {
  padding: 0;
}

.card-header {
  font-weight: bold;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
