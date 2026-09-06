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
  const MOCK_SESSIONS = new Map()
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

  // D4 模拟分类、商品与地址，字段与 api-contract.md 第 5、6 节冻结契约一致
  const MOCK_CATEGORIES = [
    { id: 1, shopId: 1, categoryName: '主食', sortOrder: 1, status: 1 },
    { id: 2, shopId: 1, categoryName: '饮品', sortOrder: 2, status: 1 },
    { id: 3, shopId: 2, categoryName: '烧烤', sortOrder: 1, status: 1 },
    { id: 4, shopId: 2, categoryName: '主食', sortOrder: 2, status: 1 },
    { id: 5, shopId: 3, categoryName: '咖啡', sortOrder: 1, status: 1 },
    { id: 6, shopId: 3, categoryName: '甜品', sortOrder: 2, status: 1 }
  ]
  const MOCK_PRODUCTS = [
    {
      id: 1,
      shopId: 1,
      categoryId: 1,
      productName: '牛肉盖饭',
      description: '演示主食商品',
      imageUrl: null,
      priceCent: 1800,
      stock: 100,
      status: 1
    },
    {
      id: 2,
      shopId: 1,
      categoryId: 2,
      productName: '柠檬水',
      description: '演示饮品商品',
      imageUrl: null,
      priceCent: 650,
      stock: 0,
      status: 1
    },
    {
      id: 3,
      shopId: 1,
      categoryId: 1,
      productName: '下架示例商品',
      description: 'status=0，不出现在公开列表',
      imageUrl: null,
      priceCent: 1500,
      stock: 10,
      status: 0
    },
    {
      id: 4,
      shopId: 2,
      categoryId: 3,
      productName: '羊肉串',
      description: '炭火现烤',
      imageUrl: null,
      priceCent: 300,
      stock: 50,
      status: 1
    },
    {
      id: 5,
      shopId: 2,
      categoryId: 4,
      productName: '蛋炒饭',
      description: '经典主食',
      imageUrl: null,
      priceCent: 1200,
      stock: 30,
      status: 1
    },
    {
      id: 6,
      shopId: 3,
      categoryId: 5,
      productName: '美式咖啡',
      description: '现磨咖啡',
      imageUrl: null,
      priceCent: 1800,
      stock: 20,
      status: 1
    },
    {
      id: 7,
      shopId: 3,
      categoryId: 6,
      productName: '提拉米苏',
      description: '经典甜品',
      imageUrl: null,
      priceCent: 2800,
      stock: 0,
      status: 1
    }
  ]
  const MOCK_ADDRESSES = [
    {
      id: 1,
      userId: 1,
      receiverName: '测试用户',
      receiverPhone: '19900000001',
      addressDetail: '测试校区1号宿舍楼',
      addressLabel: '学校',
      isDefault: 1
    },
    {
      id: 2,
      userId: 1,
      receiverName: '测试用户',
      receiverPhone: '19900000001',
      addressDetail: '测试校区2号宿舍楼',
      addressLabel: null,
      isDefault: 0
    }
  ]
  let nextAddressId = 3

  // D5 模拟购物车与订单，字段与 api-contract.md 第 7、8 节冻结契约一致
  const MOCK_CART_ITEMS = []
  let nextCartItemId = 1
  const MOCK_ORDERS = []
  let nextOrderId = 1

  const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

  const ok = (config, data, status = 200) => ({
    data: { code: 0, msg: 'success', data },
    status,
    statusText: 'OK',
    headers: {},
    config
  })

  const fail = (config, status, msg, data = null) => {
    const error = new Error(`Request failed with status code ${status}`)
    error.config = config
    error.isAxiosError = true
    error.response = { status, data: { code: status, msg, data }, headers: {}, config }
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

  const addressData = ({ userId, ...address }) => address

  const addressFields = (config, current) => {
    let body
    try {
      body = typeof config.data === 'string' ? JSON.parse(config.data) : config.data
    } catch {
      throw fail(config, 400, '地址请求格式错误')
    }
    if (!body || typeof body !== 'object' || Array.isArray(body)) {
      throw fail(config, 400, '地址请求必须为对象')
    }
    const writable = ['receiverName', 'receiverPhone', 'addressDetail', 'addressLabel', 'isDefault']
    for (const [key, value] of Object.entries(body)) {
      if (!writable.includes(key) || (value !== null && (key === 'isDefault'
        ? !Number.isInteger(value) : typeof value !== 'string'))) {
        throw fail(config, 400, '不支持的地址字段或字段类型错误')
      }
    }
    if (current && Object.keys(body).length === 0) {
      throw fail(config, 400, '修改内容不能为空')
    }
    const fields = { isDefault: 0, addressLabel: null, ...current, ...body }
    for (const key of writable) {
      if (typeof fields[key] === 'string') fields[key] = fields[key].trim()
    }
    fields.addressLabel = fields.addressLabel || null
    const fieldErrors = {}
    if (!fields.receiverName || fields.receiverName.length > 50) fieldErrors.receiverName = '收货人不能为空且不能超过50位'
    if (!/^1[0-9]{10}$/.test(fields.receiverPhone || '')) fieldErrors.receiverPhone = '联系电话必须为1开头的11位数字'
    if (!fields.addressDetail || fields.addressDetail.length > 255) fieldErrors.addressDetail = '详细地址不能为空且不能超过255位'
    if (fields.addressLabel?.length > 20) fieldErrors.addressLabel = '地址标签不能超过20位'
    if (fields.isDefault !== 0 && fields.isDefault !== 1) fieldErrors.isDefault = '默认标记必须为0或1'
    if (Object.keys(fieldErrors).length) throw fail(config, 400, '参数校验失败', { fieldErrors })
    return fields
  }

  const requireSession = (config) => {
    const token = config.headers?.Authorization?.match(/^Bearer (.+)$/)?.[1]
    const session = MOCK_SESSIONS.get(token)
    if (!session || session.expiresAt <= Date.now()) {
      throw fail(config, 401, '未登录或登录已过期，请重新登录')
    }
    return session
  }

  const cartItemView = (item) => {
    const product = MOCK_PRODUCTS.find((p) => p.id === item.productId)
    return {
      id: item.id,
      productId: product.id,
      shopId: product.shopId,
      productName: product.productName,
      imageUrl: product.imageUrl,
      priceCent: product.priceCent,
      stock: product.stock,
      status: product.status,
      quantity: item.quantity,
      subtotalCent: product.priceCent * item.quantity
    }
  }

  const orderSummary = (order) => ({
    id: order.id,
    orderNo: order.orderNo,
    shopId: order.shopId,
    orderStatus: order.orderStatus,
    totalAmountCent: order.totalAmountCent,
    createdAt: order.createdAt
  })

  const orderView = (order) => ({ ...orderSummary(order), ...order.detail })

  const isoWithOffset = () =>
    new Date(Date.now() + 8 * 3600_000).toISOString().replace('Z', '+08:00')

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
      const accessToken = `mock-jwt-${found.id}-${Date.now()}`
      MOCK_SESSIONS.set(accessToken, { userId: found.id, expiresAt: Date.now() + 3600_000 })
      return ok(config, {
        accessToken,
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

    // ---------- D4：分类、商品公开查询与详情 ----------
    if (method === 'get' && /^\/shops\/\d+\/categories$/.test(url)) {
      await wait(600)
      const shopId = Number(url.split('/')[2])
      if (!MOCK_SHOPS.some((s) => s.id === shopId)) {
        return Promise.reject(fail(config, 404, '店铺不存在'))
      }
      return ok(
        config,
        MOCK_CATEGORIES.filter((c) => c.shopId === shopId && c.status === 1)
          .sort((a, b) => a.sortOrder - b.sortOrder)
          .map(({ shopId: _shopId, status: _status, ...rest }) => rest)
      )
    }

    if (method === 'get' && /^\/shops\/\d+\/products$/.test(url)) {
      await wait(600)
      const shopId = Number(url.split('/')[2])
      if (!MOCK_SHOPS.some((s) => s.id === shopId)) {
        return Promise.reject(fail(config, 404, '店铺不存在'))
      }
      const categoryId = config.params?.categoryId
      return ok(
        config,
        MOCK_PRODUCTS.filter(
          (p) =>
            p.shopId === shopId &&
            p.status === 1 &&
            (!categoryId || p.categoryId === Number(categoryId))
        ).map(({ shopId: _shopId, ...rest }) => rest)
      )
    }

    if (method === 'get' && /^\/products\/\d+$/.test(url)) {
      await wait(600)
      const id = Number(url.split('/').pop())
      const product = MOCK_PRODUCTS.find((p) => p.id === id && p.status === 1)
      if (!product) {
        return Promise.reject(fail(config, 404, '商品不存在'))
      }
      const category = MOCK_CATEGORIES.find((c) => c.id === product.categoryId)
      return ok(config, { ...product, categoryName: category?.categoryName || '' })
    }

    // ---------- D4：收货地址接口（会话和归属均来自模拟登录） ----------
    if (url === '/addresses' || /^\/addresses\/[^/]+$/.test(url)) {
      await wait(600)
      const token = config.headers?.Authorization?.match(/^Bearer (.+)$/)?.[1]
      const session = MOCK_SESSIONS.get(token)
      if (!session || session.expiresAt <= Date.now()) {
        throw fail(config, 401, '未登录或登录已过期，请重新登录')
      }
      const userId = session.userId
      let item
      if (url !== '/addresses') {
        const idText = url.split('/').pop()
        const id = Number(idText)
        if (!/^-?\d+$/.test(idText) || !Number.isSafeInteger(id)) throw fail(config, 400, '地址ID格式错误')
        item = MOCK_ADDRESSES.find((address) => address.id === id)
        if (!item) throw fail(config, 404, '地址不存在')
        if (item.userId !== userId) throw fail(config, 403, '无权操作该地址')
      }
      if (method === 'get') {
        return ok(config, item ? addressData(item) : MOCK_ADDRESSES
          .filter((address) => address.userId === userId)
          .sort((a, b) => b.isDefault - a.isDefault || b.id - a.id)
          .map(addressData))
      }
      if ((method === 'post' && !item) || (method === 'patch' && item)) {
        const fields = addressFields(config, item)
        if (fields.isDefault === 1) {
          MOCK_ADDRESSES.filter((address) => address.userId === userId)
            .forEach((address) => { address.isDefault = 0 })
        }
        if (item) {
          Object.assign(item, fields)
        } else {
          item = { id: nextAddressId++, userId, ...fields }
          MOCK_ADDRESSES.push(item)
        }
        return ok(config, addressData(item), method === 'post' ? 201 : 200)
      }
      if (method === 'delete' && item) {
        MOCK_ADDRESSES.splice(MOCK_ADDRESSES.indexOf(item), 1)
        return ok(config, null)
      }
    }

    // ---------- D5：购物车接口（会话和归属均来自模拟登录） ----------
    if (url === '/cart/items' || /^\/cart\/items\/[^/]+$/.test(url)) {
      await wait(600)
      const session = requireSession(config)
      const userId = session.userId
      let item
      if (url !== '/cart/items') {
        const idText = url.split('/').pop()
        if (!/^\d+$/.test(idText)) throw fail(config, 400, '购物车ID格式错误')
        const id = Number(idText)
        item = MOCK_CART_ITEMS.find((c) => c.id === id)
        if (!item) throw fail(config, 404, '购物车商品不存在')
        if (item.userId !== userId) throw fail(config, 403, '无权操作该购物车商品')
      }
      if (method === 'get') {
        return ok(config, MOCK_CART_ITEMS.filter((c) => c.userId === userId).map(cartItemView))
      }
      if (method === 'post' && url === '/cart/items') {
        const body = parseBody(config)
        const { productId, quantity } = body
        if (!Number.isInteger(productId) || productId <= 0) throw fail(config, 400, '商品ID必须为正整数')
        if (!Number.isInteger(quantity) || quantity <= 0) throw fail(config, 400, '数量必须为正整数')
        const product = MOCK_PRODUCTS.find((p) => p.id === productId)
        if (!product) throw fail(config, 404, '商品不存在')
        if (product.status !== 1) throw fail(config, 409, '商品已下架')
        const existing = MOCK_CART_ITEMS.filter((c) => c.userId === userId)
        const otherShop = existing.find(
          (c) => MOCK_PRODUCTS.find((p) => p.id === c.productId).shopId !== product.shopId
        )
        if (otherShop) throw fail(config, 409, '购物车只允许同一店铺的商品')
        const dup = existing.find((c) => c.productId === productId)
        const targetQty = (dup?.quantity || 0) + quantity
        if (targetQty > product.stock) throw fail(config, 409, '库存不足')
        if (dup) {
          dup.quantity = targetQty
          return ok(config, cartItemView(dup), 201)
        }
        const created = { id: nextCartItemId++, userId, productId, quantity }
        MOCK_CART_ITEMS.push(created)
        return ok(config, cartItemView(created), 201)
      }
      if (method === 'patch' && item) {
        const body = parseBody(config)
        const keys = Object.keys(body)
        if (keys.length !== 1 || keys[0] !== 'quantity') throw fail(config, 400, '仅支持修改 quantity')
        if (!Number.isInteger(body.quantity) || body.quantity <= 0) throw fail(config, 400, '数量必须为正整数')
        const product = MOCK_PRODUCTS.find((p) => p.id === item.productId)
        if (body.quantity > product.stock) throw fail(config, 409, '库存不足')
        item.quantity = body.quantity
        return ok(config, cartItemView(item))
      }
      if (method === 'delete' && item) {
        MOCK_CART_ITEMS.splice(MOCK_CART_ITEMS.indexOf(item), 1)
        return ok(config, null)
      }
    }

    // ---------- D5：订单接口（创建、列表与详情） ----------
    if (url === '/orders' || /^\/orders\/[^/]+$/.test(url)) {
      await wait(600)
      const session = requireSession(config)
      const userId = session.userId
      let order
      if (url !== '/orders') {
        const idText = url.split('/').pop()
        if (!/^\d+$/.test(idText)) throw fail(config, 400, '订单ID格式错误')
        const id = Number(idText)
        order = MOCK_ORDERS.find((o) => o.id === id)
        if (!order) throw fail(config, 404, '订单不存在')
        if (order.userId !== userId) throw fail(config, 403, '无权查看该订单')
      }
      if (method === 'get') {
        if (order) return ok(config, orderView(order))
        return ok(
          config,
          MOCK_ORDERS.filter((o) => o.userId === userId)
            .sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id)
            .map(orderSummary)
        )
      }
      if (method === 'post' && url === '/orders') {
        const body = parseBody(config)
        const { addressId, cartItemIds, remark } = body
        if (!Number.isInteger(addressId) || addressId <= 0) throw fail(config, 400, '地址ID必须为正整数')
        if (!Array.isArray(cartItemIds) || cartItemIds.length === 0) throw fail(config, 400, '请选择要下单的购物车商品')
        if (
          cartItemIds.some((v) => !Number.isInteger(v) || v <= 0) ||
          new Set(cartItemIds).size !== cartItemIds.length
        ) {
          throw fail(config, 400, '购物车商品ID必须为不重复的正整数')
        }
        let remarkValue = null
        if (remark != null) {
          if (typeof remark !== 'string') throw fail(config, 400, '备注必须为字符串')
          remarkValue = remark.trim().slice(0, 255) || null
        }
        const address = MOCK_ADDRESSES.find((a) => a.id === addressId)
        if (!address) throw fail(config, 404, '地址不存在')
        if (address.userId !== userId) throw fail(config, 403, '无权使用该地址')
        const items = cartItemIds.map((id) => MOCK_CART_ITEMS.find((c) => c.id === id))
        if (items.some((c) => !c)) throw fail(config, 404, '购物车商品不存在')
        if (items.some((c) => c.userId !== userId)) throw fail(config, 403, '无权操作该购物车商品')
        const products = items.map((c) => MOCK_PRODUCTS.find((p) => p.id === c.productId))
        if (products.some((p) => p.status !== 1)) throw fail(config, 409, '商品已下架')
        if (products.some((p, i) => p.stock < items[i].quantity)) throw fail(config, 409, '库存不足')
        if (new Set(products.map((p) => p.shopId)).size !== 1) throw fail(config, 409, '只能对同一店铺的商品下单')
        const shop = MOCK_SHOPS.find((s) => s.id === products[0].shopId)
        if (shop.businessStatus !== 1) throw fail(config, 409, '店铺未营业，暂无法下单')
        const productAmountCent = products.reduce((sum, p, i) => sum + p.priceCent * items[i].quantity, 0)
        if (productAmountCent < shop.startPriceCent) throw fail(config, 409, '未达到店铺起送价')
        const totalAmountCent = productAmountCent + shop.deliveryPriceCent
        if (totalAmountCent > 9_999_999_999) throw fail(config, 409, '订单金额超出上限')
        products.forEach((p, i) => {
          p.stock -= items[i].quantity
        })
        items.forEach((c) => MOCK_CART_ITEMS.splice(MOCK_CART_ITEMS.indexOf(c), 1))
        const id = nextOrderId++
        const createdAt = isoWithOffset()
        const orderItems = items.map((c, i) => ({
          productId: products[i].id,
          productName: products[i].productName,
          unitPriceCent: products[i].priceCent,
          quantity: c.quantity,
          subtotalCent: products[i].priceCent * c.quantity
        }))
        order = {
          id,
          userId,
          orderNo: `TEST${Date.now()}${String(id).padStart(3, '0')}`,
          shopId: shop.id,
          orderStatus: 0,
          totalAmountCent,
          createdAt,
          detail: {
            receiverName: address.receiverName,
            receiverPhone: address.receiverPhone,
            deliveryAddress: address.addressDetail,
            productAmountCent,
            deliveryFeeCent: shop.deliveryPriceCent,
            remark: remarkValue,
            items: orderItems
          }
        }
        MOCK_ORDERS.push(order)
        return ok(config, orderView(order), 201)
      }
    }

    return axios.getAdapter(realAdapter)(config)
  }
}

export default service
