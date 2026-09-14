import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'
import { computed, ref } from 'vue'

test('数量更新未完成时不重复更新或删除，完成后重新拉取服务器数量', async () => {
  const source = await readFile(new URL('../src/views/cart/CartView.vue', import.meta.url), 'utf8')
  const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import.*$/gm, '')
  let finishUpdate, updates = 0, deletes = 0
  const context = vm.createContext({
    computed, ref, onMounted() {}, useRouter: () => ({}), ElMessage: {success() {}, error() {}},
    updateCartItem: () => { updates++; return new Promise(resolve => { finishUpdate = resolve }) },
    removeCartItem: async () => { deletes++ }, fetchCartItems: async () => [{id: 1, quantity: 2}]
  })
  vm.runInContext(script + '\nthis.page = {handleQuantity, handleRemove, busy, cartItems}', context)
  const page = context.page, item = {id: 1, quantity: 1}
  const first = page.handleQuantity(item, 2)
  await page.handleQuantity(item, 3)
  await page.handleRemove(item)
  assert.equal(updates, 1)
  assert.equal(deletes, 0)
  finishUpdate()
  await first
  assert.equal(page.cartItems.value[0].quantity, 2)
  assert.equal(page.busy.value, false)
})

test('删除响应丢失时重新拉取购物车，避免显示已经删除的商品', async () => {
  const source = await readFile(new URL('../src/views/cart/CartView.vue', import.meta.url), 'utf8')
  const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import.*$/gm, '')
  let loads = 0
  const context = vm.createContext({
    computed, ref, onMounted() {}, useRouter: () => ({}), ElMessage: {success() {}, error() {}},
    updateCartItem: async () => {}, removeCartItem: async () => { throw new Error('连接中断') },
    fetchCartItems: async () => { loads++; return [] }
  })
  vm.runInContext(script + '\nthis.page = {handleRemove, cartItems}', context)
  context.page.cartItems.value = [{id: 1, quantity: 1}]
  await context.page.handleRemove(context.page.cartItems.value[0])
  assert.equal(loads, 1)
  assert.deepEqual(context.page.cartItems.value, [])
})
