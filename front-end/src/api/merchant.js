import request from './request'

// 商家接口：D2 阶段调用开发期模拟接口，验证加载、空数据和错误三种状态；
// D3 契约冻结后改为请求真实接口 GET /api/v1/merchants。
export function fetchMerchants(mode = 'list') {
  return request.get('/mock/merchants', { params: { mode } })
}
