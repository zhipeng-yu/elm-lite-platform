import request from './request'

// 订单接口，路径与 api-contract.md 第 8 节冻结契约一致。
// 开发期由 request.js 的模拟适配器返回数据，后端就绪后关闭 VITE_USE_MOCK 即可联调。

export function createOrder(data) {
  return request.post('/orders', data)
}

export function fetchOrders() {
  return request.get('/orders')
}

export function fetchOrder(id) {
  return request.get(`/orders/${id}`)
}
