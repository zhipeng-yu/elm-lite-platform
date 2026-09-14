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
      <el-button v-if="errorMsg" :disabled="loading" @click="load">重新加载</el-button>

      <el-empty v-else-if="notFound" description="商品不存在" class="empty">
        <el-button type="primary" @click="router.push('/shops')">
          返回店铺列表
        </el-button>
      </el-empty>

      <template v-else-if="product">
        <section class="product-gallery">
          <div class="main-image-wrap">
            <img
              v-if="activeImage"
              :src="activeImage"
              :alt="product.productName"
              class="main-image"
              @error="handleMainImageError"
            >
            <div v-else class="image-placeholder">
              暂无商品图片
            </div>
          </div>

          <div v-if="galleryImages.length" class="thumbnail-list">
            <button
              v-for="image in galleryImages"
              v-show="!failedImages.includes(image)"
              :key="image"
              type="button"
              class="thumbnail-button"
              :class="{ active: activeImage === image }"
              :aria-label="`查看商品图片 ${image}`"
              @click="selectImage(image)"
            >
              <img
                :src="image"
                :alt="`${product.productName} 缩略图`"
                class="thumbnail-image"
                @error="handleThumbnailImageError(image)"
              >
            </button>
          </div>
        </section>

        <div class="product-head">
          <h2>{{ product.productName }}</h2>
          <el-tag>{{ product.categoryName }}</el-tag>
          <el-tag v-if="product.stock === 0" type="info">售罄</el-tag>
        </div>

        <section class="product-intro">
          <h3>商品介绍</h3>
          <p class="desc">{{ product.description || '暂无简介' }}</p>
        </section>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="价格">
            ¥{{ formatPriceCent(product.priceCent) }}
          </el-descriptions-item>

          <el-descriptions-item label="库存">
            {{ product.stock }}
          </el-descriptions-item>

          <el-descriptions-item label="所属店铺">
            <el-button
              link
              type="primary"
              @click="router.push(`/shops/${product.shopId}`)"
            >
              查看店铺
            </el-button>
          </el-descriptions-item>
        </el-descriptions>

        <div class="purchase-bar">
          <div class="purchase-price">
            <span class="price-label">价格</span>
            <strong>¥{{ formatPriceCent(product.priceCent) }}</strong>
          </div>

            <el-input-number
              v-model="quantity"
              :min="1"
              :max="Math.max(product.stock, 1)"
              :disabled="product.stock === 0"
              aria-label="购买数量"
            />
          <div class="cart-actions">
            <el-button @click="router.push('/cart')">购物车</el-button>
            <el-button
              type="primary"
              :disabled="product.stock === 0"
              :loading="adding"
              @click="handleAddToCart"
            >
              {{ product.stock === 0 ? '已售罄' : '加入购物车' }}
            </el-button>
          </div>
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

const galleryImages = ref([])
const activeImage = ref('')
const failedImages = ref([])

function buildGallery(productValue) {
  const images = []

  function addImage(value) {
    if (typeof value !== 'string') return

    const image = value.trim()
    if (!image || images.includes(image)) return

    images.push(image)
  }

  addImage(productValue?.imageUrl)

  if (Array.isArray(productValue?.detailImageUrls)) {
    productValue.detailImageUrls.forEach(addImage)
  }

  return images
}

function resetGallery(productValue) {
  galleryImages.value = buildGallery(productValue)
  activeImage.value = galleryImages.value[0] ?? ''
  failedImages.value = []
}

function selectImage(image) {
  if (!image || failedImages.value.includes(image)) return
  activeImage.value = image
}

function handleMainImageError() {
  if (
    activeImage.value &&
    !failedImages.value.includes(activeImage.value)
  ) {
    failedImages.value = [
      ...failedImages.value,
      activeImage.value
    ]
  }

  activeImage.value = ''
}

function handleThumbnailImageError(image) {
  if (!image || failedImages.value.includes(image)) return

  failedImages.value = [
    ...failedImages.value,
    image
  ]

  if (activeImage.value === image) {
    activeImage.value = ''
  }
}

async function handleAddToCart() {
  if (
    !product.value ||
    product.value.stock <= 0 ||
    adding.value
  ) {
    return
  }

  adding.value = true

  try {
    await addCartItem(product.value.id, quantity.value)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(
      error.response?.data?.msg ||
      '加入购物车失败，请稍后重试'
    )
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
    quantity.value = 1
    resetGallery(product.value)
  } catch (error) {
    product.value = null
    galleryImages.value = []
    activeImage.value = ''
    failedImages.value = []

    if (error.response?.status === 404) {
      notFound.value = true
    } else {
      errorMsg.value =
        error.response?.data?.msg ||
        '加载失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.product-detail {
  padding: 8px 0 96px;
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

.product-gallery {
  margin-bottom: 20px;
}

.main-image-wrap {
  width: 100%;
  max-height: 420px;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 12px;
  background: var(--el-fill-color-light);
}

.main-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
}

.thumbnail-list {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.thumbnail-button {
  flex: 0 0 72px;
  width: 72px;
  height: 72px;
  padding: 0;
  overflow: hidden;
  border: 2px solid transparent;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
}

.thumbnail-button.active {
  border-color: var(--el-color-primary);
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.product-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.product-head h2 {
  margin: 0;
}

.product-intro {
  margin-bottom: 16px;
}

.product-intro h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.desc {
  margin: 0;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.purchase-bar {
  position: sticky;
  bottom: 8px;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
}

.purchase-price {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex-shrink: 0;
}

.price-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.purchase-price strong {
  color: var(--el-color-danger);
  font-size: 20px;
}

.cart-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 600px) {
  .product-detail {
    padding-bottom: 88px;
  }

  .main-image-wrap {
    border-radius: 10px;
  }

  .purchase-bar {
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: calc(66px + env(safe-area-inset-bottom));
    flex-wrap: wrap;
    gap: 10px;
    padding: 10px 12px;
  }

  .cart-actions {
    width: 100%;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .purchase-bar > .el-input-number { width: 140px; }
  .cart-actions .el-button { flex: 1; margin-left: 0; }
  .product-detail { padding-bottom: 160px; }
}
</style>
