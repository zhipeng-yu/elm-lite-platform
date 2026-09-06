<template>
  <div class="orders">
    <h2>我的订单</h2>
    <div v-loading="loading" class="content">
      <el-alert
        v-if="errorMsg"
        type="error"
        :closable="false"
        show-icon
        :title="errorMsg"
        class="error"
      />
      <el-empty v-else-if="orders.length === 0" description="还没有订单">
        <el-button type="primary" @click="router.push('/shops')">去点餐</el-button>
      </el-empty>
      <ul v-else class="order-list">
        <li v-for="order in orders" :key="order.id" @click="goDetail(order.id)">
          <div class="order-head">
            <span class="order-no">{{ order.orderNo }}</span>
            <el-tag :type="statusTag(order.orderStatus)" size="small">
              {{ statusText(order.orderStatus) }}
            </el-tag>
          </div>
          <div class="order-body">
            <span>店铺 #{{ order.shopId }}</span>
            <span class="total">¥{{ formatPriceCent(order.totalAmountCent) }}</span>
          </div>
          <p class="time">{{ order.createdAt }}</p>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchOrders } from '@/api/order'
import {
  formatPriceCent,
  ORDER_STATUS_TAG,
  ORDER_STATUS_TEXT
} from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const orders = ref([])

function statusText(status) {
  return ORDER_STATUS_TEXT[status] || '未知'
}

function statusTag(status) {
  return ORDER_STATUS_TAG[status] || 'info'
}

function goDetail(id) {
  router.push(`/orders/${id}`)
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    orders.value = await fetchOrders()
  } catch (error) {
    orders.value = []
    errorMsg.value = error.response?.data?.msg || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.orders {
  padding: 8px 0;
}

.content {
  min-height: 160px;
  margin-top: 8px;
}

.error {
  margin-bottom: 12px;
}

.order-list {
  list-style: none;
}

.order-list li {
  padding: 12px 14px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.order-list li:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-no {
  font-weight: 700;
}

.order-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0;
  color: #606266;
}

.total {
  font-weight: 700;
}

.time {
  color: #909399;
  font-size: 12px;
}
</style>
