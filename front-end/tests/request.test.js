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
  assert.ok(Array.isArray(addresses) && addresses.length === 0)

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

function addressMock() {
  let token = null
  let now = Date.now()
  const context = vm.createContext({
    axios, env: { DEV: true, VITE_USE_MOCK: 'true' },
    ElMessage: { error() {} },
    router: { currentRoute: { value: { path: '/login' } } },
    getToken: () => token, removeToken() { token = null },
    setTimeout: (callback) => callback(),
    Date: class extends Date { static now() { return now } }
  })
  vm.runInContext(source, context)
  return {
    service: context.service,
    async login(username) {
      const session = await context.service.post('/auth/login', { username, password: '12345678' })
      token = session.accessToken
      return token
    },
    useToken(value) { token = value },
    expire() { now += 3600_000 }
  }
}

test('地址 mock 校验会话、用户归属和每个用户的默认地址', async () => {
  const mock = addressMock()
  const { service } = mock
  mock.useToken('arbitrary-token')
  await assert.rejects(service.get('/addresses'), (e) => e.response?.status === 401)
  const demoToken = await mock.login('demo')
  const demoAddresses = await service.get('/addresses')
  assert.equal(demoAddresses.length, 2)
  assert.ok(demoAddresses.every((address) => !('userId' in address)))
  const demoSnapshot = JSON.stringify(demoAddresses)

  await service.post('/users', { username: 'other', password: '12345678', displayName: '另一用户' })
  const otherToken = await mock.login('other')
  assert.equal((await service.get('/addresses')).length, 0)
  for (const method of ['get', 'patch', 'delete']) {
    await assert.rejects(service[method]('/addresses/1', { isDefault: 1 }),
      (e) => e.response?.status === 403)
    await assert.rejects(service[method]('/addresses/999', { isDefault: 1 }),
      (e) => e.response?.status === 404)
  }
  const body = { receiverName: '新用户', receiverPhone: '19900000002', addressDetail: '测试地址', isDefault: 1 }
  const first = await service.post('/addresses', body)
  const second = await service.post('/addresses', body)
  const ownAddresses = await service.get('/addresses')
  assert.equal(ownAddresses.length, 2)
  assert.equal(ownAddresses[0].id, second.id)
  assert.equal(ownAddresses.filter((address) => address.isDefault === 1).length, 1)
  assert.ok(ownAddresses.every((address) => !('userId' in address)))
  await service.patch(`/addresses/${first.id}`, { isDefault: 1 })
  assert.equal((await service.get('/addresses'))[0].id, first.id)
  await service.delete(`/addresses/${first.id}`)
  assert.equal((await service.get('/addresses'))[0].isDefault, 0)

  mock.useToken(demoToken)
  assert.equal(JSON.stringify(await service.get('/addresses')), demoSnapshot)
  mock.useToken(otherToken)
  mock.expire()
  await assert.rejects(service.get('/addresses'), (e) => e.response?.status === 401)
})

test('地址 mock 写入遵守字段白名单、trim、边界及失败后数据不变', async () => {
  const mock = addressMock()
  const { service } = mock
  await mock.login('demo')
  const original = JSON.stringify(await service.get('/addresses'))
  const invalidBodies = [
    [{}, null],
    ...['id', 'userId', 'createdAt', 'updatedAt', 'extra'].map((field) => [{ [field]: 888 }, null]),
    ...[null, '', '   ', '名'.repeat(51)].map((value) => [{ receiverName: value }, 'receiverName']),
    ...[null, '', '29900000001', '1990000000'].map((value) => [{ receiverPhone: value }, 'receiverPhone']),
    ...[null, '', '   ', '址'.repeat(256)].map((value) => [{ addressDetail: value }, 'addressDetail']),
    [{ addressLabel: '签'.repeat(21) }, 'addressLabel'],
    ...[null, -1, 2].map((value) => [{ isDefault: value }, 'isDefault']),
    [{ receiverPhone: 19900000001 }, null],
    [{ isDefault: '1' }, null],
    [{ isDefault: true }, null],
    [{ isDefault: 0.5 }, null]
  ]
  for (const [body, field] of invalidBodies) {
    await assert.rejects(service.patch('/addresses/2', body), (e) => {
      assert.equal(e.response?.status, 400)
      if (field) assert.ok(e.response.data.data?.fieldErrors[field])
      return true
    })
  }
  await assert.rejects(service.post('/addresses', { isDefault: 1 }), (e) => e.response?.status === 400)
  await assert.rejects(service.get('/addresses/invalid'), (e) => e.response?.status === 400)
  assert.equal(JSON.stringify(await service.get('/addresses')), original)

  const added = await service.post('/addresses', {
    receiverName: `  ${'名'.repeat(50)}  `,
    receiverPhone: ' 19900000001 ',
    addressDetail: `  ${'址'.repeat(255)}  `,
    addressLabel: ` ${'签'.repeat(20)} `
  })
  assert.equal(added.receiverName, '名'.repeat(50))
  assert.equal(added.receiverPhone, '19900000001')
  assert.equal(added.addressDetail, '址'.repeat(255))
  assert.equal(added.addressLabel, '签'.repeat(20))
  assert.equal(added.isDefault, 0)
  for (const addressLabel of [null, '   ']) {
    const edited = await service.patch(`/addresses/${added.id}`, { receiverName: '  中 间  ', addressLabel })
    assert.equal(edited.receiverName, '中 间')
    assert.equal(edited.receiverPhone, added.receiverPhone)
    assert.equal(edited.addressLabel, null)
  }
})
