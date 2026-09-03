import { createRouter, createWebHistory } from 'vue-router'

// 基础路由：后续页面任务在自己的模块内追加，共享修改需先与龙确认。
const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/home',
    name: 'home',
    component: () => import('@/views/home/HomeView.vue')
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
