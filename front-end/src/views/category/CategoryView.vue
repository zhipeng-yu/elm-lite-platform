<template>
  <div class="categories">
    <div class="header">
      <el-button link @click="router.push(`/shops/${route.params.id}`)">
        ← 返回店铺
      </el-button>
    </div>
    <h2>{{ shopName || '分类' }}</h2>
    <div v-loading="loading" class="content">
      <el-alert
        v-if="errorMsg"
        type="error"
        :closable="false"
        show-icon
        :title="errorMsg"
        class="error"
      />
      <el-empty v-else-if="categories.length === 0" description="暂无分类" />
      <ul v-else class="category-list">
        <li
          v-for="item in categories"
          :key="item.id"
          @click="goProducts(item.id)"
        >
          {{ item.categoryName }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchCategories } from '@/api/category'
import { fetchShop } from '@/api/shop'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMsg = ref('')
const categories = ref([])
const shopName = ref('')

function goProducts(categoryId) {
  router.push(`/shops/${route.params.id}/products?categoryId=${categoryId}`)
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [shop, list] = await Promise.all([
      fetchShop(route.params.id).catch(() => null),
      fetchCategories(route.params.id)
    ])
    shopName.value = shop?.shopName || ''
    categories.value = list
  } catch (error) {
    categories.value = []
    errorMsg.value = error.response?.data?.msg || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.categories {
  padding: 8px 0;
}

.header {
  margin-bottom: 8px;
}

.content {
  min-height: 160px;
  margin-top: 8px;
}

.error {
  margin-bottom: 12px;
}

.category-list {
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.category-list li {
  padding: 16px 24px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.category-list li:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>
