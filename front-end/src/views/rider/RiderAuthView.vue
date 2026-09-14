<template>
  <main class="auth-shell rider-auth">
    <section class="surface-card auth-panel rider-panel">
      <header>
        <p class="eyebrow">ELM LITE · 配送端</p>
        <h1>{{ registering ? '注册骑手账号' : '骑手登录' }}</h1>
        <p class="muted">接单、取餐和配送，一处完成。</p>
      </header>

      <form class="form-grid" @submit.prevent="submit">
        <label>账号<input v-model.trim="form.username" required maxlength="50" autocomplete="username"></label>
        <label>密码<input v-model="form.password" type="password" required minlength="8" maxlength="72" :autocomplete="registering ? 'new-password' : 'current-password'"></label>
        <label v-if="registering">姓名<input v-model.trim="form.displayName" required maxlength="50" autocomplete="name"></label>
        <p v-if="error" role="alert" class="error-text">{{ error }}</p>
        <button class="primary rider-submit" :disabled="busy">{{ busy ? '提交中…' : registering ? '注册' : '登录' }}</button>
      </form>

      <button class="secondary rider-toggle" type="button" :disabled="busy" @click="registering=!registering;error=''">
        {{ registering ? '已有账号，去登录' : '注册骑手账号' }}
      </button>

      <nav class="role-switch" aria-label="其他身份登录">
        <span>切换登录身份</span>
        <div>
          <router-link to="/login">顾客登录</router-link>
          <router-link to="/merchant/login">商家登录</router-link>
        </div>
      </nav>
    </section>
  </main>
</template>
<script setup>
import { reactive,ref } from 'vue'; import { useRouter } from 'vue-router'; import { loginRider,registerRider } from '@/api/rider'; import { setToken } from '@/utils/auth'
const router=useRouter(),registering=ref(false),busy=ref(false),error=ref(''); const form=reactive({username:'',password:'',displayName:''})
async function submit(){busy.value=true;error.value='';try{if(registering.value){await registerRider(form);registering.value=false}else{const r=await loginRider(form);setToken(r.accessToken,'RIDER');await router.push('/rider')}}catch(e){error.value=e.response?.data?.msg||'提交错误，请重试'}finally{busy.value=false}}
</script>

<style scoped>
.rider-auth {
  background:
    radial-gradient(circle at 50% 12%, rgba(11, 107, 203, .12), transparent 34%),
    var(--page);
}

.rider-panel {
  position: relative;
  width: min(440px, calc(100vw - 32px));
  padding: 32px;
  overflow: hidden;
  box-shadow: var(--shadow-hover);
}

.rider-panel::before {
  position: absolute;
  inset: 0 0 auto;
  height: 4px;
  background: var(--brand);
  content: '';
}

.rider-panel h1 { margin: 8px 0; }
.rider-panel .form-grid { margin-top: 24px; }
.rider-submit, .rider-toggle { width: 100%; min-height: 44px; }
.rider-toggle { margin-top: 12px; }

.role-switch {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
  color: var(--muted);
  font-size: 14px;
  text-align: center;
}

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
  .rider-panel { padding: 24px 20px; }
}
</style>
