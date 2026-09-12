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