import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'
import test from 'node:test'
import axios from 'axios'

// 执行真实拦截器和 mock 分流代码，只替换浏览器 UI/存储依赖。
const source = (await readFile(new URL('../src/api/request.js', import.meta.url), 'utf8'))
  .replace(/^import .*$/gm, '')
  .replaceAll('import.meta.env', 'env')
  .replace('export default service', 'globalThis.service = service')

function adminMock() {
  let token = null
  let now = Date.now()
  const context = vm.createContext({
    axios,
    env: { DEV: true, VITE_USE_MOCK: 'true' },
    ElMessage: { error() {} },
    router: { currentRoute: { value: { path: '/admin/login' } }, push() {} },
    getToken: () => token,
    removeToken() {
      token = null
    },
    setTimeout: (callback) => callback(),
    Date: class extends Date {
      static now() {
        return now
      }
    }
  })
  vm.runInContext(source, context)
  return {
    service: context.service,
    async login(username, password) {
      const session = await context.service.post('/admin/auth/login', { username, password })
      token = session.accessToken
      return token
    },
    useToken(value) {
      token = value
    }
  }
}

test('管理员 mock：登录、身份校验与账号停用', async () => {
  const mock = adminMock()
  const { service } = mock

  // 登录失败与成功
  await assert.rejects(service.post('/admin/auth/login', { username: 'admin', password: 'wrong' }),
    (e) => e.response?.status === 401)
  await mock.login('admin', 'admin123456')

  // 用户与商家列表：字段形状与筛选
  const users = await service.get('/admin/users')
  assert.equal(users.length, 2)
  assert.ok(users.every((u) => !('password' in u)))
  const blocked = await service.get('/admin/users', { params: { status: 0 } })
  assert.equal(blocked.length, 1)
  assert.equal(blocked[0].username, 'blocked_user')
  const keyword = await service.get('/admin/users', { params: { keyword: 'demo' } })
  assert.equal(keyword.length, 1)

  const merchants = await service.get('/admin/merchants', { params: { status: 1 } })
  assert.equal(merchants.length, 1)
  assert.equal(merchants[0].account, 'merchant_a')
  assert.ok(merchants.every((m) => !('contactPhone' in m)))

  const shops = await service.get('/admin/shops')
  assert.ok(shops.length >= 1 && shops.every((s) => 'shopName' in s))

  // 停用/启用用户与非法参数
  const disabled = await service.patch('/admin/users/2', { status: 0 })
  assert.equal(disabled.status, 0)
  await assert.rejects(service.patch('/admin/users/2', { status: 2 }), (e) => e.response?.status === 400)
  await assert.rejects(service.patch('/admin/users/999', { status: 0 }), (e) => e.response?.status === 404)
  const enabled = await service.patch('/admin/users/2', { status: 1 })
  assert.equal(enabled.status, 1)

  await assert.rejects(service.patch('/admin/merchants/1', { status: 3 }), (e) => e.response?.status === 400)
})

test('管理员 mock：订单查询与越权拦截', async () => {
  const mock = adminMock()
  const { service } = mock

  // 匿名与用户身份均不能访问管理端
  await assert.rejects(service.get('/admin/orders'), (e) => e.response?.status === 401)
  const userSession = await service.post('/auth/login', { username: 'demo', password: '12345678' })
  mock.useToken(userSession.accessToken)
  await assert.rejects(service.get('/admin/orders'), (e) => e.response?.status === 403)
  await assert.rejects(service.get('/admin/users'), (e) => e.response?.status === 403)

  // 管理员身份查询订单：筛选与详情
  await mock.login('admin', 'admin123456')
  const all = await service.get('/admin/orders')
  assert.ok(all.some((o) => o.orderNo === 'ADMIN900000001'))
  const making = await service.get('/admin/orders', { params: { orderStatus: 2 } })
  assert.ok(making.length >= 1 && making.every((o) => o.orderStatus === 2))
  await assert.rejects(service.get('/admin/orders', { params: { orderStatus: 9 } }),
    (e) => e.response?.status === 400)

  const detail = await service.get('/admin/orders/900')
  assert.equal(detail.receiverName, '示例用户')
  assert.equal(detail.items.length, 1)
  assert.equal(detail.items[0].productName, '牛肉盖饭')
  await assert.rejects(service.get('/admin/orders/99999'), (e) => e.response?.status === 404)

  // 管理员 token 不能访问用户资源（cart 需 USER）
  await assert.rejects(service.get('/cart/items'), (e) => e.response?.status === 403)
})
