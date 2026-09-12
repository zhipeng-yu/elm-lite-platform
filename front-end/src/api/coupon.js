import request from './request'

export const listClaimableCoupons = (shopId) =>
  request.get(`/shops/${shopId}/coupons`)

export const claimCoupon = (couponId) =>
  request.post(`/coupons/${couponId}/claims`)
export const listMyCoupons = () =>
  request.get('/coupons/mine')