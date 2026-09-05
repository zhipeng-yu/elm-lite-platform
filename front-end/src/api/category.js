import request from './request'

// 分类接口，路径与 api-contract.md 第 6 节冻结契约一致。

export function fetchCategories(shopId) {
  return request.get(`/shops/${shopId}/categories`)
}
