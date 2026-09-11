<template>
  <section class="coupon-section">
    <div class="coupon-heading">
      <div>
        <h3>店铺优惠券</h3>
        <p class="muted">
          当前仅展示正在生效且可以领取的优惠券。
        </p>
      </div>

      <el-button
        :disabled="loading"
        @click="load"
      >
        刷新
      </el-button>
    </div>

    <el-alert
      v-if="error"
      type="error"
      :closable="false"
      show-icon
      :title="error"
      class="coupon-alert"
    />

    <p
      v-if="loading"
      role="status"
      class="muted"
    >
      正在加载优惠券…
    </p>

    <el-empty
      v-else-if="!error && !coupons.length"
      description="当前暂无可领取优惠券"
      :image-size="80"
    />

    <div
      v-else-if="!error"
      class="coupon-list"
    >
      <article
        v-for="coupon in coupons"
        :key="coupon.id"
        class="coupon-card"
      >
        <div class="coupon-info">
          <strong>{{ coupon.name }}</strong>

          <p class="coupon-value">
            满 ¥{{ formatPriceCent(coupon.thresholdCent) }}
            减 ¥{{ formatPriceCent(coupon.discountCent) }}
          </p>

          <p class="muted">
            有效期至 {{ formatDateTime(coupon.expiresAt) }}
          </p>
        </div>

        <el-button
          type="primary"
          :loading="claimingId === coupon.id"
          :disabled="
            claimingId !== null ||
            claimedIds.includes(coupon.id)
          "
          @click="claim(coupon)"
        >
          {{
            claimedIds.includes(coupon.id)
              ? '已领取'
              : '领取'
          }}
        </el-button>
      </article>
    </div>
  </section>
</template>

<script setup>
import {
  onMounted,
  ref,
  watch
} from 'vue'
import {
  useRoute,
  useRouter
} from 'vue-router'
import { ElMessage } from 'element-plus'

import {
  claimCoupon,
  listClaimableCoupons
} from '@/api/coupon'
import {
  getAccountType,
  getToken
} from '@/utils/auth'
import { formatPriceCent } from '@/utils/format'

const props = defineProps({
  shopId: {
    type: [Number, String],
    required: true
  }
})

const route = useRoute()
const router = useRouter()

const coupons = ref([])
const claimedIds = ref([])
const loading = ref(false)
const claimingId = ref(null)
const error = ref('')

function formatDateTime(value) {
  if (!value) {
    return '—'
  }

  return String(value)
    .replace('T', ' ')
    .slice(0, 16)
}

function markClaimed(couponId) {
  if (!claimedIds.value.includes(couponId)) {
    claimedIds.value = [
      ...claimedIds.value,
      couponId
    ]
  }
}

async function load() {
  if (!props.shopId) {
    coupons.value = []
    return
  }

  loading.value = true
  error.value = ''

  try {
    coupons.value =
      await listClaimableCoupons(props.shopId)
  } catch (e) {
    coupons.value = []
    error.value =
      e.response?.data?.msg ||
      '加载优惠券失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function claim(coupon) {
  if (!getToken()) {
    await router.push({
      path: '/login',
      query: {
        redirect: route.fullPath
      }
    })

    return
  }

  if (getAccountType() !== 'USER') {
    ElMessage.warning(
      '请使用顾客账号领取优惠券'
    )

    return
  }

  if (claimingId.value !== null) {
    return
  }

  claimingId.value = coupon.id

  try {
    await claimCoupon(coupon.id)

    markClaimed(coupon.id)

    ElMessage.success('优惠券领取成功')
  } catch (e) {
    if (e.response?.status === 409) {
      markClaimed(coupon.id)
    }
  } finally {
    claimingId.value = null
  }
}

watch(
  () => props.shopId,
  () => {
    claimedIds.value = []
    load()
  }
)

onMounted(load)
</script>

<style scoped>
.coupon-section {
  margin-top: 20px;
  padding: 18px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
}

.coupon-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.coupon-heading h3 {
  margin: 0 0 4px;
}

.muted {
  margin: 0;
  color: #909399;
}

.coupon-alert {
  margin-bottom: 12px;
}

.coupon-list {
  display: grid;
  gap: 12px;
}

.coupon-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.coupon-info {
  min-width: 0;
}

.coupon-info strong {
  display: block;
  margin-bottom: 5px;
}

.coupon-value {
  margin: 0 0 4px;
  font-weight: 600;
}

@media (max-width: 640px) {
  .coupon-card {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>