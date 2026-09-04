import request from './request'

// 用户接口，路径与 api-contract.md 冻结契约一致。
// 开发期由 request.js 的模拟适配器返回数据，后端就绪后关闭 VITE_USE_MOCK 即可联调。

export function registerUser(data) {
  return request.post('/users', data)
}

export function login(data) {
  return request.post('/auth/login', data)
}
