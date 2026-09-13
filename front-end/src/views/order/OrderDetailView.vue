<template>
  <div class="order-detail">
    <div class="header">
      <el-button link @click="router.push('/orders')">← 返回订单列表</el-button>
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
      <el-empty v-else-if="notFound" description="订单不存在" class="empty">
        <el-button type="primary" @click="router.push('/orders')">返回订单列表</el-button>
      </el-empty>
      <template v-else-if="order">
        <div class="order-head">
          <h2>{{ order.orderNo }}</h2>
          <el-tag :type="statusTag(order.orderStatus)">
            {{ statusText(order.orderStatus) }}
          </el-tag>
        </div>
        <p class="time">下单时间：{{ order.createdAt }}</p>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="收货人">
            {{ order.receiverName }} {{ order.receiverPhone }}
          </el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ order.deliveryAddress }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ order.remark || '无' }}</el-descriptions-item>
        </el-descriptions>
        <h3 class="items-title">商品明细</h3>
        <el-table :data="order.items" border>
          <el-table-column prop="productName" label="商品" />
          <el-table-column label="单价">
            <template #default="{ row }">¥{{ formatPriceCent(row.unitPriceCent) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计">
            <template #default="{ row }">¥{{ formatPriceCent(row.subtotalCent) }}</template>
          </el-table-column>
        </el-table>
        <div class="summary">
          <p>商品小计：¥{{ formatPriceCent(order.productAmountCent) }}</p>
          <p>配送费：¥{{ formatPriceCent(order.deliveryFeeCent) }}</p>
          <p class="total">合计：¥{{ formatPriceCent(order.totalAmountCent) }}</p>
        </div>
        <el-button v-if="order.orderStatus === 0" type="danger" plain :loading="cancelling" @click="cancel">取消订单</el-button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { cancelOrder, fetchOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  formatPriceCent,
  ORDER_STATUS_TAG,
  ORDER_STATUS_TEXT
} from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const notFound = ref(false)
const errorMsg = ref('')
const order = ref(null)
const cancelling = ref(false)

function statusText(status) {
  return ORDER_STATUS_TEXT[status] || '未知'
}

function statusTag(status) {
  return ORDER_STATUS_TAG[status] || 'info'
}

async function load() {
  loading.value = true
  notFound.value = false
  errorMsg.value = ''
  try {
    order.value = await fetchOrder(route.params.id)
  } catch (error) {
    order.value = null
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

async function cancel() {
  try {
    await ElMessageBox.confirm('取消后将恢复商品库存，确定继续吗？', '取消订单', { type: 'warning' })
    cancelling.value = true
    order.value = await cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') errorMsg.value = error.response?.data?.msg || '取消失败，请刷新后重试'
  } finally { cancelling.value = false }
}
</script>

<style scoped>
.order-detail {
  padding: 8px 0;
}

.header {
  margin-bottom: 8px;
}

.content {
  min-height: 160px;
}

.error {
  margin-bottom: 12px;
}

.order-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.order-head h2 {
  flex: 1;
  min-width: 0;
  word-break: break-all;
  font-size: 18px;
}

.time {
  color: #909399;
  margin-bottom: 12px;
}

.items-title {
  margin: 16px 0 8px;
}

.summary {
  text-align: right;
  margin-top: 12px;
  color: #606266;
}

.summary .total {
  color: #f56c6c;
  font-size: 16px;
  font-weight: 700;
}
</style>
