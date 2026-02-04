<template>
  <el-container class="app-container">
    <el-aside width="240px" class="sidebar">
      <div class="logo">
        <el-icon><Location /></el-icon>
        <span>交通流量预测</span>
      </div>
      <el-menu
        :default-active="currentRoute"
        router
        background-color="#1a1a2e"
        text-color="#a0a0b0"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>交通态势大屏</span>
        </el-menu-item>
        <el-menu-item index="/segments">
          <el-icon><MapLocation /></el-icon>
          <span>路段管理</span>
        </el-menu-item>
        <el-menu-item index="/import">
          <el-icon><Upload /></el-icon>
          <span>数据导入</span>
        </el-menu-item>
        <el-menu-item index="/jobs">
          <el-icon><Document /></el-icon>
          <span>预测任务</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">城市区域交通流量预测系统</div>
        <div class="header-info">
          <el-tag type="success">在线</el-tag>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const currentRoute = computed(() => route.path)
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.sidebar {
  background-color: #1a1a2e;
  border-right: none;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  gap: 10px;
  border-bottom: 1px solid #2a2a4e;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
