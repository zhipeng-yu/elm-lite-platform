import request from './request'

// 收货地址接口，路径与 api-contract.md 第 5 节冻结契约一致。
// 开发期由 request.js 的模拟适配器返回数据，后端就绪后关闭 VITE_USE_MOCK 即可联调。

export function fetchAddresses() {
  return request.get('/addresses')
}

export function fetchAddress(id) {
  return request.get(`/addresses/${id}`)
}

export function createAddress(data) {
  return request.post('/addresses', data)
}

export function updateAddress(id, data) {
  return request.patch(`/addresses/${id}`, data)
}

export function deleteAddress(id) {
  return request.delete(`/addresses/${id}`)
}
