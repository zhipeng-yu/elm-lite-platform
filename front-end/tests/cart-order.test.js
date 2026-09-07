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

function cartOrderMock() {
  let token = null
  let now = Date.now()
  const context = vm.createContext({
    axios,
    env: { DEV: true, VITE_USE_MOCK: 'true' },
    ElMessage: { error() {} },
    router: { currentRoute: { value: { path: '/login' } }, push() {} },
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
    async login(username) {
      const session = await context.service.post('/auth/login', { username, password: '12345678' })
      token = session.accessToken
      return token
    },
    useToken(value) {
      token = value
    }
  }
}

test('购物车 mock：鉴权、加购、同店限制、数量与删除', async () => {
  const mock = cartOrderMock()
  const { service } = mock
  await assert.rejects(service.get('/cart/items'), (e) => e.response?.status === 401)
  await mock.login('demo')
  assert.equal((await service.get('/cart/items')).length, 0)

  // 加入商品：数量、小计与字段形状
  const added = await service.post('/cart/items', { productId: 1, quantity: 2 })
  assert.equal(added.productName, '牛肉盖饭')
  assert.equal(added.quantity, 2)
  assert.equal(added.subtotalCent, 3600)
  assert.equal(added.shopId, 1)
  assert.ok(!('userId' in added))

  // 重复加入累加数量
  const accumulated = await service.post('/cart/items', { productId: 1, quantity: 1 })
  assert.equal(accumulated.quantity, 3)

  // 跨店加入返回 409，原购物车不变
  await assert.rejects(service.post('/cart/items', { productId: 4, quantity: 1 }), (e) => e.response?.status === 409)
  assert.equal((await service.get('/cart/items')).length, 1)

  // 非法参数
  await assert.rejects(service.post('/cart/items', { productId: 0, quantity: 1 }), (e) => e.response?.status === 400)
  await assert.rejects(service.post('/cart/items', { productId: 1, quantity: 0 }), (e) => e.response?.status === 400)
  await assert.rejects(service.post('/cart/items', { productId: 999, quantity: 1 }), (e) => e.response?.status === 404)
  await assert.rejects(service.post('/cart/items', { productId: 2, quantity: 1 }), (e) => e.response?.status === 409) // 售罄商品库存不足

  // 修改数量与库存上限
  const updated = await service.patch(`/cart/items/${added.id}`, { quantity: 5 })
  assert.equal(updated.quantity, 5)
  await assert.rejects(service.patch(`/cart/items/${added.id}`, { quantity: 0 }), (e) => e.response?.status === 400)
  await assert.rejects(service.patch(`/cart/items/${added.id}`, { quantity: 9999 }), (e) => e.response?.status === 409)

  // 删除与重复删除
  await service.delete(`/cart/items/${added.id}`)
  assert.equal((await service.get('/cart/items')).length, 0)
  await assert.rejects(service.delete(`/cart/items/${added.id}`), (e) => e.response?.status === 404)
})

test('订单 mock：创建、校验、列表与详情，跑通下单全流程', async () => {
  const mock = cartOrderMock()
  const { service } = mock
  await mock.login('demo')

  // 准备购物车：牛肉盖饭 ×2 = 3600 分，达到起送价 1500
  const item = await service.post('/cart/items', { productId: 1, quantity: 2 })
  const address = (await service.get('/addresses'))[0]

  // 非法请求
  await assert.rejects(service.post('/orders', { addressId: address.id, cartItemIds: [] }), (e) => e.response?.status === 400)
  await assert.rejects(service.post('/orders', { addressId: 0, cartItemIds: [item.id] }), (e) => e.response?.status === 400)
  await assert.rejects(service.post('/orders', { addressId: address.id, cartItemIds: [item.id, item.id] }), (e) => e.response?.status === 400)
  await assert.rejects(service.post('/orders', { addressId: 999, cartItemIds: [item.id] }), (e) => e.response?.status === 404)

  // 正常下单
  const order = await service.post('/orders', {
    addressId: address.id,
    cartItemIds: [item.id],
    remark: ' 少辣 '
  })
  assert.equal(order.orderStatus, 0)
  assert.equal(order.productAmountCent, 3600)
  assert.equal(order.deliveryFeeCent, 300)
  assert.equal(order.totalAmountCent, 3900)
  assert.equal(order.remark, '少辣')
  assert.equal(order.items.length, 1)
  assert.equal(order.items[0].productName, '牛肉盖饭')
  assert.match(order.createdAt, /\+08:00$/)

  // 下单后购物车清空、库存扣减
  assert.equal((await service.get('/cart/items')).length, 0)
  const product = await service.get('/products/1')
  assert.equal(product.stock, 98)

  // 列表与详情
  const orders = await service.get('/orders')
  assert.equal(orders.length, 1)
  assert.ok(!('items' in orders[0]))
  const detail = await service.get(`/orders/${order.id}`)
  assert.equal(detail.receiverName, address.receiverName)
  assert.equal(detail.items.length, 1)
  await assert.rejects(service.get('/orders/999'), (e) => e.response?.status === 404)

  // 再次下单不存在的购物车项
  await assert.rejects(service.post('/orders', { addressId: address.id, cartItemIds: [item.id] }), (e) => e.response?.status === 404)
})

test('订单 mock：起送价与店铺营业校验', async () => {
  const mock = cartOrderMock()
  const { service } = mock
  await mock.login('demo')
  const address = (await service.get('/addresses'))[0]

  // 羊肉串 300 分 < 深夜食堂起送价 2000 分；且店铺休息
  const item = await service.post('/cart/items', { productId: 4, quantity: 1 })
  await assert.rejects(service.post('/orders', { addressId: address.id, cartItemIds: [item.id] }),
    (e) => e.response?.status === 409)
  // 购物车与库存保持不变
  assert.equal((await service.get('/cart/items')).length, 1)
  assert.equal((await service.get('/products/4')).stock, 50)
})
