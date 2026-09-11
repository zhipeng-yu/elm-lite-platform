import request from './request'

// 管理员接口，路径与 api-contract.md 第 9.5 节冻结契约一致。
// 开发期由 request.js 的模拟适配器返回数据，后端就绪后设置 VITE_USE_MOCK=false 即可联调。

export function adminLogin(username, password) {
  return request.post('/admin/auth/login', { username, password })
}

export function fetchAdminUsers(params = {}) {
  return request.get('/admin/users', { params })
}

export function fetchAdminMerchants(params = {}) {
  return request.get('/admin/merchants', { params })
}

export function fetchAdminShops() {
  return request.get('/admin/shops')
}

export function fetchAdminOrders(params = {}) {
  return request.get('/admin/orders', { params })
}

export function fetchAdminOrder(id) {
  return request.get(`/admin/orders/${id}`)
}

export function updateUserStatus(id, status) {
  return request.patch(`/admin/users/${id}`, { status })
}

export function updateMerchantStatus(id, status) {
  return request.patch(`/admin/merchants/${id}`, { status })
}
