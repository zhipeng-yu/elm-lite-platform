<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header>
        <h2>登录</h2>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="密码"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-alert
          v-if="errorMsg"
          type="error"
          :closable="false"
          show-icon
          :title="errorMsg"
          class="error"
        />
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            登录
          </el-button>
          <el-button @click="router.push('/register')">去注册</el-button>
        </el-form-item>
      </el-form>
      <nav class="role-links" aria-label="其他身份登录">
        <router-link to="/merchant/login">商家登录</router-link>
        <router-link to="/rider/login">骑手登录</router-link>
      </nav>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { login } from '@/api/user'
import { setToken } from '@/utils/auth'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleSubmit() {
  if (submitting.value) return
  errorMsg.value = ''
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    const data = await login({ username: form.username, password: form.password })
    setToken(data.accessToken)
    ElMessage.success('登录成功')
    const redirect = router.currentRoute.value.query.redirect
    router.push(typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//') && !redirect.startsWith('/merchant') ? redirect : '/home')
  } catch (error) {
    // 401 时拦截器统一提示“账号或密码错误”，这里保留页面内错误状态
    errorMsg.value = error.response?.data?.msg || '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.auth-card {
  width: 420px;
}

.error {
  margin-bottom: 16px;
}

.role-links {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.role-links a {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  color: var(--brand);
  font-size: 14px;
}
</style>
