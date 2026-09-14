import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'
import { ref } from 'vue'

test('骑手接单后打开配送信息，重复点击不重复接单，取消送达确认不发请求', async () => {
  const source = await readFile(new URL('../src/views/rider/RiderTaskView.vue', import.meta.url), 'utf8')
  const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import.*$/gm, '')
  let finishClaim, claims = 0, completes = 0
  const order = {id: 8, shopId: 2, orderStatus: 2, deliveryAddress: '虚构测试校区', receiverPhone: '13800000000', remark: '少辣'}
  const context = vm.createContext({
    ref, onMounted() {}, useRouter: () => ({}),
    fetchShop: async () => ({address: '虚构取餐点'}),
    ElMessage: {success() {}, error() {}}, ElMessageBox: {confirm: async () => {throw 'cancel'}},
    api: {
      claimOrder: () => { claims++; return new Promise(resolve => { finishClaim = resolve }) },
      getRiderOrder: async () => order, listRiderOrders: async () => [order],
      completeOrder: async () => { completes++ }
    }
  })
  vm.runInContext(script + '\nthis.page = {act, detail, pickup, detailVisible, tab, acting}', context)
  const page = context.page
  const first = page.act(order, 'claim')
  await page.act(order, 'claim')
  assert.equal(claims, 1)
  finishClaim(order)
  await first
  assert.equal(page.tab.value, 'mine')
  assert.equal(page.detailVisible.value, true)
  assert.equal(page.detail.value.deliveryAddress, order.deliveryAddress)
  assert.equal(page.detail.value.remark, '少辣')
  assert.equal(page.pickup.value.address, '虚构取餐点')
  await page.act({...order, orderStatus: 3}, 'complete')
  assert.equal(completes, 0)
  assert.equal(page.acting.value, false)
})

test('骑手切换任务列表失败时不保留上一个标签的旧订单', async () => {
  const source = await readFile(new URL('../src/views/rider/RiderTaskView.vue', import.meta.url), 'utf8')
  const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import.*$/gm, '')
  const context = vm.createContext({
    ref, onMounted() {}, useRouter: () => ({}), fetchShop: async () => ({}),
    ElMessage: {success() {}, error() {}}, ElMessageBox: {confirm: async () => true},
    api: {listAvailableOrders: async () => { throw new Error('连接中断') }}
  })
  vm.runInContext(script + '\nthis.page = {load, orders, error}', context)
  context.page.orders.value = [{id: 1}]
  await context.page.load()
  assert.deepEqual(context.page.orders.value, [])
  assert.equal(context.page.error.value, '加载失败')
})
