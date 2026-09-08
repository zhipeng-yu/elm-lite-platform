<template>
  <section class="surface-card narrow-panel">
    <p class="eyebrow">我的账户</p><h1>个人信息</h1>
    <p v-if="loading" role="status">正在加载个人信息…</p>
    <p v-if="error" role="alert" class="error-text">{{ error }} <button class="secondary" @click="load">重试</button></p>
    <form v-if="profile" class="form-grid" @submit.prevent="save">
      <label>用户名<input :value="profile.username" disabled></label>
      <label>昵称<input v-model.trim="displayName" required maxlength="50"></label>
      <button class="primary" :disabled="saving">{{ saving ? '保存中…' : '保存个人信息' }}</button>
    </form>
  </section>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '@/api/user'
const profile = ref(null), displayName = ref(''), loading = ref(false), saving = ref(false), error = ref('')
async function load() {
  loading.value = true; error.value = ''
  try { profile.value = await getProfile(); displayName.value = profile.value.displayName }
  catch (e) { error.value = e.response?.data?.msg || '加载失败' }
  finally { loading.value = false }
}
async function save() {
  if (saving.value) return
  saving.value = true; error.value = ''
  try { profile.value = await updateProfile({ displayName: displayName.value }); ElMessage.success('个人信息已保存') }
  catch (e) { error.value = e.response?.data?.msg || '保存失败' }
  finally { saving.value = false }
}
onMounted(load)
</script>
