import test from 'node:test'
import assert from 'node:assert/strict'
import { selectShops } from '../src/utils/shop-browse.js'

test('店铺搜索与费用筛选组合，不修改接口原始列表', () => {
  const shops = [
    { id: 1, shopName: '面馆', description: null, businessStatus: 0, startPriceCent: 1000, deliveryPriceCent: 0 },
    { id: 2, shopName: '校园饭堂', description: '米饭现炒', businessStatus: 1, startPriceCent: 1500, deliveryPriceCent: 200 },
    { id: 3, shopName: '盖饭店', description: '现炒盖饭', businessStatus: 1, startPriceCent: 1000, deliveryPriceCent: 0 }
  ]
  assert.deepEqual(selectShops(shops, { query: '  饭 ', openOnly: true, sort: 'delivery' }).map(s => s.id), [3, 2])
  assert.deepEqual(selectShops(shops, { freeDelivery: true }).map(s => s.id), [3, 1])
  assert.deepEqual(selectShops(shops, { query: '不存在' }), [])
  assert.deepEqual(selectShops(shops, { sort: 'minimum' }).map(s => s.id), [3, 2, 1])
  assert.deepEqual(shops.map(s => s.id), [1, 2, 3])
})
