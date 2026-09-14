<template>
  <div class="orders">
    <h2>我的订单</h2>
    <div v-loading="loading" class="content">
      <div v-if="errorMsg" class="error-state">
        <el-alert type="error" :closable="false" show-icon :title="errorMsg" />
        <el-button :disabled="loading" @click="load">重新加载</el-button>
      </div>
      <el-empty v-else-if="orders.length === 0" description="还没有订单">
        <el-button type="primary" @click="router.push('/shops')">去点餐</el-button>
      </el-empty>
      <ul v-else class="order-list">
        <li
          v-for="order in orders"
          :key="order.id"
          role="link"
          tabindex="0"
          @keydown.enter.self="goDetail(order.id)"
          @keydown.space.self.prevent="goDetail(order.id)"
          @click="goDetail(order.id)"
        >
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
          <el-button v-if="order.orderStatus === 0" type="danger" plain size="small"
            :loading="cancelling === order.id" @click.stop="cancel(order)">取消订单</el-button>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { cancelOrder, fetchOrders } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  formatPriceCent,
  ORDER_STATUS_TAG,
  ORDER_STATUS_TEXT
} from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const orders = ref([])
const cancelling = ref(null)

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

async function cancel(order) {
  try {
    await ElMessageBox.confirm('取消后将恢复商品库存，确定继续吗？', '取消订单', { type: 'warning' })
    cancelling.value = order.id
    const updated = await cancelOrder(order.id)
    Object.assign(order, updated)
    ElMessage.success('订单已取消')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') errorMsg.value = error.response?.data?.msg || '取消失败，请刷新后重试'
  } finally { cancelling.value = null }
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

.error-state {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.error-state .el-alert {
  flex: 1;
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

.order-list li:hover,
.order-list li:focus-visible {
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
