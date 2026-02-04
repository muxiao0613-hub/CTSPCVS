<template>
  <div class="segments">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>路段管理</span>
          <el-button type="primary" @click="loadFromSources">
            <el-icon><Refresh /></el-icon>
            从数据源加载
          </el-button>
        </div>
      </template>
      
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="路段ID或名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="segments" stripe v-loading="loading">
        <el-table-column prop="roadId" label="路段ID" width="120" />
        <el-table-column prop="name" label="路段名称" width="200" />
        <el-table-column label="操作" fixed="right" width="120">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import api from '@/api'
import type { RoadSegment } from '@/types'

const router = useRouter()

const searchForm = ref({
  keyword: ''
})

const segments = ref<RoadSegment[]>([])
const loading = ref(false)

const pagination = ref({
  page: 0,
  size: 10,
  total: 0
})

const loadSegments = async () => {
  loading.value = true
  try {
    const data = await api.segments.getList({
      keyword: searchForm.value.keyword || undefined,
      page: pagination.value.page,
      size: pagination.value.size
    })
    segments.value = data.content
    pagination.value.total = data.totalElements
  } catch (error) {
    ElMessage.error('加载路段列表失败')
  } finally {
    loading.value = false
  }
}

const loadFromSources = async () => {
  loading.value = true
  try {
    const data = await api.segments.getFromSources()
    ElMessage.success(`已从数据源加载 ${data.length} 个路段`)
    loadSegments()
  } catch (error) {
    ElMessage.error('从数据源加载路段失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.value.page = 0
  loadSegments()
}

const handleReset = () => {
  searchForm.value = {
    keyword: ''
  }
  pagination.value.page = 0
  loadSegments()
}

const handleViewDetail = (row: RoadSegment) => {
  router.push(`/segments/${row.id}`)
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
  pagination.value.page = 0
  loadSegments()
}

const handlePageChange = (page: number) => {
  pagination.value.page = page - 1
  loadSegments()
}

onMounted(() => {
  loadSegments()
})
</script>

<style scoped>
.segments {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
