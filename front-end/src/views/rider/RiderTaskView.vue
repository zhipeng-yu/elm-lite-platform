<template>
  <main class="rider-page">
    <header><h1>配送任务</h1><button class="secondary" @click="logout">退出登录</button></header>
    <nav aria-label="任务筛选">
      <button :class="{primary: tab === 'available'}" :disabled="loading || acting" @click="switchTab('available')">待接订单</button>
      <button :class="{primary: tab === 'mine'}" :disabled="loading || acting" @click="switchTab('mine')">我的任务</button>
      <button :disabled="loading || acting" @click="load">刷新</button>
    </nav>
    <p v-if="loading" role="status">加载中…</p>
    <p v-if="error" role="alert" class="error-text">错误：{{ error }} <button @click="load">重试</button></p>
    <section v-if="!loading && !error && !orders.length" class="surface-card empty-state">暂无任务</section>
    <article v-for="order in orders" :key="order.id" class="surface-card task">
      <b>{{ order.shopName || order.orderNo }}</b>
      <p>{{ ORDER_STATUS_TEXT[order.orderStatus] }}</p>
      <p v-if="order.shopAddress">取餐：{{ order.shopAddress }}</p>
      <p>{{ order.items?.map(i => i.productName + ' × ' + i.quantity).join('、') }}</p>
      <button v-if="tab === 'available'" class="primary" :disabled="acting || loading" @click="act(order, 'claim')">接单</button>
      <button v-else class="secondary" :disabled="acting || loading" @click="showDetail(order.id)">查看配送信息</button>
    </article>
    <el-dialog v-model="detailVisible" title="配送信息" width="min(520px, calc(100vw - 24px))">
      <p v-if="detailLoading" role="status">正在加载配送信息…</p>
      <p v-if="detailError" role="alert">{{ detailError }}</p>
      <section v-if="detail" class="delivery-detail">
        <p>{{ ORDER_STATUS_TEXT[detail.orderStatus] }}</p>
        <p>取餐：{{ pickup?.shopName }} · {{ pickup?.address || '店铺地址加载失败，请重新打开详情' }}</p>
        <p>收货人：{{ detail.receiverName }}</p>
        <p>电话：<a :href="'tel:' + detail.receiverPhone">{{ detail.receiverPhone }}</a></p>
        <p>送到：{{ detail.deliveryAddress }}</p>
        <p>备注：{{ detail.remark || '无' }}</p>
        <p v-for="item in detail.items" :key="item.productId">{{ item.productName }} × {{ item.quantity }}</p>
        <button v-if="detail.orderStatus === 2" class="primary" :disabled="acting" @click="act(detail, 'dispatch')">已取餐，开始配送</button>
        <button v-if="detail.orderStatus === 3" class="primary" :disabled="acting" @click="act(detail, 'complete')">确认送达</button>
      </section>
    </el-dialog>
  </main>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/rider'
import { fetchShop } from '@/api/shop'
import { removeToken } from '@/utils/auth'
import { ORDER_STATUS_TEXT } from '@/utils/format'
const router = useRouter()
const tab = ref('available'), orders = ref([]), loading = ref(false), acting = ref(false), error = ref('')
const detail = ref(null), pickup = ref(null), detailVisible = ref(false), detailLoading = ref(false), detailError = ref('')
function logout() { removeToken(); router.replace('/rider/login') }
function switchTab(value) { tab.value = value; load() }
async function load() {
  if (loading.value) return
  loading.value = true; error.value = ''
  try { orders.value = await (tab.value === 'available' ? api.listAvailableOrders() : api.listRiderOrders()) }
  catch (e) { orders.value = []; error.value = e.response?.data?.msg || '加载失败' }
  finally { loading.value = false }
}
async function showDetail(id) {
  if (detailLoading.value) return
  detailVisible.value = true; detailLoading.value = true; detailError.value = ''; detail.value = null; pickup.value = null
  try { detail.value = await api.getRiderOrder(id); pickup.value = await fetchShop(detail.value.shopId) }
  catch (e) { detailError.value = e.response?.data?.msg || '加载配送信息失败，请关闭后重试' }
  finally { detailLoading.value = false }
}
async function act(order, action) {
  if (acting.value) return
  acting.value = true
  try {
    if (action === 'complete') await ElMessageBox.confirm('确认已将餐品交给收货人？', '确认送达', {confirmButtonText: '已送达', cancelButtonText: '继续配送'})
    const updated = await ({claim: api.claimOrder, dispatch: api.dispatchOrder, complete: api.completeOrder}[action])(order.id)
    if (action === 'claim') { tab.value = 'mine'; await showDetail(order.id) }
    else detail.value = updated
    await load()
    ElMessage.success('任务已更新')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') { ElMessage.error(e.response?.data?.msg || '操作失败，请刷新查看最新状态'); await load() }
  } finally { acting.value = false }
}
onMounted(load)
</script>
<style scoped>
.rider-page { max-width: 620px; min-height: 100dvh; margin: auto; padding: 24px 16px 48px; }
header h1 { font-size: 30px; }
header, nav { display: flex; align-items: center; justify-content: space-between; gap: 8px; flex-wrap: wrap; }
nav { margin: 16px 0; }
nav button { flex: 1; min-height: 44px; padding: 0 12px; border: 1px solid var(--line); border-radius: var(--radius-control); background: white; color: var(--ink); font: inherit; cursor: pointer; }
nav button.primary { border-color: var(--brand); background: var(--brand); color: white; }
.task { margin: 12px 0; padding: 18px; overflow-wrap: anywhere; box-shadow: 0 1px 2px rgba(15, 23, 42, .04); }
.task b { display: block; font-size: 18px; }
.task p, .delivery-detail p { margin: 12px 0; line-height: 1.6; overflow-wrap: anywhere; }
.task button, .delivery-detail button { width: 100%; min-height: 44px; margin-top: 10px; }
</style>
