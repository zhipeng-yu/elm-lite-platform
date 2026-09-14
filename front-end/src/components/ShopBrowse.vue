<template>
  <div class="shop-browse">
    <section class="search-area" aria-label="搜索店铺">
      <div class="search-heading">
        <div><h1>{{ home ? '美食外卖' : '全部店铺' }}</h1><p>米饭面食、汉堡小吃、奶茶饮品</p></div>
        <router-link to="/addresses" class="address-link">收货地址 <span aria-hidden="true">›</span></router-link>
      </div>
      <form class="search-box" role="search" @submit.prevent="search">
        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></svg>
        <input v-model="draft" type="search" aria-label="搜索店铺或口味" placeholder="搜索店铺、口味" maxlength="100">
        <button type="submit">搜索</button>
      </form>
    </section>
    <nav v-if="home" class="food-categories" aria-label="美食快捷搜索">
      <button v-for="category in categories" :key="category.keyword" type="button" :aria-pressed="query === category.keyword" @click="quickSearch(category.keyword)">
        <img :src="`/images/food/${category.image}.jpg`" alt="" width="76" height="76">
        <span>{{ category.label }}</span>
      </button>
    </nav>
    <section class="restaurant-section" aria-labelledby="restaurant-heading">
      <div class="restaurant-heading">
        <div><h2 id="restaurant-heading">{{ query ? `“${query}”的搜索结果` : '全部商家' }}</h2><span v-if="!loading && !errorMsg">{{ visibleShops.length }} 家店铺</span></div>
        <button class="refresh-button" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
      </div>
      <div class="shop-filters">
        <label class="sort-control">排序
          <select v-model="sort" aria-label="店铺排序"><option value="">默认排序</option><option value="delivery">配送费最低</option><option value="minimum">起送价最低</option></select>
        </label>
        <label class="filter-check"><input v-model="openOnly" type="checkbox">只看营业中</label>
        <label class="filter-check"><input v-model="freeDelivery" type="checkbox">免配送费</label>
        <button v-if="query || openOnly || freeDelivery || sort" class="reset-button" @click="reset">清除筛选</button>
      </div>
      <div v-if="loading" class="browse-state" role="status">正在加载店铺…</div>
      <div v-else-if="errorMsg" class="browse-state" role="alert"><p>{{ errorMsg }}</p><button @click="load">重新加载</button></div>
      <div v-else-if="!visibleShops.length" class="browse-state"><h3>{{ shops.length ? '没有找到符合条件的店铺' : '暂时还没有店铺' }}</h3><p>{{ shops.length ? '换个关键词，或清除筛选再看看。' : '请稍后再来看看。' }}</p><button v-if="shops.length" @click="reset">查看全部店铺</button></div>
      <ul v-else class="restaurant-grid">
        <li v-for="shop in visibleShops" :key="shop.id">
          <router-link :to="`/shops/${shop.id}`" class="restaurant-card" :class="{ resting: shop.businessStatus !== 1 }">
            <div class="restaurant-photo">
              <img v-if="shop.imageUrl && !failedImages.has(shop.imageUrl)" :src="shop.imageUrl" alt="" width="108" height="108" loading="lazy" @error="failedImages.add(shop.imageUrl)">
              <span v-else class="photo-placeholder" aria-hidden="true">{{ shop.shopName.slice(0, 2) }}</span>
              <span v-if="shop.businessStatus !== 1" class="resting-label">{{ BUSINESS_STATUS_TEXT[shop.businessStatus] || '状态未知' }}</span>
            </div>
            <div class="restaurant-info">
              <h3>{{ shop.shopName }}</h3>
              <p class="business-status" :class="{ closed: shop.businessStatus !== 1 }"><i aria-hidden="true"></i>{{ BUSINESS_STATUS_TEXT[shop.businessStatus] || '状态未知' }}</p>
              <p class="delivery-fees"><span>¥{{ formatPriceCent(shop.startPriceCent) }}起送</span><span>{{ shop.deliveryPriceCent === 0 ? '免配送费' : `配送 ¥${formatPriceCent(shop.deliveryPriceCent)}` }}</span></p>
              <p class="restaurant-description">{{ shop.description || '进店看看有什么好吃的' }}</p>
            </div>
          </router-link>
        </li>
      </ul>
      <p v-if="!loading && visibleShops.length" class="list-end">已经到底了 · 共 {{ visibleShops.length }} 家店铺</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchShops } from '@/api/shop'
import { BUSINESS_STATUS_TEXT, formatPriceCent } from '@/utils/format'
import { selectShops } from '@/utils/shop-browse'

defineProps({ home: Boolean })
const route = useRoute()
const router = useRouter()
const query = computed(() => typeof route.query.q === 'string' ? route.query.q : '')
const draft = ref(query.value)
const shops = ref([])
const loading = ref(false)
const errorMsg = ref('')
const openOnly = ref(false)
const freeDelivery = ref(false)
const sort = ref('')
const failedImages = ref(new Set())
const categories = [
  { label: '米饭快餐', keyword: '米饭', image: 'rice' },
  { label: '面馆粉丝', keyword: '面', image: 'noodles' },
  { label: '汉堡小吃', keyword: '汉堡', image: 'burger' },
  { label: '饺子馄饨', keyword: '饺', image: 'dumplings' },
  { label: '奶茶饮品', keyword: '奶茶', image: 'tea' },
  { label: '轻食沙拉', keyword: '轻食', image: 'salad' }
]
const visibleShops = computed(() => selectShops(shops.value, { query: query.value, openOnly: openOnly.value, freeDelivery: freeDelivery.value, sort: sort.value }))
watch(query, value => { draft.value = value })
function search() { router.replace({ path: route.path, query: draft.value.trim() ? { q: draft.value.trim() } : {} }) }
function quickSearch(keyword) { draft.value = query.value === keyword ? '' : keyword; search() }
function reset() { draft.value = ''; openOnly.value = false; freeDelivery.value = false; sort.value = ''; search() }
async function load() {
  loading.value = true
  errorMsg.value = ''
  try { shops.value = await fetchShops() }
  catch (error) { errorMsg.value = error.response?.data?.msg || '店铺加载失败，请检查网络后重试' }
  finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.shop-browse { color: var(--ink); }
.search-area { padding: 24px 30px; background: #fff; border: 1px solid var(--line); border-radius: var(--radius-card); }
.search-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.search-heading h1 { font-size: 26px; line-height: 1.4; }
.search-heading p { color: var(--muted); font-size: 14px; margin-top: 5px; }
.address-link { font-size: 14px; color: var(--brand); white-space: nowrap; text-decoration: none; }
.address-link span { margin-left: 8px; font-size: 22px; }
.search-box { display: flex; align-items: center; border: 2px solid var(--brand); border-radius: var(--radius-control); overflow: hidden; background: #fff; }
.search-box svg { width: 20px; height: 20px; margin: 0 13px 0 18px; fill: none; stroke: #999; stroke-width: 1.8; flex-shrink: 0; }
.search-box input { border: 0; outline: none; flex: 1; min-width: 0; height: 46px; font: inherit; font-size: 15px; background: transparent; }
.search-box:focus-within { outline: 3px solid #c9e8ff; outline-offset: 2px; }
.search-box button { align-self: stretch; min-width: 104px; background: var(--brand); border: 0; color: #fff; font-size: 16px; font-weight: 600; cursor: pointer; }
.food-categories { display: grid; grid-template-columns: repeat(6, 1fr); padding: 22px 30px; background: #fff; border-top: 1px solid #f5f5f5; }
.food-categories button { display: grid; justify-items: center; gap: 10px; background: none; border: 0; padding: 8px; font: inherit; font-size: 14px; cursor: pointer; }
.food-categories img { width: 76px; height: 76px; border-radius: 50%; object-fit: cover; transition: transform .15s; }
.food-categories button:hover img { transform: translateY(-3px); }
.food-categories button[aria-pressed="true"] { color: var(--brand); font-weight: bold; }
.food-categories button[aria-pressed="true"] img { outline: 3px solid var(--brand); outline-offset: 3px; }
.restaurant-section { margin-top: 28px; }
.restaurant-heading, .restaurant-heading > div { display: flex; align-items: baseline; gap: 12px; }
.restaurant-heading { justify-content: space-between; padding-bottom: 17px; }
.restaurant-heading h2 { font-size: 21px; overflow-wrap: anywhere; }
.restaurant-heading span { color: #999; font-size: 13px; white-space: nowrap; }
.refresh-button, .reset-button { background: none; border: 0; color: var(--brand); font: inherit; font-size: 14px; cursor: pointer; padding: 8px; white-space: nowrap; }
.shop-filters { display: flex; align-items: center; gap: 24px; flex-wrap: wrap; padding: 15px 20px; margin-bottom: 12px; background: #fff; font-size: 13px; }
.sort-control { color: #888; display: flex; align-items: center; gap: 10px; }
.sort-control select { color: #333; font: inherit; background: #fff; border: 0; padding: 5px; cursor: pointer; }
.filter-check { display: flex; align-items: center; gap: 7px; cursor: pointer; }
.filter-check input { width: 18px; height: 18px; accent-color: var(--brand); }
.restaurant-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; list-style: none; padding: 0; }
.restaurant-card { display: flex; gap: 15px; height: 100%; min-height: 164px; padding: 20px 16px; background: #fff; color: inherit; text-decoration: none; border: 1px solid var(--line); border-radius: var(--radius-card); transition: border-color .15s, box-shadow .15s; }
.restaurant-card:hover { border-color: #9bc8f2; box-shadow: var(--shadow-hover); }
.restaurant-photo { flex: 0 0 94px; position: relative; height: 94px; border-radius: 4px; overflow: hidden; }
.restaurant-photo img { width: 100%; height: 100%; object-fit: cover; }
.photo-placeholder { display: grid; place-items: center; height: 100%; background: var(--brand-soft); color: var(--brand); font-size: 25px; font-weight: 700; }
.resting .restaurant-photo img { filter: grayscale(.8); }
.resting-label { position: absolute; bottom: 0; left: 0; right: 0; background: #333a; text-align: center; color: white; padding: 3px; font-size: 12px; }
.restaurant-info { min-width: 0; flex: 1; }
.restaurant-info h3 { font-size: 16px; line-height: 1.5; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.business-status { display: flex; align-items: center; gap: 5px; color: var(--success); font-size: 13px; margin-top: 7px; }
.business-status i { display: inline-block; width: 5px; height: 5px; border-radius: 50%; background: currentColor; }
.business-status.closed { color: #888; }
.delivery-fees { display: flex; flex-wrap: wrap; gap: 0; color: #555; font-size: 12px; margin-top: 9px; }
.delivery-fees span + span { border-left: 1px solid #ddd; margin-left: 8px; padding-left: 8px; }
.restaurant-description { font-size: 13px; line-height: 1.6; color: var(--muted); border-top: 1px dashed var(--line); padding-top: 8px; margin-top: 10px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.browse-state { text-align: center; background: #fff; padding: 65px 20px; color: #888; font-size: 14px; }
.browse-state h3 { color: #555; font-size: 17px; margin-bottom: 10px; }
.browse-state button { margin-top: 20px; padding: 10px 20px; background: var(--brand); border: 0; color: #fff; border-radius: var(--radius-control); cursor: pointer; }
.list-end { text-align: center; color: #aaa; font-size: 12px; padding: 30px 0; }
@media (max-width: 1080px) { .restaurant-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) {
  .search-area { margin: -16px -16px 0; border: 0; border-radius: 0; padding: 18px 16px 22px; background: var(--brand); color: #fff; }
  .search-heading { margin-bottom: 16px; }
  .search-heading h1 { font-size: 23px; }
  .search-heading p { display: none; }
  .address-link { color: white; font-size: 13px; }
  .search-box { border-color: white; }
  .search-box input { height: 39px; font-size: 14px; }
  .search-box svg { margin-left: 12px; width: 18px; }
  .search-box button { margin: 4px; min-width: 62px; font-size: 14px; border-radius: 3px; }
  .food-categories { margin: 0 -16px; padding: 18px 10px; grid-template-columns: repeat(3, 1fr); row-gap: 8px; }
  .food-categories button { font-size: 13px; gap: 8px; }
  .food-categories img { height: 60px; width: 60px; }
  .restaurant-section { margin-top: 23px; }
  .restaurant-heading h2 { font-size: 19px; }
  .restaurant-heading span { font-size: 12px; }
  .shop-filters { gap: 12px; padding: 10px 16px; margin: 0 -16px; font-size: 12px; }
  .sort-control { gap: 3px; }
  .sort-control > select { max-width: 112px; }
  .restaurant-grid { grid-template-columns: 1fr; gap: 0; margin: 0 -16px; }
  .restaurant-card { border-width: 0 0 1px; border-radius: 0; padding: 18px 16px; min-height: 145px; gap: 13px; }
  .restaurant-photo { flex-basis: 86px; height: 86px; }
  .restaurant-info h3 { font-size: 17px; }
}
</style>
