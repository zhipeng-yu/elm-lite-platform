import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { createServer } from 'node:http'
import vm from 'node:vm'
import test from 'node:test'
import axios from 'axios'

// 执行真实拦截器和 mock 分流代码，只替换浏览器 UI/存储依赖。
const source = (await readFile(new URL('../src/api/request.js', import.meta.url), 'utf8'))
  .replace(/^import .*$/gm, '')
  .replaceAll('import.meta.env', 'env')
  .replace('export default service', 'globalThis.service = service')

for (const useMock of ['true', 'false']) {
  test(`未模拟接口转发真实 HTTP，VITE_USE_MOCK=${useMock}`, async () => {
    const server = createServer((request, response) => {
      response.setHeader('Content-Type', 'application/json')
      response.end(JSON.stringify({ code: 0, msg: 'success', data: { path: request.url } }))
    })
    await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
    try {
      const context = vm.createContext({
        axios, env: { DEV: true, VITE_USE_MOCK: useMock },
        ElMessage: { error() {} },
        router: { currentRoute: { value: { path: '/login' } } },
        getToken: () => null, removeToken() {}, setTimeout
      })
      vm.runInContext(source, context)
      const data = await context.service.get('/users/me', {
        baseURL: `http://127.0.0.1:${server.address().port}/api/v1`
      })
      assert.equal(data.path, '/api/v1/users/me')
      if (useMock === 'true') {
        const shops = await context.service.get('/shops')
        assert.equal(shops[0].startPriceCent, 1500)
      }
    } finally {
      await new Promise((resolve) => server.close(resolve))
    }
  })
}

test('D4 模拟契约：分类、商品公开查询与地址鉴权/增删改查', async () => {
  let token = null
  const context = vm.createContext({
    axios,
    env: { DEV: true, VITE_USE_MOCK: 'true' },
    ElMessage: { error() {} },
    router: { currentRoute: { value: { path: '/login' } }, push() {} },
    getToken: () => token,
    removeToken() {
      token = null
    },
    setTimeout
  })
  vm.runInContext(source, context)

  // 公开分类：仅返回启用的分类，且不含 shopId
  const categories = await context.service.get('/shops/1/categories')
  assert.ok(categories.some((c) => c.categoryName === '主食'))
  assert.ok(categories.every((c) => !('shopId' in c)))

  // 公开商品：全部为 status=1，支持 categoryId 筛选
  const all = await context.service.get('/shops/1/products')
  assert.ok(all.length > 0 && all.every((p) => p.status === 1))
  const drinks = await context.service.get('/shops/1/products', {
    params: { categoryId: 2 }
  })
  assert.ok(drinks.length > 0 && drinks.every((p) => p.categoryId === 2))

  // 商品详情：补充 categoryName
  const detail = await context.service.get('/products/1')
  assert.equal(detail.categoryName, '主食')

  // 地址接口要求登录
  await assert.rejects(
    () => context.service.get('/addresses'),
    (e) => e.response?.status === 401
  )

  // 注册、登录后完成地址增删改查
  const created = await context.service.post('/users', {
    username: 'tester',
    password: '12345678',
    displayName: '测试'
  })
  assert.equal(created.username, 'tester')
  const loginData = await context.service.post('/auth/login', {
    username: 'tester',
    password: '12345678'
  })
  token = loginData.accessToken

  const addresses = await context.service.get('/addresses')
  assert.ok(Array.isArray(addresses) && addresses[0].isDefault === 1)

  const added = await context.service.post('/addresses', {
    receiverName: '张三',
    receiverPhone: '13800138000',
    addressDetail: '测试校区3号宿舍楼',
    addressLabel: '学校',
    isDefault: 1
  })
  assert.equal(added.id, 3)
  assert.equal(added.isDefault, 1)

  await context.service.patch(`/addresses/${added.id}`, { receiverName: '张三三' })
  const edited = await context.service.get(`/addresses/${added.id}`)
  assert.equal(edited.receiverName, '张三三')

  await context.service.delete(`/addresses/${added.id}`)
  await assert.rejects(
    () => context.service.delete(`/addresses/${added.id}`),
    (e) => e.response?.status === 404
  )
})
