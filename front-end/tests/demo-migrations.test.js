import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { createServer } from 'node:net'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

test('duplicate demo startup identifies the occupied port and how to stop the old demo',
  { skip: process.platform !== 'win32' }, async () => {
    const server = createServer()
    await new Promise((resolve, reject) => {
      server.once('error', error => error.code === 'EADDRINUSE' ? resolve() : reject(error))
      server.listen(13317, '127.0.0.1', resolve)
    })
    try {
      const result = spawnSync('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
        fileURLToPath(new URL('../../scripts/start-demo.ps1', import.meta.url))], { encoding: 'utf8', timeout: 15000 })
      assert.equal(result.status, 1)
      assert.match(result.stdout + result.stderr, /Port 13317.*already in use/)
      assert.match(result.stdout + result.stderr, /Enter.*stop/)
    } finally {
      if (server.listening) await new Promise(resolve => server.close(resolve))
    }
  })

test('demo startup applies every versioned migration once', async () => {
  const script = await readFile(new URL('../../scripts/start-demo.ps1', import.meta.url), 'utf8')
  assert.match(script.split('\n')[0], /^[\x00-\x7F]*$/)
  assert.match(script, /param\(\[switch\]\$Verify\)/)
  assert.match(script, /Get-ChildItem[^\n]+database\/migration\/V\*__\*\.sql/)
  assert.match(script, /migration-.*\.applied/)
  assert.match(script, /\$demoDir = Join-Path \$repo '\.local-demo'/)
  assert.match(script, /\$jar = Join-Path \$demoDir 'backend-demo\.jar'/)
  assert.match(script, /Copy-Item -LiteralPath .*target\/elm-lite-platform.* -Destination \$jar/)
})
