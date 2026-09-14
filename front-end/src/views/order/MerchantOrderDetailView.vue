<template>
  <div class="workspace">
    <header class="page-heading"><div><p class="eyebrow">订单详情</p><h1>{{ order?.orderNo || '商家订单' }}</h1></div><router-link class="secondary" to="/merchant/orders">返回订单列表</router-link></header>
    <p v-if="error" role="alert" class="error-text">{{ error }} <button class="text-button" @click="load">重新加载</button></p>
    <p v-if="loading" role="status">正在加载订单…</p>
    <section v-else-if="order" class="surface-card">
      <div class="section-heading"><h2>订单信息</h2><el-tag :type="ORDER_STATUS_TAG[order.orderStatus]">{{ ORDER_STATUS_TEXT[order.orderStatus] }}</el-tag></div>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="下单账号">{{ order.buyer.displayName }}（#{{ order.buyer.id }}）</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ order.receiverName }} {{ order.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ order.deliveryAddress }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ order.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
      <div class="table-scroll"><table><thead><tr><th>商品</th><th>单价</th><th>数量</th><th>小计</th></tr></thead><tbody><tr v-for="item in order.items" :key="item.productId"><td>{{ item.productName }}</td><td>¥{{ formatPriceCent(item.unitPriceCent) }}</td><td>{{ item.quantity }}</td><td>¥{{ formatPriceCent(item.subtotalCent) }}</td></tr></tbody></table></div>
      <div class="amounts"><p>商品 ¥{{ formatPriceCent(order.productAmountCent) }}</p><p v-if="order.discountAmountCent > 0">优惠 −¥{{ formatPriceCent(order.discountAmountCent) }}</p><p>配送费 ¥{{ formatPriceCent(order.deliveryFeeCent) }}</p><strong>合计 ¥{{ formatPriceCent(order.totalAmountCent) }}</strong></div>
      <div class="actions"><button v-if="order.orderStatus === 0" class="primary" :disabled="acting" @click="act('confirm')">确认订单</button><button v-if="order.orderStatus === 1" class="primary" :disabled="acting" @click="act('prepare')">开始制作</button></div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as api from '@/api/merchant'
import { formatPriceCent, ORDER_STATUS_TAG, ORDER_STATUS_TEXT } from '@/utils/format'
const route = useRoute(), order = ref(null), loading = ref(false), acting = ref(false), error = ref('')
async function load() {
  loading.value = true; error.value = ''
  try { order.value = await api.fetchMerchantOrder(route.params.id) }
  catch (e) { error.value = e.response?.data?.msg || '加载订单失败，请重试' }
  finally { loading.value = false }
}
async function act(action) {
  acting.value = true; error.value = ''
  try { order.value = action === 'confirm' ? await api.confirmMerchantOrder(order.value.id) : await api.prepareMerchantOrder(order.value.id); ElMessage.success('订单状态已更新') }
  catch (e) { error.value = e.response?.data?.msg || '操作失败，请刷新后重试' }
  finally { acting.value = false }
}
onMounted(load)
</script>

<style scoped>.amounts { margin-top: 20px; text-align: right; display: grid; gap: 5px; }.amounts p { margin: 0; }</style>
