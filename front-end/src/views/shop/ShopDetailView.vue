<template>
  <div class="shop-detail">
    <div class="header">
      <el-button link @click="router.back()">← 返回</el-button>
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
      <el-empty v-else-if="notFound" description="店铺不存在" class="empty">
        <el-button type="primary" @click="router.push('/shops')">返回店铺列表</el-button>
      </el-empty>
      <template v-else-if="shop">
        <div class="shop-head">
          <h2>{{ shop.shopName }}</h2>
          <el-tag :type="statusTag(shop.businessStatus)">
            {{ statusText(shop.businessStatus) }}
          </el-tag>
        </div>
        <p class="desc">{{ shop.description || '暂无简介' }}</p>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="地址">{{ shop.address }}</el-descriptions-item>
          <el-descriptions-item label="起送价">
            ¥{{ formatPriceCent(shop.startPriceCent) }}
          </el-descriptions-item>
          <el-descriptions-item label="配送费">
            ¥{{ formatPriceCent(shop.deliveryPriceCent) }}
          </el-descriptions-item>
        </el-descriptions>
        <div class="actions">
          <el-button type="primary" @click="router.push(`/shops/${shop.id}/products`)">
            去点餐
          </el-button>
          <el-button @click="router.push(`/shops/${shop.id}/categories`)">
            查看分类
          </el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchShop } from '@/api/shop'
import {
  BUSINESS_STATUS_TAG,
  BUSINESS_STATUS_TEXT,
  formatPriceCent
} from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const notFound = ref(false)
const errorMsg = ref('')
const shop = ref(null)

function statusText(status) {
  return BUSINESS_STATUS_TEXT[status] || '未知'
}

function statusTag(status) {
  return BUSINESS_STATUS_TAG[status] || 'info'
}

async function load() {
  loading.value = true
  notFound.value = false
  errorMsg.value = ''
  try {
    shop.value = await fetchShop(route.params.id)
  } catch (error) {
    shop.value = null
    if (error.response?.status === 404) {
      notFound.value = true
    } else {
      errorMsg.value = error.response?.data?.msg || '加载失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.shop-detail {
  padding: 8px 0;
}

.header {
  margin-bottom: 8px;
}

.content {
  min-height: 200px;
}

.error {
  margin-bottom: 12px;
}

.shop-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.desc {
  color: #909399;
  margin-bottom: 16px;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
</style>
