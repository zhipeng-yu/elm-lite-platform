import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { productPayload } from '../src/utils/merchant-form.js'

test('本站封面路径不会被浏览器绝对网址校验挡住保存', async () => {
  const source = await readFile(new URL('../src/views/shop/MerchantDashboardView.vue', import.meta.url), 'utf8')
  const input = source.match(/<input[^>]*v-model\.trim="form\.imageUrl"[^>]*>/)?.[0]
  assert.ok(input)
  assert.doesNotMatch(input, /type="url"/)
  assert.equal(productPayload({ imageUrl: '/images/food/rice.jpg' }).imageUrl, '/images/food/rice.jpg')
})

test('编辑名称不发送旧库存、未更改分类或未更改详情图', () => {
  const old = {
    productName: '旧名称',
    categoryId: 1,
    priceCent: 1234,
    stock: 5,
    description: null,
    imageUrl: null,
    detailImageUrls: [
      '/images/products/detail-1.jpg',
      'https://example.com/detail-2.jpg'
    ]
  }

  const form = {
    productName: '新名称',
    categoryId: 1,
    priceYuan: 12.34,
    stock: 5,
    description: '',
    imageUrl: '',
    detailImageUrls: [
      '/images/products/detail-1.jpg',
      'https://example.com/detail-2.jpg'
    ]
  }

  assert.deepEqual(
    productPayload(form, old),
    { productName: '新名称' }
  )
})

test('新增商品转换整数分并提交详情图', () => {
  const form = {
    productName: '餐品',
    categoryId: 1,
    priceYuan: 0.29,
    stock: 3,
    description: '',
    imageUrl: '',
    detailImageUrls: [
      ' /images/products/detail-1.jpg ',
      '',
      'https://example.com/detail-2.jpg'
    ]
  }

  assert.deepEqual(productPayload(form), {
    productName: '餐品',
    categoryId: 1,
    description: '',
    imageUrl: '',
    detailImageUrls: [
      '/images/products/detail-1.jpg',
      'https://example.com/detail-2.jpg'
    ],
    priceCent: 29,
    stock: 3
  })
})

test('编辑时显式改库存才提交库存', () => {
  const form = {
    productName: '餐品',
    categoryId: 1,
    priceYuan: 12.34,
    stock: 3,
    description: '',
    imageUrl: '',
    detailImageUrls: []
  }

  const old = {
    ...productPayload(form),
    stock: 7
  }

  assert.deepEqual(
    productPayload(form, old),
    { stock: 3 }
  )
})

test('详情图清空时 PATCH 显式发送空数组', () => {
  const old = {
    productName: '餐品',
    categoryId: 1,
    priceCent: 1200,
    stock: 5,
    description: '',
    imageUrl: '',
    detailImageUrls: [
      '/images/products/detail-1.jpg'
    ]
  }

  const form = {
    productName: '餐品',
    categoryId: 1,
    priceYuan: 12,
    stock: 5,
    description: '',
    imageUrl: '',
    detailImageUrls: []
  }

  assert.deepEqual(
    productPayload(form, old),
    { detailImageUrls: [] }
  )
})

test('详情图顺序变化时 PATCH 发送新的有序数组', () => {
  const old = {
    productName: '餐品',
    categoryId: 1,
    priceCent: 1200,
    stock: 5,
    description: '',
    imageUrl: '',
    detailImageUrls: [
      '/images/products/detail-1.jpg',
      '/images/products/detail-2.jpg'
    ]
  }

  const form = {
    productName: '餐品',
    categoryId: 1,
    priceYuan: 12,
    stock: 5,
    description: '',
    imageUrl: '',
    detailImageUrls: [
      '/images/products/detail-2.jpg',
      '/images/products/detail-1.jpg'
    ]
  }

  assert.deepEqual(
    productPayload(form, old),
    {
      detailImageUrls: [
        '/images/products/detail-2.jpg',
        '/images/products/detail-1.jpg'
      ]
    }
  )
})
