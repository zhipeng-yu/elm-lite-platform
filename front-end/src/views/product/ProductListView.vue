<template>
  <div class="products">
    <div class="header">
      <el-button link @click="router.push(`/shops/${route.params.id}`)">
        ← 返回店铺
      </el-button>
    </div>
    <h2>商品列表</h2>
    <el-tabs v-if="categories.length > 0" v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="0" />
      <el-tab-pane
        v-for="item in categories"
        :key="item.id"
        :label="item.categoryName"
        :name="String(item.id)"
      />
    </el-tabs>
    <div v-loading="loading" class="content">
      <el-alert
        v-if="errorMsg"
        type="error"
        :closable="false"
        show-icon
        :title="errorMsg"
        class="error"
      />
      <el-empty v-else-if="products.length === 0" description="暂无商品" />
      <ul v-else class="product-list">
        <li v-for="item in products" :key="item.id" @click="goDetail(item.id)">
          <div class="product-name">
            {{ item.productName }}
            <el-tag v-if="item.stock === 0" type="info" size="small">售罄</el-tag>
          </div>
          <p class="desc">{{ item.description || '暂无简介' }}</p>
          <p class="meta">¥{{ formatPriceCent(item.priceCent) }} · 库存 {{ item.stock }}</p>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchCategories } from '@/api/category'
import { fetchProducts } from '@/api/product'
import { formatPriceCent } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMsg = ref('')
const categories = ref([])
const products = ref([])
const activeTab = ref(String(route.query.categoryId || '0'))
let latestRequest = 0

function goDetail(id) {
  router.push(`/products/${id}`)
}

function handleTabChange(name) {
  const categoryId = Number(name)
  router.replace({
    path: `/shops/${route.params.id}/products`,
    query: categoryId ? { categoryId } : {}
  })
}

async function loadProducts() {
  const requestId = ++latestRequest
  loading.value = true
  errorMsg.value = ''
  try {
    const categoryId = activeTab.value === '0' ? undefined : Number(activeTab.value)
    const list = await fetchProducts(route.params.id, categoryId)
    if (requestId === latestRequest) {
      products.value = list
    }
  } catch (error) {
    if (requestId !== latestRequest) {
      return
    }
    products.value = []
    errorMsg.value = error.response?.data?.msg || '加载失败，请稍后重试'
  } finally {
    if (requestId === latestRequest) {
      loading.value = false
    }
  }
}

watch(
  () => route.query.categoryId,
  () => {
    activeTab.value = String(route.query.categoryId || '0')
    loadProducts()
  }
)

onMounted(async () => {
  try {
    categories.value = await fetchCategories(route.params.id)
  } catch {
    categories.value = []
  }
  loadProducts()
})
</script>

<style scoped>
.products {
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

.product-list {
  list-style: none;
}

.product-list li {
  padding: 12px 14px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.product-list li:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.product-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
}

.desc {
  color: #909399;
  margin: 6px 0;
}

.meta {
  color: #606266;
}
</style>
