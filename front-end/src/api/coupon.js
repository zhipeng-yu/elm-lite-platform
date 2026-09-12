import request from './request'

export const listClaimableCoupons = (shopId) =>
  request.get(`/shops/${shopId}/coupons`)

export const claimCoupon = (couponId) =>
  request.post(`/coupons/${couponId}/claims`)