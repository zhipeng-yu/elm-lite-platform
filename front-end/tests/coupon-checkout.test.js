import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

import {
  applicableCoupons,
  couponDiscountCent,
  myCouponStatusLabel
} from '../src/utils/coupon.js'

const apiSource = await readFile(
  new URL('../src/api/coupon.js', import.meta.url),
  'utf8'
)
const checkoutSource = await readFile(
  new URL('../src/views/order/CheckoutView.vue', import.meta.url),
  'utf8'
)
const myCouponsSource = await readFile(
  new URL('../src/views/coupon/MyCouponsView.vue', import.meta.url),
  'utf8'
)

test('结算只展示当前店铺且达到门槛的可用券', () => {
  const coupons = [
    { userCouponId: 1, shopId: 10, thresholdCent: 2000, discountCent: 500, status: 'AVAILABLE' },
    { userCouponId: 2, shopId: 11, thresholdCent: 0, discountCent: 300, status: 'AVAILABLE' },
    { userCouponId: 3, shopId: 10, thresholdCent: 3000, discountCent: 600, status: 'AVAILABLE' },
    { userCouponId: 4, shopId: 10, thresholdCent: 0, discountCent: 200, status: 'USED' }
  ]

  assert.deepEqual(
    applicableCoupons(coupons, 10, 2500).map((coupon) => coupon.userCouponId),
    [1]
  )
})

test('优惠额不超过商品金额', () => {
  assert.equal(couponDiscountCent({ discountCent: 500 }, 2500), 500)
  assert.equal(couponDiscountCent({ discountCent: 5000 }, 2500), 2500)
  assert.equal(couponDiscountCent(null, 2500), 0)
})

test('我的券状态使用约定中文标签', () => {
  assert.equal(myCouponStatusLabel('AVAILABLE'), '可使用')
  assert.equal(myCouponStatusLabel('USED'), '已使用')
  assert.equal(myCouponStatusLabel('EXPIRED'), '已过期')
  assert.equal(myCouponStatusLabel('DISABLED'), '已停用')
})

test('前端接入我的券和下单 userCouponId', () => {
  assert.match(apiSource, /\/coupons\/mine/)
  assert.match(checkoutSource, /listMyCoupons/)
  assert.match(checkoutSource, /userCouponId/)
  assert.match(checkoutSource, /discountAmountCent/)
  assert.match(myCouponsSource, /listMyCoupons/)
})