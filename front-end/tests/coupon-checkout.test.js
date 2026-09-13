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
const routerSource = await readFile(
  new URL('../src/router/index.js', import.meta.url),
  'utf8'
)
const layoutSource = await readFile(
  new URL('../src/layouts/DefaultLayout.vue', import.meta.url),
  'utf8'
)

test('结算只展示当前店铺且达到门槛的可用券', () => {
  const coupons = [
    { userCouponId: 1, shopId: 10, thresholdCent: 2000, discountCent: 500, status: 'AVAILABLE' },
    { userCouponId: 2, shopId: 11, thresholdCent: 0, discountCent: 300, status: 'AVAILABLE' },
    { userCouponId: 3, shopId: 10, thresholdCent: 3000, discountCent: 600, status: 'AVAILABLE' },
    { userCouponId: 4, shopId: 10, thresholdCent: 0, discountCent: 200, status: 'USED' }
  ].map((coupon) => ({
    ...coupon,
    startsAt: '2026-09-12T10:00:00',
    expiresAt: '2026-09-12T14:00:00'
  }))
  const now = new Date('2026-09-12T12:00:00')

  assert.deepEqual(
    applicableCoupons(coupons, 10, 2500, now).map((coupon) => coupon.userCouponId),
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

test('用户可以从账户菜单进入我的优惠券页面', () => {
  assert.match(routerSource, /path:\s*'coupons\/mine'/)
  assert.match(routerSource, /MyCouponsView\.vue/)
  assert.match(routerSource, /path:\s*'coupons\/mine'[\s\S]*?accountType:\s*'USER'/)
  assert.match(layoutSource, /to="\/coupons\/mine"[^>]*>我的优惠券/)
})

test('结算校验优惠券有效期边界', () => {
  const now = new Date('2026-09-12T12:00:00')
  const coupon = {
    userCouponId: 5,
    shopId: 10,
    thresholdCent: 0,
    discountCent: 100,
    status: 'AVAILABLE',
    startsAt: '2026-09-12T12:00:00',
    expiresAt: '2026-09-13T12:00:00'
  }

  // 开始时刻可以使用
  assert.deepEqual(
    applicableCoupons([coupon], 10, 2500, now),
    [coupon]
  )

  // 未开始、刚到期、时间无效均不能使用
  for (const dates of [
    { startsAt: '2026-09-12T13:00:00' },
    { expiresAt: '2026-09-12T12:00:00' },
    { startsAt: 'invalid' }
  ]) {
    assert.deepEqual(
      applicableCoupons([{ ...coupon, ...dates }], 10, 2500, now),
      []
    )
  }
})
