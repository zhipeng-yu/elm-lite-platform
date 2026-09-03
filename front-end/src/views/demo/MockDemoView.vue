<template>
  <div class="demo">
    <h2>模拟数据状态验证</h2>
    <p class="desc">
      后端接口尚未就绪，通过开发期模拟接口验证加载、空数据、错误三种状态和 401 统一处理。
    </p>

    <el-card class="panel">
      <template #header>
        <div class="panel-header">
          <span>商家列表（模拟接口）</span>
          <div class="actions">
            <el-button type="primary" :disabled="loading" @click="load('list')">加载数据</el-button>
            <el-button :disabled="loading" @click="load('empty')">空数据</el-button>
            <el-button type="danger" :disabled="loading" @click="load('error')">模拟报错</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="content">
        <el-alert v-if="errorMsg" type="error" :closable="false" show-icon :title="errorMsg" />
        <el-empty v-else-if="empty" description="暂无数据" />
        <ul v-else class="merchant-list">
          <li v-for="item in merchants" :key="item.id">
            {{ item.name }}（评分 {{ item.rating }}）
          </li>
        </ul>
      </div>
    </el-card>

    <el-card class="panel">
      <template #header><span>统一鉴权验证（401）</span></template>
      <div class="content">
        <p class="desc">清除 Token 后发起请求，验证统一 401 处理：提示并跳转登录页。</p>
        <el-button type="warning" @click="handleUnauthorized">清除 Token 并发起请求</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'

import { fetchMerchants } from '@/api/merchant'
import { removeToken } from '@/utils/auth'

const loading = ref(false)
const empty = ref(false)
const errorMsg = ref('')
const merchants = ref([])

async function load(mode) {
  loading.value = true
  empty.value = false
  errorMsg.value = ''
  merchants.value = []
  try {
    const data = await fetchMerchants(mode)
    if (Array.isArray(data) && data.length === 0) {
      empty.value = true
    } else {
      merchants.value = data
    }
  } catch {
    // 统一错误提示由 Axios 拦截器弹出，这里只保留页面内错误状态
    errorMsg.value = '请求失败，页面已进入错误状态（右上角会同时弹出统一错误提示）'
  } finally {
    loading.value = false
  }
}

function handleUnauthorized() {
  removeToken()
  load('list')
}
</script>

<style scoped>
.demo {
  padding: 8px 0;
}

.desc {
  color: #909399;
  margin: 8px 0 16px;
}

.panel {
  margin-bottom: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.actions {
  display: flex;
  gap: 8px;
}

.content {
  min-height: 120px;
}

.merchant-list {
  list-style: none;
}

.merchant-list li {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
</style>
