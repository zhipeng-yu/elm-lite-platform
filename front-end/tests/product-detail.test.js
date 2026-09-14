import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'
import test from 'node:test'

const vueSource = await readFile(
  new URL('../src/views/product/ProductDetailView.vue', import.meta.url),
  'utf8'
)

const scriptSource = vueSource
  .match(/<script setup>([\s\S]*?)<\/script>/)[1]
  .replace(/^import .*$/gm, '')

function createPage(productResponse, options = {}) {
  const added = []
  const messages = []
  let fetches = 0

  const context = vm.createContext({
    ref: (value) => ({ value }),
    onMounted() {},
    useRoute: () => ({ params: { id: '10' } }),
    useRouter: () => ({
      push() {},
      back() {}
    }),
    ElMessage: {
      success: (message) => messages.push({ type: 'success', message }),
      error: (message) => messages.push({ type: 'error', message })
    },
    fetchProduct: async () => {
      fetches++
      return options.fetchProduct
        ? options.fetchProduct(fetches)
        : productResponse
    },
    addCartItem: async (productId, quantity) => {
      added.push({ productId, quantity })
      if (options.addError) throw options.addError
    }
  })

  vm.runInContext(
    scriptSource +
      '\nglobalThis.page = {' +
      ' product, quantity, adding, galleryImages, activeImage,' +
      ' load, selectImage, handleMainImageError, handleAddToCart' +
      ' }',
    context
  )

  return {
    page: context.page,
    added,
    messages,
    fetches: () => fetches
  }
}

test('加载商品后封面图优先作为大图，并保留有序详情图缩略图', async () => {
  const { page } = createPage({
    id: 10,
    stock: 5,
    imageUrl: '/images/cover.jpg',
    detailImageUrls: [
      '/images/detail-1.jpg',
      '/images/detail-2.jpg'
    ]
  })

  await page.load()

  assert.deepEqual(
    Array.from(page.galleryImages.value),
    [
      '/images/cover.jpg',
      '/images/detail-1.jpg',
      '/images/detail-2.jpg'
    ]
  )
  assert.equal(page.activeImage.value, '/images/cover.jpg')
})

test('没有封面图时使用第一张详情图，完全无图时保持空图片状态', async () => {
  const withDetail = createPage({
    id: 11,
    stock: 3,
    imageUrl: '',
    detailImageUrls: [
      '/images/detail-only.jpg'
    ]
  })

  await withDetail.page.load()

  assert.deepEqual(
    Array.from(withDetail.page.galleryImages.value),
    ['/images/detail-only.jpg']
  )
  assert.equal(
    withDetail.page.activeImage.value,
    '/images/detail-only.jpg'
  )

  const withoutImage = createPage({
    id: 12,
    stock: 3,
    imageUrl: null,
    detailImageUrls: []
  })

  await withoutImage.page.load()

  assert.deepEqual(
    Array.from(withoutImage.page.galleryImages.value),
    []
  )
  assert.equal(withoutImage.page.activeImage.value, '')
})

test('点击缩略图切换大图，大图加载失败后进入占位状态', async () => {
  const { page } = createPage({
    id: 13,
    stock: 2,
    imageUrl: '/images/cover.jpg',
    detailImageUrls: ['/images/detail.jpg']
  })

  await page.load()

  page.selectImage('/images/detail.jpg')
  assert.equal(page.activeImage.value, '/images/detail.jpg')

  page.handleMainImageError()
  assert.equal(page.activeImage.value, '')
})

test('售罄商品即使触发加购方法也不会请求购物车接口', async () => {
  const { page, added } = createPage({
    id: 14,
    stock: 0,
    imageUrl: '',
    detailImageUrls: []
  })

  await page.load()
  await page.handleAddToCart()

  assert.equal(added.length, 0)
  assert.equal(page.adding.value, false)
})

test('库存冲突后刷新商品可用库存，避免继续展示旧数量', async () => {
  const { page, fetches } = createPage(null, {
    fetchProduct: (count) => ({
      id: 15,
      stock: count === 1 ? 25 : 10,
      imageUrl: '',
      detailImageUrls: []
    }),
    addError: {
      response: {
        status: 409,
        data: { msg: '库存不足' }
      }
    }
  })

  await page.load()
  await page.handleAddToCart()

  assert.equal(fetches(), 2)
  assert.equal(page.product.value.stock, 10)
})

test('商品详情模板包含大图、缩略图和底部购买栏', () => {
  assert.match(vueSource, /class="product-gallery"/)
  assert.match(vueSource, /class="thumbnail-list"/)
  assert.match(vueSource, /class="purchase-bar"/)
  assert.match(vueSource, /@error="handleMainImageError"/)
})
