import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('商家订单加载失败后重试当前筛选，不跳回第一家店', async () => {
  const source = await readFile(new URL('../src/views/order/MerchantOrderListView.vue', import.meta.url), 'utf8')
  assert.match(source, /v-if="error"[^>]*>[\s\S]*?@click="loadOrders">重新加载/)
})

test('管理员工具栏和订单弹窗适配手机宽度', async () => {
  const source = await readFile(new URL('../src/views/admin/AdminConsoleView.vue', import.meta.url), 'utf8')
  assert.match(source, /width="min\(560px, calc\(100vw - 24px\)\)"/)
  assert.match(source, /\.toolbar\s*\{[\s\S]*?flex-wrap:\s*wrap/)
})
