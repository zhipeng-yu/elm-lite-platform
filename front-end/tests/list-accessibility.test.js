import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const cases = [
  ['订单', '../src/views/order/OrderListView.vue', 'load'],
  ['商品', '../src/views/product/ProductListView.vue', 'loadProducts']
]

for (const [name, path, reload] of cases) {
  test(`${name}列表支持键盘打开详情和加载失败后重试`, async () => {
    const source = await readFile(new URL(path, import.meta.url), 'utf8')
    assert.match(source, /<li[^>]*role="link"[^>]*tabindex="0"[^>]*@keydown\.enter\.self="goDetail\([^)]*\)"[^>]*@keydown\.space\.self\.prevent="goDetail\([^)]*\)"/)
    assert.match(source, new RegExp(`v-if="errorMsg"[^>]*>[\\s\\S]*?@click="${reload}">重新加载`))
  })
}
