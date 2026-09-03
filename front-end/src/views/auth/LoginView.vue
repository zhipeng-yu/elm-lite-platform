<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <h2>登录</h2>
      </template>
      <p class="tip">
        登录接口尚未就绪（D3 接入）。D2 阶段点击“模拟登录”写入假 Token，用于验证认证头与 401 统一处理。
      </p>
      <div class="actions">
        <el-button type="primary" @click="handleMockLogin">模拟登录</el-button>
        <el-button @click="router.push('/home')">返回首页</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { setToken } from '@/utils/auth'

const router = useRouter()

function handleMockLogin() {
  // 模拟登录：只写入占位 Token，真实登录逻辑在 D3 实现
  setToken(`mock-token-${Date.now()}`)
  ElMessage.success('模拟登录成功')
  const redirect = router.currentRoute.value.query.redirect
  router.push(typeof redirect === 'string' ? redirect : '/home')
}
</script>

<style scoped>
.login-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 400px;
}

.tip {
  color: #909399;
  margin-bottom: 16px;
}

.actions {
  display: flex;
  gap: 12px;
}
</style>
