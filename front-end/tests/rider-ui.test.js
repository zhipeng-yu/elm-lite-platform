import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('rider mobile workflow exposes auth, task routes and actions', async () => {
  const [router, api, page] = await Promise.all([
    readFile(new URL('../src/router/index.js', import.meta.url), 'utf8'),
    readFile(new URL('../src/api/rider.js', import.meta.url), 'utf8'),
    readFile(new URL('../src/views/rider/RiderTaskView.vue', import.meta.url), 'utf8')
  ])
  assert.match(router, /\/rider\/login/)
  assert.match(router, /accountType: 'RIDER'/)
  for (const action of ['available-orders', 'claim', 'dispatch', 'complete']) assert.match(api, new RegExp(action))
  for (const state of ['加载', '暂无', '错误']) assert.match(page, new RegExp(state))
})
