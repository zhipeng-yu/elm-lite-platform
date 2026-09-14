<template>
  <div class="cart">
    <div class="header">
      <h2>购物车</h2>
      <el-button
        type="primary"
        :disabled="loading || busy || cartItems.length === 0 || Boolean(errorMsg)"
        @click="router.push('/checkout')"
      >
        去结算
      </el-button>
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
      <el-button v-if="errorMsg" :disabled="loading" @click="load">重新加载</el-button>
      <el-empty v-else-if="cartItems.length === 0" description="购物车是空的，去店铺点餐吧">
        <el-button type="primary" @click="router.push('/shops')">去点餐</el-button>
      </el-empty>
      <template v-else>
        <ul class="cart-list">
          <li v-for="item in cartItems" :key="item.id">
            <div class="item-main">
              <div class="name">
                {{ item.productName }}
                <el-tag v-if="item.status !== 1" type="warning" size="small">已下架</el-tag>
                <el-tag v-if="item.stock === 0" type="info" size="small">售罄</el-tag>
              </div>
              <p class="meta">单价 ¥{{ formatPriceCent(item.priceCent) }}</p>
            </div>
            <div class="item-actions">
              <el-input-number
                :model-value="item.quantity"
                :min="1"
                :max="Math.max(item.stock, 1)"
                :disabled="busy || item.status !== 1 || item.stock === 0"
                :aria-label="`${item.productName} 的数量`"
                @change="(value) => handleQuantity(item, value)"
              />
              <span class="subtotal">¥{{ formatPriceCent(item.subtotalCent) }}</span>
              <el-button link type="danger" :disabled="busy" @click="handleRemove(item)">删除</el-button>
            </div>
          </li>
        </ul>
        <div class="summary">
          商品合计：<span class="total">¥{{ formatPriceCent(totalCent) }}</span>（配送费在结算时显示）
        </div>
        <el-button @click="router.push(`/shops/${cartItems[0].shopId}/products`)">继续加购</el-button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import { fetchCartItems, removeCartItem, updateCartItem } from '@/api/cart'
import { formatPriceCent } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const cartItems = ref([])
const busy = ref(false)

const totalCent = computed(() =>
  cartItems.value.reduce((sum, item) => sum + item.subtotalCent, 0)
)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    cartItems.value = await fetchCartItems()
  } catch (error) {
    cartItems.value = []
    errorMsg.value = error.response?.data?.msg || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function handleQuantity(item, value) {
  if (busy.value || value == null || value === item.quantity) {
    return
  }
  busy.value = true
  try {
    await updateCartItem(item.id, value)
    await load()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '修改数量失败，请稍后重试')
    await load()
  } finally { busy.value = false }
}

async function handleRemove(item) {
  if (busy.value) return
  busy.value = true
  try {
    await removeCartItem(item.id)
    ElMessage.success('已从购物车移除')
    await load()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '删除结果未确认，已刷新购物车')
    await load()
  } finally { busy.value = false }
}

onMounted(load)
</script>

<style scoped>
.cart {
  padding: 8px 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.content {
  min-height: 160px;
  margin-top: 8px;
}

.error {
  margin-bottom: 12px;
}

.cart-list {
  list-style: none;
}

.cart-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
}

.meta {
  color: #909399;
  margin-top: 4px;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.subtotal {
  font-weight: 700;
  min-width: 72px;
  text-align: right;
}

.summary {
  text-align: right;
  margin-top: 8px;
}

.total {
  color: #f56c6c;
  font-size: 18px;
  font-weight: 700;
}
@media (max-width: 600px) {
  .cart-list li { align-items: stretch; flex-direction: column; gap: 14px; }
  .name { flex-wrap: wrap; overflow-wrap: anywhere; }
  .item-actions { flex-wrap: wrap; gap: 8px; }
  .item-actions .el-input-number { width: 140px; }
  .subtotal { min-width: 0; margin-left: auto; }
  .summary { font-size: 13px; line-height: 1.8; margin-bottom: 12px; }
}
</style>
