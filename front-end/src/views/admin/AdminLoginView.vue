<template>
  <div class="admin-login-page">
    <el-card class="admin-login-card">
      <template #header>
        <h2>管理员登录</h2>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="70px"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="管理员账号" />
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
        </el-form-item>
      </el-form>
      <p v-if="isMock" class="tip">开发期模拟账号：admin / admin123456（后端就绪后失效）</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { adminLogin } from '@/api/admin'
import { setToken } from '@/utils/auth'

const isMock = import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleSubmit() {
  errorMsg.value = ''
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    const data = await adminLogin(form.username, form.password)
    setToken(data.accessToken, 'ADMIN')
    ElMessage.success('登录成功')
    router.push('/admin')
  } catch (error) {
    errorMsg.value = error.response?.data?.msg || '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.admin-login-page {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(145deg, #eef6ff, var(--page));
}

.admin-login-card {
  width: min(420px, 100%);
  border-radius: var(--radius-card);
}

.error {
  margin-bottom: 16px;
}

.tip {
  color: var(--muted);
  font-size: 13px;
}
</style>
