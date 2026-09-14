import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('top-level and workspace routes share a keyed page transition', async () => {
  const [app, layout] = await Promise.all([
    readFile(new URL('../src/App.vue', import.meta.url), 'utf8'),
    readFile(new URL('../src/layouts/DefaultLayout.vue', import.meta.url), 'utf8')
  ])

  assert.match(app, /<Transition name="route" mode="out-in">/)
  assert.match(app, /:key="route\.matched\[0\]\?\.path \|\| route\.path"/)
  assert.match(layout, /<Transition name="route" mode="out-in">/)
  assert.match(layout, /:key="route\.path"/)
})

test('motion stays subtle and respects reduced-motion preferences', async () => {
  const [base, layout] = await Promise.all([
    readFile(new URL('../src/assets/base.css', import.meta.url), 'utf8'),
    readFile(new URL('../src/layouts/DefaultLayout.vue', import.meta.url), 'utf8')
  ])

  assert.match(base, /\.route-enter-active/)
  assert.match(base, /translateY\(8px\)/)
  assert.match(base, /prefers-reduced-motion:\s*reduce/)
  assert.match(base, /button:not\(:disabled\):active/)
  assert.match(layout, /\.mobile-nav a::after/)
  assert.match(layout, /details\[open\] \.account-dropdown/)
})
