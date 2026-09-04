<template>
  <div class="shops">
    <div class="header">
      <h2>店铺列表</h2>
      <el-button :disabled="loading" @click="load">刷新</el-button>
    </div>
    <div v-loading="loading" class="content">
      <el-alert
        v-if="errorMsg"
        type="error"
        :closable="false"
        show-icon
        :title="errorMsg"
        class="error"
      />
      <el-empty v-else-if="shops.length === 0" description="暂无店铺" />
      <ul v-else class="shop-list">
        <li v-for="shop in shops" :key="shop.id" @click="goDetail(shop.id)">
          <div class="shop-name">
            {{ shop.shopName }}
            <el-tag :type="statusTag(shop.businessStatus)" size="small">
              {{ statusText(shop.businessStatus) }}
            </el-tag>
          </div>
          <p class="desc">{{ shop.description || '暂无简介' }}</p>
          <p class="meta">
            起送 ¥{{ formatPriceCent(shop.startPriceCent) }} ·
            配送 ¥{{ formatPriceCent(shop.deliveryPriceCent) }}
          </p>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchShops } from '@/api/shop'
import {
  BUSINESS_STATUS_TAG,
  BUSINESS_STATUS_TEXT,
  formatPriceCent
} from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const shops = ref([])

function statusText(status) {
  return BUSINESS_STATUS_TEXT[status] || '未知'
}

function statusTag(status) {
  return BUSINESS_STATUS_TAG[status] || 'info'
}

function goDetail(id) {
  router.push(`/shops/${id}`)
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    shops.value = await fetchShops()
  } catch (error) {
    shops.value = []
    errorMsg.value = error.response?.data?.msg || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.shops {
  padding: 8px 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.content {
  min-height: 200px;
  margin-top: 8px;
}

.error {
  margin-bottom: 12px;
}

.shop-list {
  list-style: none;
}

.shop-list li {
  padding: 14px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.shop-list li:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.shop-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
}

.desc {
  color: #909399;
  margin: 6px 0;
}

.meta {
  color: #606266;
}
</style>
