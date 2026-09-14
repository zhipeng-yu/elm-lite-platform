import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'
import { ref, computed, watch, nextTick } from 'vue'
import { applicableCoupons, couponDiscountCent } from '../src/utils/coupon.js'

async function page() {
  const source = await readFile(new URL('../src/views/order/CheckoutView.vue', import.meta.url), 'utf8')
  const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import[\s\S]*?from ['"][^'"]+['"]\s*$/gm, '')
  const data = {
    addresses: [{ id: 1, isDefault: 1 }, { id: 2, isDefault: 0 }],
    items: [{ id: 10, shopId: 1, quantity: 1, stock: 5, status: 1, subtotalCent: 500 }],
    shop: { id: 1, businessStatus: 1, startPriceCent: 1000, deliveryPriceCent: 200 },
    coupons: [], calls: 0
  }
  const context = vm.createContext({
    ref, computed, watch, onMounted() {}, onActivated() {},
    useRouter: () => ({ push() {}, replace() {} }),
    fetchAddresses: async () => data.addresses,
    fetchCartItems: async () => data.items,
    fetchShop: async () => data.shop,
    listMyCoupons: async () => data.coupons,
    createOrder: async () => { data.calls++; return { id: 9 } },
    applicableCoupons, couponDiscountCent,
    formatPriceCent: value => (value / 100).toFixed(2),
    ElMessage: { success() {}, error() {} }
  })
  vm.runInContext(script + '\nthis.page = {load, canSubmit, handleSubmit, selectedIds, addressId, remark, selectedUserCouponId}', context)
  return { data, page: context.page }
}

test('未达到起送价、休息店铺、下架和库存不足均不能提交', async () => {
  const { data, page: p } = await page()
  await p.load()
  assert.equal(p.canSubmit.value, false)
  await p.handleSubmit()
  assert.equal(data.calls, 0)
  data.shop.startPriceCent = 0
  await p.load()
  assert.equal(p.canSubmit.value, true)
  data.shop.businessStatus = 0
  await p.load()
  assert.equal(p.canSubmit.value, false)
  data.shop.businessStatus = 1
  data.items[0].stock = 0
  await p.load()
  assert.equal(p.canSubmit.value, false)
  data.items[0].stock = 5
  data.items[0].status = 0
  await p.load()
  assert.equal(p.canSubmit.value, false)
})

test('从地址或优惠券页返回，保留商品勾选、地址、备注和仍有效的券', async () => {
  const { data, page: p } = await page()
  data.shop.startPriceCent = 0
  data.items.push({ ...data.items[0], id: 11 })
  data.coupons = [{ userCouponId: 7, shopId: 1, status: 'AVAILABLE', enabled: true, thresholdCent: 0, discountCent: 100, startsAt: '2020-01-01T00:00:00+08:00', expiresAt: '2099-01-01T00:00:00+08:00' }]
  await p.load()
  await nextTick()
  p.selectedIds.value = [10]
  p.addressId.value = 2
  p.remark.value = '不要香菜'
  p.selectedUserCouponId.value = 7
  await p.load()
  await nextTick()
  assert.deepEqual(Array.from(p.selectedIds.value), [10])
  assert.equal(p.addressId.value, 2)
  assert.equal(p.remark.value, '不要香菜')
  assert.equal(p.selectedUserCouponId.value, 7)
  data.addresses = [data.addresses[0]]
  await p.load()
  assert.equal(p.addressId.value, null)
})
