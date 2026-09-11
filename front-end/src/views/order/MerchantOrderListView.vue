<template>
  <div class="workspace">
    <header class="page-heading">
      <div><p class="eyebrow">订单履约</p><h1>商家订单</h1></div>
      <router-link class="secondary" to="/merchant">返回工作台</router-link>
    </header>
    <section class="surface-card shop-toolbar">
      <label>店铺<select v-model="shopId" :disabled="loading || acting" @change="loadOrders"><option v-for="shop in shops" :key="shop.id" :value="shop.id">{{ shop.shopName }}</option></select></label>
      <label>订单状态<select v-model="status" :disabled="loading || acting" @change="loadOrders"><option value="">全部</option><option v-for="(text, value) in ORDER_STATUS_TEXT" :key="value" :value="Number(value)">{{ text }}</option></select></label>
      <button class="secondary" :disabled="loading || acting || !shopId" @click="loadOrders">刷新</button>
    </section>
    <p v-if="error" role="alert" class="error-text">{{ error }} <button class="text-button" @click="load">重新加载</button></p>
    <p v-if="loading" role="status">正在加载订单…</p>
    <section v-else-if="!orders.length" class="surface-card empty-state"><h2>暂无订单</h2><p class="muted">新订单会在刷新后显示。</p></section>
    <section v-else class="surface-card table-scroll">
      <table><thead><tr><th>订单号</th><th>收货人</th><th>商品</th><th>数量</th><th>金额</th><th>状态</th><th>下单时间</th><th>操作</th></tr></thead>
        <tbody><tr v-for="order in orders" :key="order.id"><td><router-link :to="`/merchant/orders/${order.id}`">{{ order.orderNo }}</router-link></td><td>{{ order.receiverName }}</td><td>{{ order.items.map(item => item.productName).join('、') }}</td><td>{{ order.items.reduce((sum, item) => sum + item.quantity, 0) }}</td><td>¥{{ formatPriceCent(order.totalAmountCent) }}</td><td><el-tag :type="ORDER_STATUS_TAG[order.orderStatus]">{{ ORDER_STATUS_TEXT[order.orderStatus] }}</el-tag></td><td>{{ order.createdAt }}</td><td><button v-if="order.orderStatus === 0" class="text-button" :disabled="acting === order.id" @click="act(order, 'confirm')">确认</button><button v-if="order.orderStatus === 1" class="text-button" :disabled="acting === order.id" @click="act(order, 'prepare')">开始制作</button></td></tr></tbody>
      </table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from '@/api/merchant'
import { formatPriceCent, ORDER_STATUS_TAG, ORDER_STATUS_TEXT } from '@/utils/format'
const shops = ref([]), shopId = ref(null), status = ref(''), orders = ref([])
const loading = ref(false), acting = ref(null), error = ref('')
async function load() {
  loading.value = true; error.value = ''
  try { shops.value = await api.listMyShops(); shopId.value = shops.value[0]?.id ?? null; await loadOrders() }
  catch (e) { error.value = e.response?.data?.msg || '加载订单失败，请重试' }
  finally { loading.value = false }
}
async function loadOrders() {
  if (!shopId.value) { orders.value = []; return }
  loading.value = true; error.value = ''
  try { orders.value = await api.listMerchantOrders(shopId.value, status.value) }
  catch (e) { orders.value = []; error.value = e.response?.data?.msg || '加载订单失败，请重试' }
  finally { loading.value = false }
}
async function act(order, action) {
  acting.value = order.id; error.value = ''
  try {
    const updated = action === 'confirm' ? await api.confirmMerchantOrder(order.id) : await api.prepareMerchantOrder(order.id)
    const index = orders.value.findIndex(item => item.id === order.id)
    if (status.value === '' || Number(status.value) === updated.orderStatus) orders.value[index] = { ...order, ...updated }
    else orders.value.splice(index, 1)
    ElMessage.success(action === 'confirm' ? '订单已确认' : '已开始制作')
  } catch (e) { error.value = e.response?.data?.msg || '操作失败，请刷新后重试' }
  finally { acting.value = null }
}
onMounted(load)
</script>
