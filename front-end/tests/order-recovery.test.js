import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'
import { ref } from 'vue'

test('取消与商家确认冲突后仍显示订单，并获取最新状态', async () => {
  const source = await readFile(new URL('../src/views/order/OrderDetailView.vue', import.meta.url), 'utf8')
  const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import[\s\S]*?from ['"][^'"]+['"]\s*$/gm, '')
  const context = vm.createContext({
    ref, onMounted() {}, useRoute: () => ({params: {id: 1}}), useRouter: () => ({}),
    fetchOrder: async () => ({id: 1, orderStatus: 1}),
    cancelOrder: async () => { throw {response: {status: 409, data: {msg: '商家已接单，无法取消'}}} },
    ElMessageBox: {confirm: async () => true}, ElMessage: {success() {}, error() {}}
  })
  vm.runInContext(script + '\nthis.page = {cancel, order, errorMsg}', context)
  context.page.order.value = {id: 1, orderStatus: 0}
  await context.page.cancel()
  assert.equal(context.page.errorMsg.value, '')
  assert.equal(context.page.order.value.orderStatus, 1)
})
