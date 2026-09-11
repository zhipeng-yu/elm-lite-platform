<template>
  <div class="workspace">
    <header class="page-heading"><div><p class="eyebrow">商家工作台</p><h1>把店铺打理好，把美味送出去</h1></div><button class="primary" :disabled="busy" @click="openShop">创建店铺</button></header>
    <p v-if="error" role="alert" class="error-text">{{ error }} <button class="secondary" :disabled="busy" @click="loadShops">重新加载</button></p>
    <p v-if="loading" role="status">正在加载店铺…</p>
    <section v-else-if="!shops.length" class="surface-card empty-state"><h2>从你的第一家店开始</h2><p>创建店铺后，添加分类和商品，再将店铺设为营业。</p></section>
    <template v-else>
      <section class="surface-card shop-toolbar">
        <label>当前店铺<select v-model="shopId" :disabled="busy || detailLoading" @change="loadDetails"><option v-for="s in shops" :key="s.id" :value="s.id">{{ s.shopName }}</option></select></label>
        <div v-if="shop"><p>{{ shop.address }}</p><p class="muted">起送 ¥{{ formatPriceCent(shop.startPriceCent) }} · 配送 ¥{{ formatPriceCent(shop.deliveryPriceCent) }}</p></div>
        <label v-if="shop">营业状态<select :value="shop.businessStatus" :disabled="busy || detailLoading" @change="changeStatus(Number($event.target.value))"><option :value="0">休息</option><option :value="1">营业</option><option :value="2">临时闭店</option></select></label>
      </section>
      <p v-if="detailLoading" role="status">正在加载分类与商品…</p>
      <template v-else>
        <section class="surface-card">
          <div class="section-heading"><h2>商品分类</h2><button class="secondary" :disabled="busy" @click="openCategory()">新增分类</button></div>
          <p v-if="!categories.length" class="muted">还没有分类，请先添加一个。</p>
          <div class="category-list"><div v-for="c in categories" :key="c.id" class="category-chip"><span>{{ c.categoryName }} · {{ c.status === 1 ? '启用' : '停用' }} · 排序 {{ c.sortOrder }}</span><button class="text-button" :disabled="busy" :aria-label="`编辑分类 ${c.categoryName}`" @click="openCategory(c)">编辑</button></div></div>
        </section>
        <section class="surface-card">
          <div class="section-heading"><h2>店内商品 <small>{{ products.length }} 件</small></h2><button class="primary" :disabled="busy || !categories.some(c => c.status === 1)" @click="openProduct()">新增商品</button></div>
          <p v-if="!products.length" class="muted">暂无商品。添加启用分类后即可上架第一件商品。</p>
          <div v-else class="table-scroll"><table><thead><tr><th>商品</th><th>分类</th><th>价格</th><th>库存</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="p in products" :key="p.id"><td><strong>{{ p.productName }}</strong><p class="muted">{{ p.description }}</p></td><td>{{ categories.find(c => c.id === p.categoryId)?.categoryName || '—' }}</td><td>¥{{ formatPriceCent(p.priceCent) }}</td><td>{{ p.stock }}</td><td><span class="status-pill">{{ p.status === 1 ? '上架' : '下架' }}</span></td><td><button class="text-button" :disabled="busy" :aria-label="`编辑商品 ${p.productName}`" @click="openProduct(p)">编辑</button><button class="text-button" :disabled="busy" @click="toggleProduct(p)">{{ p.status === 1 ? '下架' : '上架' }}</button></td></tr></tbody></table></div>
        </section>
      </template>
    </template>

    <el-dialog v-model="dialogOpen" :title="dialogTitle" width="min(560px, 94vw)" :close-on-click-modal="!busy" :close-on-press-escape="!busy" :show-close="!busy">
      <form class="form-grid" @submit.prevent="save">
        <fieldset :disabled="busy" class="form-grid">
          <template v-if="kind === 'shop'">
            <label>店铺名称<input v-model.trim="form.shopName" required maxlength="100"></label>
            <label>店铺地址<input v-model.trim="form.address" required maxlength="255"></label>
            <label>店铺简介<textarea v-model="form.description" maxlength="255"></textarea></label>
            <div class="form-columns"><label>起送金额（元）<input v-model.number="form.startYuan" type="number" required min="0" max="99999999.99" step="0.01"></label><label>配送费（元）<input v-model.number="form.deliveryYuan" type="number" required min="0" max="99999999.99" step="0.01"></label></div>
          </template>
          <template v-else-if="kind === 'category'">
            <label>分类名称<input v-model.trim="form.categoryName" required maxlength="50"></label>
            <label>显示顺序<input v-model.number="form.sortOrder" type="number" required min="0" max="2147483647" step="1"></label>
            <label v-if="editingId">分类状态<select v-model.number="form.status"><option :value="1">启用</option><option :value="0">停用</option></select></label>
          </template>
          <template v-else>
            <label>商品名称<input v-model.trim="form.productName" required maxlength="100"></label>
            <label>所属分类<select v-model.number="form.categoryId" required><option v-for="c in categories.filter(c => c.status === 1 || c.id === form.categoryId)" :key="c.id" :value="c.id" :disabled="c.status !== 1">{{ c.categoryName }}{{ c.status === 0 ? '（停用）' : '' }}</option></select></label>
            <label>商品描述<textarea v-model="form.description" maxlength="255"></textarea></label>
            <label>封面图片地址<input v-model.trim="form.imageUrl" type="url" maxlength="255" placeholder="https://…（可选）"></label>
            <p class="muted">详情图最多 3 张，支持 HTTP(S) 地址或本站 /images/ 路径。</p>
            <label>详情图 1<input v-model.trim="form.detailImageUrls[0]" type="text" maxlength="255" placeholder="https://… 或 /images/…（可选）"></label>
            <label>详情图 2<input v-model.trim="form.detailImageUrls[1]" type="text" maxlength="255" placeholder="https://… 或 /images/…（可选）"></label>
            <label>详情图 3<input v-model.trim="form.detailImageUrls[2]" type="text" maxlength="255" placeholder="https://… 或 /images/…（可选）"></label>
            <div class="form-columns"><label>售价（元）<input v-model.number="form.priceYuan" type="number" required min="0.01" max="99999999.99" step="0.01"></label><label>库存<input v-model.number="form.stock" type="number" required min="0" max="2147483647" step="1"></label></div>
            <p v-if="editingId" class="muted">只有修改库存数值时才会设置库存，请先确认最新库存。</p>
          </template>
        </fieldset>
        <p v-if="formError" role="alert" class="error-text">{{ formError }}</p>
        <div class="actions"><button class="primary" :disabled="busy">{{ busy ? '保存中…' : '保存' }}</button><button class="secondary" type="button" :disabled="busy" @click="dialogOpen = false">取消</button></div>
      </form>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from '@/api/merchant'
import { formatPriceCent } from '@/utils/format'
import { productPayload } from '@/utils/merchant-form'
const shops = ref([]), shopId = ref(null), categories = ref([]), products = ref([])
const loading = ref(false), detailLoading = ref(false), busy = ref(false), error = ref(''), formError = ref('')
const dialogOpen = ref(false), kind = ref('shop'), editingId = ref(null), form = ref({})
let original = {}, requestVersion = 0
const shop = computed(() => shops.value.find(s => s.id === shopId.value))
const dialogTitle = computed(() => `${editingId.value ? '编辑' : '新增'}${{ shop: '店铺', category: '分类', product: '商品' }[kind.value]}`)
async function loadShops() {
  loading.value = true; error.value = ''
  try {
    shops.value = await api.listMyShops()
    if (!shops.value.some(s => s.id === shopId.value)) shopId.value = shops.value[0]?.id ?? null
    if (shopId.value) await loadDetails()
  } catch (e) { error.value = e.response?.data?.msg || '加载店铺失败，请重试' }
  finally { loading.value = false }
}
async function loadDetails() {
  const version = ++requestVersion, id = shopId.value
  detailLoading.value = true; error.value = ''; categories.value = []; products.value = []
  try {
    const [c, p] = await Promise.all([api.listManagedCategories(id), api.listManagedProducts(id)])
    if (version === requestVersion) { categories.value = c; products.value = p }
  } catch (e) { if (version === requestVersion) error.value = e.response?.data?.msg || '加载商品失败，请重试' }
  finally { if (version === requestVersion) detailLoading.value = false }
}
function open(kindValue, row, values) {
  kind.value = kindValue; editingId.value = row?.id ?? null; original = row ? { ...row } : {}
  form.value = values; formError.value = ''; dialogOpen.value = true
}
function openShop() { open('shop', null, { shopName: '', address: '', description: '', startYuan: 0, deliveryYuan: 0 }) }
function openCategory(row) { open('category', row, { categoryName: row?.categoryName ?? '', sortOrder: row?.sortOrder ?? 0, status: row?.status ?? 1 }) }
function openProduct(row) { open('product', row, { productName: row?.productName ?? '', categoryId: row?.categoryId ?? categories.value.find(c => c.status === 1)?.id, description: row?.description ?? '', imageUrl: row?.imageUrl ?? '', detailImageUrls: [row?.detailImageUrls?.[0] ?? '', row?.detailImageUrls?.[1] ?? '', row?.detailImageUrls?.[2] ?? ''], priceYuan: (row?.priceCent ?? 100) / 100, stock: row?.stock ?? 0 }) }
async function save() {
  if (busy.value) return
  busy.value = true; formError.value = ''
  try {
    if (kind.value === 'shop') {
      const f = form.value
      const created = await api.createShop({ shopName: f.shopName, address: f.address, description: f.description, startPriceCent: Math.round(f.startYuan * 100), deliveryPriceCent: Math.round(f.deliveryYuan * 100) })
      shopId.value = created.id
    } else if (kind.value === 'category') {
      const { categoryName, sortOrder, status } = form.value
      if (editingId.value) await api.updateCategory(editingId.value, { categoryName, sortOrder, status })
      else await api.createCategory(shopId.value, { categoryName, sortOrder })
    } else {
      const payload = productPayload(form.value, editingId.value ? original : null)
      if (editingId.value) {
        if (Object.keys(payload).length) await api.updateProduct(editingId.value, payload)
      } else await api.createProduct(shopId.value, payload)
    }
    dialogOpen.value = false; ElMessage.success('保存成功'); await loadShops()
  } catch (e) { formError.value = e.response?.data?.msg || e.message || '保存失败' }
  finally { busy.value = false }
}
async function changeStatus(value) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await api.setBusinessStatus(shopId.value, value); await loadShops(); ElMessage.success('营业状态已更新') }
  catch (e) { error.value = e.response?.data?.msg || '修改失败' }
  finally { busy.value = false }
}
async function toggleProduct(product) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await api.updateProduct(product.id, { status: product.status === 1 ? 0 : 1 }); await loadDetails() }
  catch (e) { error.value = e.response?.data?.msg || '修改失败' }
  finally { busy.value = false }
}
onMounted(loadShops)
</script>
