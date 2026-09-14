<template>
  <div class="my-coupons">
    <el-button v-if="route.query.from === 'checkout'" link type="primary" @click="router.push('/checkout')">← 返回结算</el-button>
    <h2>我的优惠券</h2>

    <div v-loading="loading">
      <el-alert
        v-if="errorMsg"
        type="error"
        :closable="false"
        show-icon
        :title="errorMsg"
      />
      <el-button
        v-if="errorMsg"
        :disabled="loading"
        @click="load"
      >
        重新加载
      </el-button>

      <el-empty
        v-else-if="coupons.length === 0"
        description="暂无优惠券"
      />

      <div v-else class="coupon-list">
        <el-card
          v-for="coupon in coupons"
          :key="coupon.userCouponId"
          class="coupon-card"
        >
          <div class="coupon-main">
            <strong>{{ coupon.name }}</strong>
            <el-tag :type="tagType(coupon.status)">
              {{ myCouponStatusLabel(coupon.status) }}
            </el-tag>
          </div>
          <p>
            满 ¥{{ formatPriceCent(coupon.thresholdCent) }}
            减 ¥{{ formatPriceCent(coupon.discountCent) }}
          </p>
          <p class="time">
            {{ formatTime(coupon.startsAt) }}
            至
            {{ formatTime(coupon.expiresAt) }}
          </p>
          <el-button v-if="coupon.status === 'AVAILABLE'" type="primary" plain @click="router.push(`/shops/${coupon.shopId}`)">去适用店铺</el-button>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { listMyCoupons } from '@/api/coupon'
import {
  myCouponStatusLabel
} from '@/utils/coupon'
import { formatPriceCent } from '@/utils/format'

const loading = ref(false)
const route = useRoute()
const router = useRouter()
const errorMsg = ref('')
const coupons = ref([])

function tagType(status) {
  return {
    AVAILABLE: 'success',
    USED: 'info',
    EXPIRED: 'warning',
    DISABLED: 'danger'
  }[status] ?? 'info'
}

function formatTime(value) {
  return value
    ? new Date(value).toLocaleString()
    : '-'
}

async function load() {
  loading.value = true
  errorMsg.value = ''

  try {
    coupons.value = await listMyCoupons()
  } catch (error) {
    errorMsg.value =
      error.response?.data?.msg ||
      '优惠券加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.my-coupons {
  padding: 8px 0;
}

.coupon-list {
  display: grid;
  gap: 12px;
}

.coupon-main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.coupon-card p {
  margin: 10px 0 0;
}

.time {
  color: #909399;
  font-size: 13px;
}
</style>
