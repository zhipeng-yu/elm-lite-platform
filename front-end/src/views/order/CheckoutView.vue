<template>
  <div class="checkout">
    <div class="header">
      <el-button link @click="router.push('/cart')">← 返回购物车</el-button>
    </div>
    <h2>确认下单</h2>
    <div v-loading="loading" class="content">
      <el-alert
        v-if="errorMsg"
        type="error"
        :closable="false"
        show-icon
        :title="errorMsg"
        class="error"
      />
      <template v-else>
        <el-card class="panel">
          <template #header>
            <div class="panel-header">
              <span>收货地址</span>
              <el-button link type="primary" @click="router.push('/addresses')">管理地址</el-button>
            </div>
          </template>
          <el-empty
            v-if="addresses.length === 0"
            description="暂无收货地址"
            :image-size="60"
          >
            <el-button type="primary" @click="router.push('/addresses')">去新增地址</el-button>
          </el-empty>
          <el-radio-group v-else v-model="addressId" class="address-group">
            <el-radio v-for="address in addresses" :key="address.id" :value="address.id" border>
              {{ address.receiverName }} {{ address.receiverPhone }}
              <span class="address-detail">{{ address.addressDetail }}</span>
              <el-tag v-if="address.isDefault === 1" type="success" size="small">默认</el-tag>
            </el-radio>
          </el-radio-group>
        </el-card>

        <el-card class="panel">
          <template #header><span>商品清单</span></template>
          <ul class="item-list">
            <li v-for="item in cartItems" :key="item.id">
              <el-checkbox :model-value="selectedIds.includes(item.id)" @change="toggle(item.id)" />
              <span class="item-name">{{ item.productName }} ×{{ item.quantity }}</span>
              <span class="item-price">¥{{ formatPriceCent(item.subtotalCent) }}</span>
            </li>
          </ul>
          <div class="summary">
            <p>商品小计：¥{{ formatPriceCent(selectedTotalCent) }}</p>
            <p>配送费：¥{{ formatPriceCent(deliveryFeeCent) }}</p>
            <p class="total">合计：¥{{ formatPriceCent(selectedTotalCent + deliveryFeeCent) }}</p>
          </div>
        </el-card>

        <el-card class="panel">
          <template #header><span>备注</span></template>
          <el-input
            v-model="remark"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-word-limit
            placeholder="选填，最多 255 个字符"
          />
        </el-card>

        <div class="footer">
          <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
            提交订单
          </el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import { fetchAddresses } from '@/api/address'
import { fetchCartItems } from '@/api/cart'
import { createOrder } from '@/api/order'
import { fetchShop } from '@/api/shop'
import { formatPriceCent } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const errorMsg = ref('')
const addresses = ref([])
const cartItems = ref([])
const shop = ref(null)
const addressId = ref(null)
const selectedIds = ref([])
const remark = ref('')

const selectedItems = computed(() =>
  cartItems.value.filter((item) => selectedIds.value.includes(item.id))
)

const selectedTotalCent = computed(() =>
  selectedItems.value.reduce((sum, item) => sum + item.subtotalCent, 0)
)

const deliveryFeeCent = computed(() => shop.value?.deliveryPriceCent ?? 0)

const canSubmit = computed(
  () =>
    addressId.value != null &&
    selectedIds.value.length > 0 &&
    !submitting.value
)

function toggle(id) {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter((value) => value !== id)
  } else {
    selectedIds.value = [...selectedIds.value, id]
  }
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [addressList, cartList] = await Promise.all([
      fetchAddresses(),
      fetchCartItems()
    ])
    addresses.value = addressList
    cartItems.value = cartList
    shop.value = cartList.length > 0 ? await fetchShop(cartList[0].shopId) : null
    const defaultAddress = addressList.find((address) => address.isDefault === 1)
    addressId.value = defaultAddress ? defaultAddress.id : addressList[0]?.id ?? null
    selectedIds.value = cartList.map((item) => item.id)
  } catch (error) {
    errorMsg.value = error.response?.data?.msg || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!canSubmit.value) {
    return
  }
  submitting.value = true
  try {
    const order = await createOrder({
      addressId: addressId.value,
      cartItemIds: selectedIds.value,
      remark: remark.value || null
    })
    ElMessage.success('下单成功')
    router.push(`/orders/${order.id}`)
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '下单失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.checkout {
  padding: 8px 0;
}

.header {
  margin-bottom: 8px;
}

.content {
  margin-top: 8px;
}

.error {
  margin-bottom: 12px;
}

.panel {
  margin-bottom: 14px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.address-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: stretch;
}

.address-detail {
  color: #909399;
  margin: 0 8px;
}

.item-list {
  list-style: none;
}

.item-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.item-name {
  flex: 1;
}

.item-price {
  font-weight: 700;
}

.summary {
  text-align: right;
  margin-top: 8px;
  color: #606266;
}

.summary .total {
  color: #f56c6c;
  font-size: 16px;
  font-weight: 700;
}

.footer {
  text-align: right;
}
</style>
