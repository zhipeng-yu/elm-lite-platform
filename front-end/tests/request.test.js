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
