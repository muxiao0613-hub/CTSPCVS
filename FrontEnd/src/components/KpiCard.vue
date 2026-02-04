<template>
  <div class="kpi-card">
    <div class="kpi-icon" :style="{ backgroundColor: iconBg }">
      <el-icon :size="24"><component :is="icon" /></el-icon>
    </div>
    <div class="kpi-content">
      <div class="kpi-label">{{ label }}</div>
      <div class="kpi-value">
        {{ value }}
        <span v-if="unit" class="kpi-unit">{{ unit }}</span>
      </div>
      <div v-if="trend" class="kpi-trend" :class="trendClass">
        <el-icon><component :is="trendIcon" /></el-icon>
        <span>{{ trend }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'

interface Props {
  label: string
  value: string | number
  unit?: string
  icon: any
  iconBg?: string
  trend?: string
  trendUp?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  unit: '',
  iconBg: '#409eff',
  trend: '',
  trendUp: true
})

const trendIcon = computed(() => props.trendUp ? ArrowUp : ArrowDown)
const trendClass = computed(() => props.trendUp ? 'trend-up' : 'trend-down')
</script>

<style scoped>
.kpi-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.kpi-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.kpi-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.kpi-content {
  flex: 1;
}

.kpi-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.kpi-unit {
  font-size: 14px;
  font-weight: normal;
  color: #909399;
}

.kpi-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}

.trend-up {
  color: #67c23a;
}

.trend-down {
  color: #f56c6c;
}
</style>
