import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

import {
  couponAvailability,
  couponPayload
} from '../src/utils/coupon.js'

const merchantDashboardSource = await readFile(
  new URL('../src/views/shop/MerchantDashboardView.vue', import.meta.url),
  'utf8'
)

test('优惠券表单转换为整数分并清理名称', () => {
  assert.deepEqual(
    couponPayload({
      name: ' 满减券 ',
      thresholdYuan: 20.01,
      discountYuan: 5.55,
      startsAt: '2026-09-11T10:00',
      expiresAt: '2026-09-12T10:00'
    }),
    {
      name: '满减券',
      thresholdCent: 2001,
      discountCent: 555,
      startsAt: '2026-09-11T10:00',
      expiresAt: '2026-09-12T10:00'
    }
  )
})

test('优惠券表单拒绝非法名称、金额和时间', () => {
  assert.throws(
    () => couponPayload({
      name: ' ',
      thresholdYuan: 0,
      discountYuan: 5,
      startsAt: '2026-09-11T10:00',
      expiresAt: '2026-09-12T10:00'
    }),
    /名称/
  )

  assert.throws(
    () => couponPayload({
      name: '测试券',
      thresholdYuan: -1,
      discountYuan: 5,
      startsAt: '2026-09-11T10:00',
      expiresAt: '2026-09-12T10:00'
    }),
    /门槛/
  )

  assert.throws(
    () => couponPayload({
      name: '测试券',
      thresholdYuan: 20,
      discountYuan: 0,
      startsAt: '2026-09-11T10:00',
      expiresAt: '2026-09-12T10:00'
    }),
    /优惠金额/
  )

  assert.throws(
    () => couponPayload({
      name: '测试券',
      thresholdYuan: 20,
      discountYuan: 5,
      startsAt: '2026-09-12T10:00',
      expiresAt: '2026-09-11T10:00'
    }),
    /到期时间/
  )
})

test('优惠券状态区分可领取、停用、未开始和过期', () => {
  const now = new Date('2026-09-11T12:00:00')

  const base = {
    enabled: true,
    startsAt: '2026-09-11T10:00:00',
    expiresAt: '2026-09-11T14:00:00'
  }

  assert.equal(
    couponAvailability(base, now),
    '可领取'
  )

  assert.equal(
    couponAvailability(
      { ...base, enabled: false },
      now
    ),
    '已停用'
  )

  assert.equal(
    couponAvailability(
      {
        ...base,
        startsAt: '2026-09-11T13:00:00'
      },
      now
    ),
    '未开始'
  )

  assert.equal(
    couponAvailability(
      {
        ...base,
        expiresAt: '2026-09-11T12:00:00'
      },
      now
    ),
    '已过期'
  )
})

test('商家工作台展示当前店铺的优惠券管理面板', () => {
  assert.match(
    merchantDashboardSource,
    /<MerchantCouponPanel\s+:shop-id="shopId"/
  )
})
