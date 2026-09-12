function toCent(value, fieldName) {
  const amount = Number(value)

  if (!Number.isFinite(amount)) {
    throw new Error(`${fieldName}必须是有效金额`)
  }

  return Math.round(amount * 100)
}

export function couponPayload(form) {
  const name = String(form?.name ?? '').trim()

  if (!name) {
    throw new Error('优惠券名称不能为空')
  }

  const thresholdCent = toCent(
    form?.thresholdYuan,
    '使用门槛'
  )

  if (thresholdCent < 0) {
    throw new Error('使用门槛不能为负数')
  }

  const discountCent = toCent(
    form?.discountYuan,
    '优惠金额'
  )

  if (discountCent <= 0) {
    throw new Error('优惠金额必须大于0')
  }

  const startsAt = form?.startsAt
  const expiresAt = form?.expiresAt

  if (!startsAt || !expiresAt) {
    throw new Error('开始时间和到期时间不能为空')
  }

  const startsTime = new Date(startsAt).getTime()
  const expiresTime = new Date(expiresAt).getTime()

  if (
    !Number.isFinite(startsTime) ||
    !Number.isFinite(expiresTime)
  ) {
    throw new Error('优惠券时间格式无效')
  }

  if (startsTime >= expiresTime) {
    throw new Error('到期时间必须晚于开始时间')
  }

  return {
    name,
    thresholdCent,
    discountCent,
    startsAt,
    expiresAt
  }
}

export function couponAvailability(
  coupon,
  now = new Date()
) {
  if (!coupon?.enabled) {
    return '已停用'
  }

  const currentTime = now.getTime()
  const startsTime =
    new Date(coupon.startsAt).getTime()
  const expiresTime =
    new Date(coupon.expiresAt).getTime()

  if (currentTime < startsTime) {
    return '未开始'
  }

  if (currentTime >= expiresTime) {
    return '已过期'
  }

  return '可领取'
}
const MY_COUPON_STATUS_LABELS = {
  AVAILABLE: '可使用',
  USED: '已使用',
  EXPIRED: '已过期',
  DISABLED: '已停用'
}

export function myCouponStatusLabel(status) {
  return MY_COUPON_STATUS_LABELS[status] ?? '状态未知'
}

export function applicableCoupons(
  coupons,
  shopId,
  productAmountCent
) {
  const amount = Number(productAmountCent)

  return (coupons ?? []).filter(
    (coupon) =>
      coupon.status === 'AVAILABLE' &&
      Number(coupon.shopId) === Number(shopId) &&
      amount >= Number(coupon.thresholdCent)
  )
}

export function couponDiscountCent(
  coupon,
  productAmountCent
) {
  if (!coupon) {
    return 0
  }

  const productAmount = Math.max(
    0,
    Number(productAmountCent) || 0
  )
  const discount = Math.max(
    0,
    Number(coupon.discountCent) || 0
  )

  return Math.min(productAmount, discount)
}