import test from 'node:test'
import assert from 'node:assert/strict'
import { productPayload } from '../src/utils/merchant-form.js'

test('编辑名称不发送旧库存或未更改的停用分类', () => {
  const old = { productName: '旧名称', categoryId: 1, priceCent: 1234, stock: 5, description: null, imageUrl: null }
  assert.deepEqual(productPayload({ productName: '新名称', categoryId: 1, priceYuan: 12.34, stock: 5, description: '', imageUrl: '' }, old), { productName: '新名称' })
})
test('新增商品转换整数分，显式改库存才提交库存', () => {
  const form = { productName: '餐品', categoryId: 1, priceYuan: 0.29, stock: 3, description: '', imageUrl: '' }
  assert.equal(productPayload(form).priceCent, 29)
  assert.deepEqual(productPayload(form, { ...productPayload(form), stock: 7 }), { stock: 3 })
})
