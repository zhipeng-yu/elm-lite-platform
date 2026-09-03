import axios from 'axios'
import { ElMessage } from 'element-plus'

import router from '@/router'
import { getToken, removeToken } from '@/utils/auth'

// 统一 Axios 实例：基础地址 /api/v1，超时 5 秒。
// 约定见 api-contract.md：统一 code/msg/data 响应、Bearer Token、401 跳转登录。
const service = axios.create({
  baseURL: '/api/v1',
  timeout: 5000
})

// 请求拦截器：自动附加认证头
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：按统一响应结构解包 data，并集中处理业务错误与 HTTP 错误
service.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data
      }
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      // 未登录或 Token 过期：清除 Token，提示并跳转登录页
      removeToken()
      ElMessage.error(error.response?.data?.msg || '登录已过期，请重新登录')
      const current = router.currentRoute.value
      if (current.path !== '/login') {
        router.push({ path: '/login', query: { redirect: current.fullPath } })
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error(error.response?.data?.msg || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

// ---------- 开发期模拟接口（D2 验证用，D3 接入真实接口后整体删除） ----------
// 只在 dev 模式生效，设置 VITE_USE_MOCK=false 可关闭。
if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK !== 'false') {
  const realAdapter = service.defaults.adapter

  const MOCK_MERCHANTS = [
    { id: 1, name: '万家饺子（软件园E18店）', status: 'OPEN', rating: 4.9 },
    { id: 2, name: '兰州牛肉面（总店）', status: 'OPEN', rating: 4.7 }
  ]

  const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

  const ok = (config, data) => ({
    data: { code: 0, msg: 'success', data },
    status: 200,
    statusText: 'OK',
    headers: {},
    config
  })

  const fail = (config, status, msg) => {
    const error = new Error(`Request failed with status code ${status}`)
    error.config = config
    error.isAxiosError = true
    error.response = { status, data: { code: status, msg }, headers: {}, config }
    return error
  }

  service.defaults.adapter = async (config) => {
    // 只拦截 /mock 前缀的模拟接口，其余请求走真实网络
    if (!config.url?.startsWith('/mock')) {
      return realAdapter(config)
    }
    await wait(800)

    if (config.url === '/mock/merchants') {
      // 模拟鉴权：未携带 Token 返回 401，用于验证统一鉴权与跳转登录
      if (!config.headers?.Authorization) {
        return Promise.reject(fail(config, 401, '未登录，请先登录'))
      }
      const mode = config.params?.mode
      if (mode === 'empty') {
        return ok(config, [])
      }
      if (mode === 'error') {
        return Promise.reject(fail(config, 500, '服务器开小差了，请稍后重试'))
      }
      return ok(config, MOCK_MERCHANTS)
    }

    return Promise.reject(fail(config, 404, '接口不存在'))
  }
}

export default service
