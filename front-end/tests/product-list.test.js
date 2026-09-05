import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'
import test from 'node:test'

// 执行页面的实际加载逻辑，只替换 Vue 生命周期和可控的接口响应。
const source = (await readFile(new URL('../src/views/product/ProductListView.vue', import.meta.url), 'utf8'))
  .match(/<script setup>([\s\S]*?)<\/script>/)[1]
  .replace(/^import .*$/gm, '')

for (const staleFails of [false, true]) {
  for (const latestFirst of [false, true]) {
    test(`切换分类忽略旧请求${staleFails ? '失败' : '成功'}，${latestFirst ? '新' : '旧'}请求先返回`, async () => {
      const requests = []
      const context = vm.createContext({
        ref: (value) => ({ value }), onMounted() {}, watch() {},
        useRoute: () => ({ params: { id: '1' }, query: {} }), useRouter: () => ({}),
        fetchProducts: (shopId, categoryId) => new Promise((resolve, reject) => {
          requests.push({ shopId, categoryId, resolve, reject })
        })
      })
      vm.runInContext(source + '\nglobalThis.page = { activeTab, products, errorMsg, loading, loadProducts }', context)
      const page = context.page
      page.activeTab.value = '1'
      const old = page.loadProducts()
      page.activeTab.value = '2'
      const latest = page.loadProducts()
      assert.equal(requests[0].categoryId, 1)
      assert.equal(requests[1].categoryId, 2)

      const finishOld = () => staleFails
        ? requests[0].reject({ response: { data: { msg: '旧分类加载失败' } } })
        : requests[0].resolve([{ id: 10, categoryId: 1 }])
      const finishLatest = () => requests[1].resolve([{ id: 20, categoryId: 2 }])
      if (latestFirst) {
        finishLatest()
        await latest
        finishOld()
        await old
      } else {
        finishOld()
        await old
        assert.equal(page.loading.value, true)
        assert.equal(page.errorMsg.value, '')
        assert.equal(page.products.value.length, 0)
        finishLatest()
        await latest
      }
      assert.equal(page.loading.value, false)
      assert.equal(page.errorMsg.value, '')
      assert.equal(page.products.value.length, 1)
      assert.equal(page.products.value[0].categoryId, 2)
    })
  }
}
