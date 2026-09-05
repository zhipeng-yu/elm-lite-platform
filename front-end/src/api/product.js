import request from './request'

// 商品接口，路径与 api-contract.md 第 6 节冻结契约一致。

export function fetchProducts(shopId, categoryId) {
  return request.get(`/shops/${shopId}/products`, {
    params: categoryId ? { categoryId } : {}
  })
}

export function fetchProduct(id) {
  return request.get(`/products/${id}`)
}
