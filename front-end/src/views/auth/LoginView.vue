<template>
  <main class="auth-shell customer-auth">
    <section class="surface-card auth-panel customer-panel">
      <header>
        <p class="eyebrow">ELM LITE · 顾客端</p>
        <h1>顾客登录</h1>
        <p class="muted">发现附近好店，轻松下单每一餐。</p>
      </header>

      <el-form
        ref="formRef"
        class="customer-form"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入密码"
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
        <el-button class="customer-submit" type="primary" :loading="submitting" @click="handleSubmit">登录</el-button>
      </el-form>

      <el-button class="customer-register" :disabled="submitting" @click="router.push('/register')">注册顾客账号</el-button>

      <nav class="role-switch" aria-label="其他身份登录">
        <span>切换登录身份</span>
        <div>
          <router-link to="/merchant/login">商家登录</router-link>
          <router-link to="/rider/login">骑手登录</router-link>
        </div>
      </nav>
    </section>
  </main>
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
.customer-auth {
  background:
    radial-gradient(circle at 50% 12%, rgba(11, 107, 203, .12), transparent 34%),
    var(--page);
}

.customer-panel {
  position: relative;
  width: min(440px, calc(100vw - 32px));
  padding: 32px;
  overflow: hidden;
  box-shadow: var(--shadow-hover);
}

.customer-panel::before {
  position: absolute;
  inset: 0 0 auto;
  height: 4px;
  background: var(--brand);
  content: '';
}

.customer-panel h1 { margin: 8px 0; }
.customer-form { margin-top: 24px; }
.error { margin-bottom: 18px; }
.customer-submit, .customer-register { width: 100%; min-height: 44px; margin-left: 0; }
.customer-register { margin-top: 12px; }

:deep(.customer-form .el-form-item) { margin-bottom: 18px; }
:deep(.customer-form .el-form-item__label) { height: auto; margin-bottom: 8px; font-size: 14px; font-weight: 600; line-height: 1.5; }
:deep(.customer-form .el-input__wrapper) { min-height: 44px; border-radius: var(--radius-control); box-shadow: 0 0 0 1px var(--line) inset; }

.role-switch { margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--line); color: var(--muted); font-size: 14px; text-align: center; }
.role-switch div { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 12px; }
.role-switch a {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-control);
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 600;
  text-decoration: none;
}
.role-switch a:hover { background: #dcecff; }

@media (max-width: 480px) {
  .customer-panel { padding: 24px 20px; }
}
</style>
