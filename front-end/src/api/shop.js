import request from './request'

// 店铺接口，路径与 api-contract.md 冻结契约一致。
// 开发期由 request.js 的模拟适配器返回数据，后端就绪后关闭 VITE_USE_MOCK 即可联调。

export function fetchShops() {
  return request.get('/shops')
}

export function fetchShop(id) {
  return request.get(`/shops/${id}`)
}
