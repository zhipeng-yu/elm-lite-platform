<template>
  <div class="product-detail">
    <div class="header">
      <el-button link @click="goBack">← 返回</el-button>
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
      <el-empty v-else-if="notFound" description="商品不存在" class="empty">
        <el-button type="primary" @click="router.push('/shops')">返回店铺列表</el-button>
      </el-empty>
      <template v-else-if="product">
        <div class="product-head">
          <h2>{{ product.productName }}</h2>
          <el-tag>{{ product.categoryName }}</el-tag>
          <el-tag v-if="product.stock === 0" type="info">售罄</el-tag>
        </div>
        <p class="desc">{{ product.description || '暂无简介' }}</p>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="价格">
            ¥{{ formatPriceCent(product.priceCent) }}
          </el-descriptions-item>
          <el-descriptions-item label="库存">{{ product.stock }}</el-descriptions-item>
          <el-descriptions-item label="所属店铺">
            <el-button link type="primary" @click="router.push(`/shops/${product.shopId}`)">
              查看店铺
            </el-button>
          </el-descriptions-item>
        </el-descriptions>
        <div class="cart-actions">
          <el-input-number v-model="quantity" :min="1" :max="product.stock" :disabled="product.stock === 0" />
          <el-button
            type="primary"
            :disabled="product.stock === 0"
            :loading="adding"
            @click="handleAddToCart"
          >
            加入购物车
          </el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { addCartItem } from '@/api/cart'
import { fetchProduct } from '@/api/product'
import { formatPriceCent } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const notFound = ref(false)
const errorMsg = ref('')
const product = ref(null)
const quantity = ref(1)
const adding = ref(false)

async function handleAddToCart() {
  adding.value = true
  try {
    await addCartItem(product.value.id, quantity.value)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '加入购物车失败，请稍后重试')
  } finally {
    adding.value = false
  }
}

function goBack() {
  if (product.value?.shopId) {
    router.push(`/shops/${product.value.shopId}/products`)
    return
  }
  router.back()
}

async function load() {
  loading.value = true
  notFound.value = false
  errorMsg.value = ''
  try {
    product.value = await fetchProduct(route.params.id)
  } catch (error) {
    product.value = null
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
.product-detail {
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

.product-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.desc {
  color: #909399;
  margin-bottom: 16px;
}

.cart-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  align-items: center;
}
</style>
