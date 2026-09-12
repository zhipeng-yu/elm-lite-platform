import request from './request'

// 商家接口：D2 阶段调用开发期模拟接口，验证加载、空数据和错误三种状态；
// D3 契约冻结后改为请求真实接口 GET /api/v1/merchants。
export function fetchMerchants(mode = 'list') {
  return request.get('/mock/merchants', { params: { mode } })
}

export const registerMerchant = (data) => request.post('/merchants', data)
export const loginMerchant = (data) => request.post('/merchant/auth/login', data)
export const listMyShops = () => request.get('/merchant/shops')
export const createShop = (data) => request.post('/merchant/shops', data)
export const setBusinessStatus = (id, businessStatus) => request.patch(`/merchant/shops/${id}`, { businessStatus })
export const listManagedCategories = (id) => request.get(`/merchant/shops/${id}/categories`)
export const listManagedProducts = (id) => request.get(`/merchant/shops/${id}/products`)
export const createCategory = (id, data) => request.post(`/merchant/shops/${id}/categories`, data)
export const updateCategory = (id, data) => request.patch(`/merchant/categories/${id}`, data)
export const createProduct = (id, data) => request.post(`/merchant/shops/${id}/products`, data)
export const updateProduct = (id, data) => request.patch(`/merchant/products/${id}`, data)

export const listManagedCoupons = (shopId) =>
  request.get(`/merchant/shops/${shopId}/coupons`)

export const createCoupon = (shopId, data) =>
  request.post(`/merchant/shops/${shopId}/coupons`, data)

export const updateCouponEnabled = (couponId, enabled) =>
  request.patch(`/merchant/coupons/${couponId}`, { enabled })

export const listMerchantOrders = (shopId, orderStatus) => request.get(`/merchant/shops/${shopId}/orders`, {
  params: orderStatus === '' ? {} : { orderStatus }
})
export const fetchMerchantOrder = (id) => request.get(`/merchant/orders/${id}`)
export const confirmMerchantOrder = (id) => request.post(`/merchant/orders/${id}/confirm`)
export const prepareMerchantOrder = (id) => request.post(`/merchant/orders/${id}/prepare`)
