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

// ---------- 开发期模拟接口（按冻结契约，D3 页面验证用） ----------
// 只在 dev 模式生效，设置 VITE_USE_MOCK=false 可关闭；后端接口就绪后关闭即可联调真实接口。
if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK !== 'false') {
  const realAdapter = service.defaults.adapter

  // D2 演示页用的模拟商家数据
  const MOCK_MERCHANTS = [
    { id: 1, name: '万家饺子（软件园E18店）', status: 'OPEN', rating: 4.9 },
    { id: 2, name: '兰州牛肉面（总店）', status: 'OPEN', rating: 4.7 }
  ]

  // D3 模拟用户与店铺，字段与 api-contract.md 冻结契约一致
  const MOCK_USERS = [
    { id: 1, username: 'demo', password: '12345678', displayName: '演示用户' }
  ]
  const MOCK_SHOPS = [
    {
      id: 1,
      shopName: '校园美食店',
      description: '用于本地开发和测试的演示店铺',
      imageUrl: null,
      startPriceCent: 1500,
      deliveryPriceCent: 300,
      businessStatus: 1,
      address: '测试地址1号'
    },
    {
      id: 2,
      shopName: '深夜食堂',
      description: '只在晚上营业的夜宵店铺',
      imageUrl: null,
      startPriceCent: 2000,
      deliveryPriceCent: 400,
      businessStatus: 0,
      address: '测试地址2号'
    },
    {
      id: 3,
      shopName: '小憩咖啡',
      description: '饮品店铺，目前临时闭店',
      imageUrl: null,
      startPriceCent: 3000,
      deliveryPriceCent: 500,
      businessStatus: 2,
      address: '测试地址3号'
    }
  ]

  const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

  const ok = (config, data, status = 200) => ({
    data: { code: 0, msg: 'success', data },
    status,
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

  const parseBody = (config) => {
    if (typeof config.data === 'string') {
      try {
        return JSON.parse(config.data)
      } catch {
        return {}
      }
    }
    return config.data || {}
  }

  service.defaults.adapter = async (config) => {
    const { url, method } = config

    // D2 演示页使用的 /mock 前缀接口，保留原行为
    if (url?.startsWith('/mock')) {
      await wait(800)
      if (url === '/mock/merchants') {
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

    // ---------- 按冻结契约的模拟接口：注册、登录、店铺列表、店铺详情 ----------
    if (method === 'post' && url === '/users') {
      await wait(600)
      const body = parseBody(config)
      if (MOCK_USERS.some((u) => u.username === body.username)) {
        return Promise.reject(fail(config, 409, '用户名已存在'))
      }
      const user = {
        id: MOCK_USERS.length + 1,
        username: body.username,
        displayName: body.displayName
      }
      MOCK_USERS.push({ ...user, password: body.password })
      return ok(config, user, 201)
    }

    if (method === 'post' && url === '/auth/login') {
      await wait(600)
      const body = parseBody(config)
      const found = MOCK_USERS.find(
        (u) => u.username === body.username && u.password === body.password
      )
      if (!found) {
        return Promise.reject(fail(config, 401, '账号或密码错误'))
      }
      return ok(config, {
        accessToken: `mock-jwt-${found.id}-${Date.now()}`,
        expiresIn: 3600,
        user: {
          id: found.id,
          username: found.username,
          displayName: found.displayName
        }
      })
    }

    if (method === 'get' && url === '/shops') {
      await wait(600)
      const mode = config.params?.mode
      if (mode === 'empty') {
        return ok(config, [])
      }
      if (mode === 'error') {
        return Promise.reject(fail(config, 500, '服务器开小差了，请稍后重试'))
      }
      // 列表不返回 address，与契约一致
      return ok(
        config,
        MOCK_SHOPS.map(({ address, ...rest }) => rest)
      )
    }

    if (method === 'get' && /^\/shops\/\d+$/.test(url)) {
      await wait(600)
      const id = Number(url.split('/').pop())
      const shop = MOCK_SHOPS.find((s) => s.id === id)
      if (!shop) {
        return Promise.reject(fail(config, 404, '店铺不存在'))
      }
      return ok(config, shop)
    }

    return realAdapter(config)
  }
}

export default service
