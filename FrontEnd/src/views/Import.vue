<template>
  <div class="import">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据导入</span>
          <el-button type="primary" @click="downloadTemplate">
            <el-icon><Download /></el-icon>
            下载模板
          </el-button>
        </div>
      </template>

      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :multiple="true"
        accept=".csv"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持上传多个CSV文件（如 speeddata_Aug.csv, speeddata_Sep.csv），每个文件不超过10MB
          </div>
        </template>
      </el-upload>

      <div v-if="selectedFiles.length > 0" class="file-info">
        <h4>已选择文件：</h4>
        <el-table :data="selectedFiles" stripe style="margin-bottom: 20px">
          <el-table-column prop="name" label="文件名" />
          <el-table-column prop="size" label="文件大小">
            <template #default="{ row }">
              {{ formatFileSize(row.size) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ $index }">
              <el-button type="danger" size="small" @click="removeFile($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button type="primary" @click="handleUpload" :loading="uploading">
          开始导入
        </el-button>
      </div>

      <div v-if="uploadProgress > 0 && uploading" class="progress-area">
        <el-progress :percentage="uploadProgress" :status="uploadProgress === 100 ? 'success' : ''" />
      </div>

      <div v-if="importJobs.length > 0" class="job-result">
        <el-divider>导入结果</el-divider>
        <el-table :data="importJobs" stripe>
          <el-table-column prop="id" label="任务ID" width="80" />
          <el-table-column prop="filename" label="文件名" width="200" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalRows" label="总行数" width="100" />
          <el-table-column prop="successRows" label="成功" width="100" />
          <el-table-column prop="failRows" label="失败" width="100" />
          <el-table-column label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-card class="data-sources">
      <template #header>
        <div class="card-header">
          <span>已注册数据源</span>
          <el-button type="primary" @click="loadDataSources" :loading="loadingSources">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      <el-table :data="dataSources" stripe v-loading="loadingSources">
        <el-table-column prop="filename" label="文件名" width="200" />
        <el-table-column prop="month" label="月份" width="100" />
        <el-table-column prop="totalRoads" label="路段数" width="100" />
        <el-table-column prop="totalDays" label="天数" width="100" />
        <el-table-column label="数据量" width="150">
          <template #default="{ row }">
            {{ (row.totalRoads * row.totalDays * 144).toLocaleString() }} 条
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="recent-jobs">
      <template #header>
        <div class="card-header">
          <span>最近导入任务</span>
        </div>
      </template>
      <el-table :data="recentJobs" stripe v-loading="loadingJobs">
        <el-table-column prop="id" label="任务ID" width="80" />
        <el-table-column prop="filename" label="文件名" width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalRows" label="总行数" width="100" />
        <el-table-column prop="successRows" label="成功" width="100" />
        <el-table-column prop="failRows" label="失败" width="100" />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, UploadFilled, Refresh } from '@element-plus/icons-vue'
import api from '@/api'
import type { ImportJob, DataSource } from '@/types'
import dayjs from 'dayjs'

const uploadRef = ref()
const selectedFiles = ref<File[]>([])
const uploading = ref(false)
const uploadProgress = ref(0)
const importJobs = ref<ImportJob[]>([])
const recentJobs = ref<ImportJob[]>([])
const dataSources = ref<DataSource[]>([])
const loadingJobs = ref(false)
const loadingSources = ref(false)

const handleFileChange = (file: any) => {
  if (!selectedFiles.value.find(f => f.name === file.name)) {
    selectedFiles.value.push(file.raw)
  }
}

const removeFile = (index: number) => {
  selectedFiles.value.splice(index, 1)
}

const handleUpload = async () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  uploadProgress.value = 0
  importJobs.value = []
  
  try {
    const jobIds = await api.import.uploadBatch(selectedFiles.value)
    
    uploadProgress.value = 100
    
    for (const jobId of jobIds) {
      await pollJobStatus(jobId)
    }
    
    ElMessage.success('导入完成')
    loadRecentJobs()
    loadDataSources()
  } catch (error) {
    ElMessage.error('导入失败')
  } finally {
    uploading.value = false
    selectedFiles.value = []
  }
}

const pollJobStatus = async (jobId: number) => {
  const maxAttempts = 30
  let attempts = 0
  
  while (attempts < maxAttempts) {
    const job = await api.import.getJob(jobId)
    
    if (!importJobs.value.find(j => j.id === job.id)) {
      importJobs.value.push(job)
    }
    
    if (job.status === 'COMPLETED' || job.status === 'FAILED') {
      break
    }
    
    await new Promise(resolve => setTimeout(resolve, 1000))
    attempts++
  }
}

const downloadTemplate = async () => {
  try {
    const template = await api.import.getTemplate()
    const blob = new Blob([template], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = 'speed_template.csv'
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('模板下载成功')
  } catch (error) {
    ElMessage.error('模板下载失败')
  }
}

const loadDataSources = async () => {
  loadingSources.value = true
  try {
    dataSources.value = await api.import.getDataSources()
  } catch (error) {
    ElMessage.error('加载数据源失败')
  } finally {
    loadingSources.value = false
  }
}

const loadRecentJobs = async () => {
  loadingJobs.value = true
  try {
    const data = await api.predict.getJobs({ page: 0, size: 10 })
    recentJobs.value = data.content
  } catch (error) {
    ElMessage.error('加载导入任务失败')
  } finally {
    loadingJobs.value = false
  }
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const formatTime = (timestamp: number) => {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
}

const getStatusTagType = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'PROCESSING':
      return 'warning'
    default:
      return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'PENDING':
      return '待处理'
    case 'PROCESSING':
      return '处理中'
    case 'COMPLETED':
      return '已完成'
    case 'FAILED':
      return '失败'
    default:
      return '未知'
  }
}

loadRecentJobs()
loadDataSources()
</script>

<style scoped>
.import {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.upload-area {
  margin-bottom: 20px;
}

.file-info {
  margin-top: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.file-info h4 {
  margin: 0 0 15px 0;
  color: #606266;
}

.progress-area {
  margin-top: 20px;
}

.job-result {
  margin-top: 30px;
}

.data-sources {
  margin-top: 20px;
}

.recent-jobs {
  margin-top: 20px;
}
</style>
