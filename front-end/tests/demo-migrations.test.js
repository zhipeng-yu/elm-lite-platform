import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('demo startup applies every versioned migration once', async () => {
  const script = await readFile(new URL('../../scripts/start-demo.ps1', import.meta.url), 'utf8')
  assert.match(script, /Get-ChildItem[^\n]+database\/migration\/V\*__\*\.sql/)
  assert.match(script, /migration-.*\.applied/)
})
