<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header>
        <h2>注册</h2>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="2-20 个字符" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="长度至少为 8 位"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="displayName">
          <el-input v-model="form.displayName" placeholder="1-20 个字符" />
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
            注册
          </el-button>
          <el-button @click="router.push('/login')">已有账号，去登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { registerUser } from '@/api/user'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: '',
  password: '',
  displayName: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '长度至少为 8 位', trigger: 'blur' }
  ],
  displayName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 20, message: '昵称最长 20 个字符', trigger: 'blur' }
  ]
}

async function handleSubmit() {
  errorMsg.value = ''
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    await registerUser({
      username: form.username,
      password: form.password,
      displayName: form.displayName
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    // 用户名重复等业务错误由拦截器统一提示，这里保留页面内错误状态
    errorMsg.value = error.response?.data?.msg || '注册失败，请稍后重试'
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
</style>
