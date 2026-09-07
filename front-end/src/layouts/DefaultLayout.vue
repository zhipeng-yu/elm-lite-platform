<template>
  <el-container class="layout">
    <el-header class="layout-header">
      <div class="brand">食光 · 校园外卖</div>
      <div class="user-area">
        <span>{{ hasToken ? (isMerchant ? '商家已登录' : '用户已登录') : '未登录' }}</span>
        <el-button v-if="hasToken" link class="user-btn" @click="handleLogout">退出</el-button>
        <el-button v-else link class="user-btn" @click="router.push('/login')">去登录</el-button>
      </div>
    </el-header>
    <el-container class="layout-body">
      <el-aside width="200px" class="layout-aside">
        <el-menu router :default-active="route.path">
          <el-menu-item index="/home">首页</el-menu-item>
          <el-menu-item index="/shops">店铺</el-menu-item>
          <template v-if="!isMerchant">
            <el-menu-item index="/cart">购物车</el-menu-item>
            <el-menu-item index="/orders">订单</el-menu-item>
            <el-menu-item index="/addresses">收货地址</el-menu-item>
            <el-menu-item index="/profile">个人信息</el-menu-item>
          </template>
          <el-menu-item v-if="isMerchant" index="/merchant">商家工作台</el-menu-item>
          <el-menu-item v-if="!hasToken" index="/merchant/login">商家入口</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { getToken, getAccountType, removeToken } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const hasToken = computed(() => { route.fullPath; return Boolean(getToken()) })
const isMerchant = computed(() => { route.fullPath; return hasToken.value && getAccountType() === 'MERCHANT' })

function handleLogout() {
  const loginPath = isMerchant.value ? '/merchant/login' : '/login'
  removeToken()
  ElMessage.success('已退出登录')
  router.push(loginPath)
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #0097ff;
  color: #fff;
}

.brand {
  font-weight: 700;
  font-size: 18px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-btn {
  color: #fff;
}

.layout-body {
  height: calc(100% - 60px);
}

.layout-aside {
  border-right: 1px solid #eee;
  background-color: #fafafa;
}

.layout-main {
  background-color: #fff;
}
</style>
