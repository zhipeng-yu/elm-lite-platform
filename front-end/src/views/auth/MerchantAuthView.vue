<template>
  <main class="auth-shell">
    <section class="surface-card auth-panel">
      <p class="eyebrow">ELM LITE · 商家服务</p>
      <h1>{{ registering ? '注册商家账号' : '欢迎回到你的店铺' }}</h1>
      <p class="muted">管理店铺、分类与商品，让每一份美味准时上架。</p>
      <form class="form-grid" @submit.prevent="submit">
        <label>商家账号<input v-model.trim="form.account" required maxlength="50" autocomplete="username"></label>
        <label>密码<input v-model="form.password" required type="password" minlength="8" maxlength="72" :autocomplete="registering ? 'new-password' : 'current-password'"></label>
        <template v-if="registering">
          <label>商家名称<input v-model.trim="form.merchantName" required maxlength="100"></label>
          <label>联系人<input v-model.trim="form.contactName" required maxlength="50"></label>
          <label>联系电话<input v-model.trim="form.contactPhone" required maxlength="20" type="tel"></label>
        </template>
        <p v-if="error" role="alert" class="error-text">{{ error }}</p>
        <button class="primary" :disabled="busy">{{ busy ? '提交中…' : registering ? '注册商家' : '商家登录' }}</button>
      </form>
      <div class="actions">
        <button class="secondary" :disabled="busy" @click="registering = !registering; error = ''">{{ registering ? '已有账号，去登录' : '还没有店铺？注册商家' }}</button>
        <router-link to="/login">用户登录</router-link>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginMerchant, registerMerchant } from '@/api/merchant'
import { setToken } from '@/utils/auth'
const router = useRouter()
const registering = ref(false), busy = ref(false), error = ref('')
const form = reactive({ account: '', password: '', merchantName: '', contactName: '', contactPhone: '' })
async function submit() {
  if (busy.value) return
  busy.value = true; error.value = ''
  try {
    if (registering.value) {
      await registerMerchant({ ...form })
      registering.value = false
      ElMessage.success('注册成功，请登录')
    } else {
      const result = await loginMerchant({ account: form.account, password: form.password })
      setToken(result.accessToken, 'MERCHANT')
      await router.push('/merchant')
    }
  } catch (e) { error.value = e.response?.data?.msg || '提交失败，请稍后重试' }
  finally { busy.value = false }
}
</script>
