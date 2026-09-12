<template>
  <section class="surface-card coupon-panel">
    <div class="section-heading">
      <div>
        <h2>
          优惠券
          <small>{{ coupons.length }} 张</small>
        </h2>
        <p class="muted">
          创建后的优惠金额、门槛和有效期不可修改，只能启用或停用。
        </p>
      </div>

      <button
        class="primary"
        :disabled="busy || loading"
        @click="openCreate"
      >
        新增优惠券
      </button>
    </div>

    <p
      v-if="error"
      role="alert"
      class="error-text"
    >
      {{ error }}
      <button
        class="secondary"
        :disabled="busy"
        @click="load"
      >
        重新加载
      </button>
    </p>

    <p
      v-if="loading"
      role="status"
      class="muted"
    >
      正在加载优惠券…
    </p>

    <p
      v-else-if="!coupons.length"
      class="muted"
    >
      暂无优惠券，可以创建第一张优惠券。
    </p>

    <div
      v-else
      class="table-scroll"
    >
      <table>
        <thead>
          <tr>
            <th>名称</th>
            <th>优惠</th>
            <th>有效期</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="coupon in coupons"
            :key="coupon.id"
          >
            <td>
              <strong>{{ coupon.name }}</strong>
            </td>

            <td>
              满 ¥{{ formatPriceCent(coupon.thresholdCent) }}
              减 ¥{{ formatPriceCent(coupon.discountCent) }}
            </td>

            <td>
              <p>{{ formatDateTime(coupon.startsAt) }}</p>
              <p class="muted">
                至 {{ formatDateTime(coupon.expiresAt) }}
              </p>
            </td>

            <td>
              <span class="status-pill">
                {{ couponAvailability(coupon) }}
              </span>
            </td>

            <td>
              <button
                class="text-button"
                :disabled="busy"
                @click="toggleCoupon(coupon)"
              >
                {{ coupon.enabled ? '停用' : '启用' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <el-dialog
      v-model="dialogOpen"
      title="新增优惠券"
      width="min(520px, 94vw)"
      :close-on-click-modal="!busy"
      :close-on-press-escape="!busy"
      :show-close="!busy"
    >
      <form
        class="form-grid"
        @submit.prevent="save"
      >
        <fieldset
          :disabled="busy"
          class="form-grid"
        >
          <label>
            优惠券名称
            <input
              v-model.trim="form.name"
              required
              maxlength="255"
              placeholder="例如：新人满减券"
            >
          </label>

          <div class="form-columns">
            <label>
              使用门槛（元）
              <input
                v-model.number="form.thresholdYuan"
                type="number"
                required
                min="0"
                max="99999999.99"
                step="0.01"
              >
            </label>

            <label>
              优惠金额（元）
              <input
                v-model.number="form.discountYuan"
                type="number"
                required
                min="0.01"
                max="99999999.99"
                step="0.01"
              >
            </label>
          </div>

          <label>
            开始时间
            <input
              v-model="form.startsAt"
              type="datetime-local"
              required
            >
          </label>

          <label>
            到期时间
            <input
              v-model="form.expiresAt"
              type="datetime-local"
              required
            >
          </label>
        </fieldset>

        <p
          v-if="formError"
          role="alert"
          class="error-text"
        >
          {{ formError }}
        </p>

        <div class="actions">
          <button
            class="primary"
            :disabled="busy"
          >
            {{ busy ? '创建中…' : '创建' }}
          </button>

          <button
            class="secondary"
            type="button"
            :disabled="busy"
            @click="dialogOpen = false"
          >
            取消
          </button>
        </div>
      </form>
    </el-dialog>
  </section>
</template>

<script setup>
import {
  onMounted,
  reactive,
  ref,
  watch
} from 'vue'
import { ElMessage } from 'element-plus'

import * as api from '@/api/merchant'
import {
  couponAvailability,
  couponPayload
} from '@/utils/coupon'
import { formatPriceCent } from '@/utils/format'

const props = defineProps({
  shopId: {
    type: [Number, String],
    required: true
  }
})

const coupons = ref([])
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const formError = ref('')
const dialogOpen = ref(false)

const form = reactive({
  name: '',
  thresholdYuan: 0,
  discountYuan: 1,
  startsAt: '',
  expiresAt: ''
})

function localInputValue(date) {
  const pad = (value) =>
    String(value).padStart(2, '0')

  return [
    date.getFullYear(),
    '-',
    pad(date.getMonth() + 1),
    '-',
    pad(date.getDate()),
    'T',
    pad(date.getHours()),
    ':',
    pad(date.getMinutes())
  ].join('')
}

function formatDateTime(value) {
  if (!value) {
    return '—'
  }

  return String(value)
    .replace('T', ' ')
    .slice(0, 16)
}

function openCreate() {
  const starts = new Date(
    Date.now() + 5 * 60 * 1000
  )

  const expires = new Date(
    starts.getTime() + 7 * 24 * 60 * 60 * 1000
  )

  form.name = ''
  form.thresholdYuan = 0
  form.discountYuan = 1
  form.startsAt = localInputValue(starts)
  form.expiresAt = localInputValue(expires)

  formError.value = ''
  dialogOpen.value = true
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
      await api.listManagedCoupons(props.shopId)
  } catch (e) {
    coupons.value = []
    error.value =
      e.response?.data?.msg ||
      '加载优惠券失败，请重试'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (busy.value) {
    return
  }

  busy.value = true
  formError.value = ''

  try {
    const payload = couponPayload(form)

    await api.createCoupon(
      props.shopId,
      payload
    )

    dialogOpen.value = false

    ElMessage.success('优惠券创建成功')

    await load()
  } catch (e) {
    formError.value =
      e.response?.data?.msg ||
      e.message ||
      '创建优惠券失败'
  } finally {
    busy.value = false
  }
}

async function toggleCoupon(coupon) {
  if (busy.value) {
    return
  }

  busy.value = true
  error.value = ''

  try {
    await api.updateCouponEnabled(
      coupon.id,
      !coupon.enabled
    )

    ElMessage.success(
      coupon.enabled
        ? '优惠券已停用'
        : '优惠券已启用'
    )

    await load()
  } catch (e) {
    error.value =
      e.response?.data?.msg ||
      '修改优惠券状态失败'
  } finally {
    busy.value = false
  }
}

watch(
  () => props.shopId,
  () => load()
)

onMounted(load)
</script>

<style scoped>
.coupon-panel {
  margin-top: 18px;
}

.section-heading > div:first-child {
  min-width: 0;
}

.section-heading h2 {
  margin-bottom: 4px;
}

table {
  width: 100%;
}

td p {
  margin: 0;
}

.actions {
  display: flex;
  gap: 12px;
}
</style>