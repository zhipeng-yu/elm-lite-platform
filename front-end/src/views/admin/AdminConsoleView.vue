<template>
  <div class="admin-console">
    <header class="console-header">
      <h2>管理员控制台</h2>
      <el-button link @click="handleLogout">退出登录</el-button>
    </header>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="用户管理" name="users">
        <div class="toolbar">
          <el-input v-model="userKeyword" placeholder="按用户名或昵称搜索" clearable class="keyword" />
          <el-select v-model="userStatus" placeholder="状态" clearable class="status">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button type="primary" @click="loadUsers">查询</el-button>
        </div>
        <el-table v-loading="loading" :data="users" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button
                link
                :type="row.status === 1 ? 'danger' : 'success'"
                @click="toggleUser(row)"
              >
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="商家管理" name="merchants">
        <div class="toolbar">
          <el-input v-model="merchantKeyword" placeholder="按账号或商家名搜索" clearable class="keyword" />
          <el-select v-model="merchantStatus" placeholder="状态" clearable class="status">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button type="primary" @click="loadMerchants">查询</el-button>
        </div>
        <el-table v-loading="loading" :data="merchants" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="account" label="账号" />
          <el-table-column prop="merchantName" label="商家名" />
          <el-table-column prop="contactName" label="联系人" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button
                link
                :type="row.status === 1 ? 'danger' : 'success'"
                @click="toggleMerchant(row)"
              >
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="店铺管理" name="shops">
        <el-table v-loading="loading" :data="shops" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="shopName" label="店铺名" />
          <el-table-column prop="merchantId" label="商家ID" width="90" />
          <el-table-column label="营业状态" width="100">
            <template #default="{ row }">
              <el-tag :type="businessTag(row.businessStatus)" size="small">
                {{ businessText(row.businessStatus) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="订单管理" name="orders">
        <div class="toolbar">
          <el-select v-model="orderStatusFilter" placeholder="订单状态" clearable class="status">
            <el-option v-for="(text, value) in ORDER_STATUS_TEXT" :key="value" :label="text" :value="Number(value)" />
          </el-select>
          <el-button type="primary" @click="loadOrders">查询</el-button>
        </div>
        <el-table v-loading="loading" :data="orders" border>
          <el-table-column prop="orderNo" label="订单号" />
          <el-table-column prop="shopId" label="店铺ID" width="90" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="ORDER_STATUS_TAG[row.orderStatus]" size="small">
                {{ ORDER_STATUS_TEXT[row.orderStatus] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="100">
            <template #default="{ row }">¥{{ formatPriceCent(row.totalAmountCent) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button link type="primary" @click="openOrder(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="orderDialogVisible" title="订单详情" width="560px">
      <div v-if="orderDetail" class="order-detail">
        <p>订单号：{{ orderDetail.orderNo }}</p>
        <p>
          状态：
          <el-tag :type="ORDER_STATUS_TAG[orderDetail.orderStatus]" size="small">
            {{ ORDER_STATUS_TEXT[orderDetail.orderStatus] }}
          </el-tag>
        </p>
        <p>收货人：{{ orderDetail.receiverName }} {{ orderDetail.receiverPhone }}</p>
        <p>收货地址：{{ orderDetail.deliveryAddress }}</p>
        <p>备注：{{ orderDetail.remark || '无' }}</p>
        <el-table :data="orderDetail.items" border size="small">
          <el-table-column prop="productName" label="商品" />
          <el-table-column label="单价">
            <template #default="{ row }">¥{{ formatPriceCent(row.unitPriceCent) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column label="小计">
            <template #default="{ row }">¥{{ formatPriceCent(row.subtotalCent) }}</template>
          </el-table-column>
        </el-table>
        <p class="amount">
          商品 ¥{{ formatPriceCent(orderDetail.productAmountCent) }} −
          优惠 ¥{{ formatPriceCent(orderDetail.discountAmountCent ?? 0) }} +
          配送 ¥{{ formatPriceCent(orderDetail.deliveryFeeCent) }} =
          <b>¥{{ formatPriceCent(orderDetail.totalAmountCent) }}</b>
        </p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  fetchAdminMerchants,
  fetchAdminOrder,
  fetchAdminOrders,
  fetchAdminShops,
  fetchAdminUsers,
  updateMerchantStatus,
  updateUserStatus
} from '@/api/admin'
import { getAccountType, getToken, removeToken } from '@/utils/auth'
import {
  BUSINESS_STATUS_TAG,
  BUSINESS_STATUS_TEXT,
  formatPriceCent,
  ORDER_STATUS_TAG,
  ORDER_STATUS_TEXT
} from '@/utils/format'

const router = useRouter()
const activeTab = ref('users')
const loading = ref(false)

const users = ref([])
const userKeyword = ref('')
const userStatus = ref(null)

const merchants = ref([])
const merchantKeyword = ref('')
const merchantStatus = ref(null)

const shops = ref([])
const orders = ref([])
const orderStatusFilter = ref(null)

const orderDialogVisible = ref(false)
const orderDetail = ref(null)

function businessText(status) {
  return BUSINESS_STATUS_TEXT[status] || '未知'
}

function businessTag(status) {
  return BUSINESS_STATUS_TAG[status] || 'info'
}

function loadUsers() {
  loading.value = true
  fetchAdminUsers({
    keyword: userKeyword.value || undefined,
    status: userStatus.value ?? undefined
  })
    .then((data) => {
      users.value = data
    })
    .catch((error) => ElMessage.error(error.response?.data?.msg || '加载失败'))
    .finally(() => {
      loading.value = false
    })
}

function loadMerchants() {
  loading.value = true
  fetchAdminMerchants({
    keyword: merchantKeyword.value || undefined,
    status: merchantStatus.value ?? undefined
  })
    .then((data) => {
      merchants.value = data
    })
    .catch((error) => ElMessage.error(error.response?.data?.msg || '加载失败'))
    .finally(() => {
      loading.value = false
    })
}

function loadShops() {
  loading.value = true
  fetchAdminShops()
    .then((data) => {
      shops.value = data
    })
    .catch((error) => ElMessage.error(error.response?.data?.msg || '加载失败'))
    .finally(() => {
      loading.value = false
    })
}

function loadOrders() {
  loading.value = true
  fetchAdminOrders({ orderStatus: orderStatusFilter.value ?? undefined })
    .then((data) => {
      orders.value = data
    })
    .catch((error) => ElMessage.error(error.response?.data?.msg || '加载失败'))
    .finally(() => {
      loading.value = false
    })
}

async function toggleUser(row) {
  const target = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确定${target === 0 ? '停用' : '启用'}用户“${row.username}”吗？`,
      '操作确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await updateUserStatus(row.id, target)
    ElMessage.success(target === 0 ? '已停用' : '已启用')
    loadUsers()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

async function toggleMerchant(row) {
  const target = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确定${target === 0 ? '停用' : '启用'}商家“${row.merchantName}”吗？`,
      '操作确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await updateMerchantStatus(row.id, target)
    ElMessage.success(target === 0 ? '已停用' : '已启用')
    loadMerchants()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

async function openOrder(row) {
  try {
    orderDetail.value = await fetchAdminOrder(row.id)
    orderDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '加载订单失败')
  }
}

function handleLogout() {
  removeToken()
  router.push('/admin/login')
}

onMounted(() => {
  if (!getToken() || getAccountType() !== 'ADMIN') {
    router.replace('/admin/login')
    return
  }
  loadUsers()
  loadMerchants()
  loadShops()
  loadOrders()
})
</script>

<style scoped>
.admin-console {
  max-width: 1080px;
  margin: auto;
  padding: 20px 24px 40px;
}

.console-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.keyword {
  max-width: 260px;
}

.status {
  width: 130px;
}

.order-detail p {
  margin: 6px 0;
}

.amount {
  text-align: right;
}
</style>
