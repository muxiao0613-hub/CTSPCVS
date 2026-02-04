import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue')
  },
  {
    path: '/segments',
    name: 'Segments',
    component: () => import('@/views/Segments.vue')
  },
  {
    path: '/segments/:id',
    name: 'SegmentDetail',
    component: () => import('@/views/SegmentDetail.vue')
  },
  {
    path: '/import',
    name: 'Import',
    component: () => import('@/views/Import.vue')
  },
  {
    path: '/jobs',
    name: 'Jobs',
    component: () => import('@/views/Jobs.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
