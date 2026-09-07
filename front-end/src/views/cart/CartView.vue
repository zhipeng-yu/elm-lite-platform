<template>
  <div class="cart">
    <div class="header">
      <h2>购物车</h2>
      <el-button
        type="primary"
        :disabled="loading || cartItems.length === 0"
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
      <el-empty v-else-if="cartItems.length === 0" description="购物车是空的，去店铺点餐吧">
        <el-button type="primary" @click="router.push('/shops')">去点餐</el-button>
      </el-empty>
      <template v-else>
        <ul class="cart-list">
          <li v-for="item in cartItems" :key="item.id">
            <div class="item-main">
              <div class="name">
                {{ item.productName }}
                <el-tag v-if="item.stock === 0" type="info" size="small">售罄</el-tag>
              </div>
              <p class="meta">单价 ¥{{ formatPriceCent(item.priceCent) }}</p>
            </div>
            <div class="item-actions">
              <el-input-number
                :model-value="item.quantity"
                :min="1"
                :max="item.stock"
                size="small"
                @change="(value) => handleQuantity(item, value)"
              />
              <span class="subtotal">¥{{ formatPriceCent(item.subtotalCent) }}</span>
              <el-button link type="danger" @click="handleRemove(item)">删除</el-button>
            </div>
          </li>
        </ul>
        <div class="summary">
          合计：<span class="total">¥{{ formatPriceCent(totalCent) }}</span>
        </div>
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
  if (value == null) {
    return
  }
  try {
    await updateCartItem(item.id, value)
    await load()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '修改数量失败，请稍后重试')
    await load()
  }
}

async function handleRemove(item) {
  try {
    await removeCartItem(item.id)
    ElMessage.success('已从购物车移除')
    await load()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '删除失败，请稍后重试')
  }
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
</style>
