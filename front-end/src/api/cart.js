import request from './request'

// 购物车接口，路径与 api-contract.md 第 7 节冻结契约一致。
// 开发期由 request.js 的模拟适配器返回数据，后端就绪后关闭 VITE_USE_MOCK 即可联调。

export function fetchCartItems() {
  return request.get('/cart/items')
}

export function addCartItem(productId, quantity) {
  return request.post('/cart/items', { productId, quantity })
}

export function updateCartItem(id, quantity) {
  return request.patch(`/cart/items/${id}`, { quantity })
}

export function removeCartItem(id) {
  return request.delete(`/cart/items/${id}`)
}
