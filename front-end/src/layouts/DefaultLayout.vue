<template>
  <div class="food-layout">
    <header class="food-header">
      <div class="header-inner">
        <router-link class="brand" to="/home" aria-label="饿了么校园版首页"><b>饿了么</b><span>校园版</span></router-link>
        <nav class="desktop-nav" aria-label="主导航">
          <router-link to="/home">外卖首页</router-link>
          <router-link to="/shops">全部店铺</router-link>
          <router-link v-if="!isMerchant" to="/orders">我的订单</router-link>
          <router-link v-if="isMerchant" to="/merchant">商家工作台</router-link>
        </nav>
        <div class="account-nav">
          <router-link v-if="!isMerchant" to="/cart" class="cart-link">购物车</router-link>
          <details v-if="hasToken" ref="accountMenu" class="account-menu">
            <summary>{{ isMerchant ? '商家账户' : '我的账户' }} <span aria-hidden="true">⌄</span></summary>
            <div class="account-dropdown">
              <template v-if="!isMerchant"><router-link to="/profile">个人信息</router-link><router-link to="/addresses">收货地址</router-link><router-link to="/coupons/mine">我的优惠券</router-link></template>
              <router-link v-else to="/merchant">商家工作台</router-link>
              <button @click="handleLogout">退出登录</button>
            </div>
          </details>
          <router-link v-else to="/login">登录 / 注册</router-link>
        </div>
      </div>
    </header>
    <main class="food-main">
      <router-view v-slot="{ Component }">
        <KeepAlive :key="sessionKey" include="CheckoutView"><component :is="Component" /></KeepAlive>
      </router-view>
    </main>
    <footer class="food-footer"><span>校园外卖课程演示</span><router-link v-if="!hasToken" to="/merchant/login">商家入驻</router-link></footer>
    <nav v-if="!isMerchant" class="mobile-nav" aria-label="手机导航">
      <router-link to="/home"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m3 10 9-7 9 7v10H3Z M9 20v-7h6v7"/></svg><span>首页</span></router-link>
      <router-link to="/shops"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 10v11h16V10 M3 10l2-7h14l2 7Z M9 21v-7h6v7"/></svg><span>店铺</span></router-link>
      <router-link to="/orders"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 3h12v18l-3-2-3 2-3-2-3 2Z M9 8h6 M9 12h6"/></svg><span>订单</span></router-link>
      <router-link to="/profile"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="7" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/></svg><span>我的</span></router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken, getAccountType, removeToken } from '@/utils/auth'
const route = useRoute()
const router = useRouter()
const accountMenu = ref(null)
const hasToken = computed(() => { route.fullPath; return Boolean(getToken()) })
const sessionKey = computed(() => { route.fullPath; return getToken() || 'guest' })
const isMerchant = computed(() => { route.fullPath; return hasToken.value && getAccountType() === 'MERCHANT' })
watch(() => route.fullPath, () => { if (accountMenu.value) accountMenu.value.open = false })
function handleLogout() {
  const loginPath = isMerchant.value ? '/merchant/login' : '/login'
  removeToken()
  ElMessage.success('已退出登录')
  router.push(loginPath)
}
</script>

<style scoped>
.food-layout { min-height: 100%; background: var(--page); }
.food-header { background: var(--brand); color: white; box-shadow: 0 1px 0 rgba(15, 23, 42, .08); }
.header-inner { max-width: 1200px; min-height: 72px; padding: 0 4px; margin: auto; display: flex; align-items: center; gap: 55px; }
.food-header a { color: inherit; text-decoration: none; }
.brand { display: flex; align-items: center; gap: 10px; white-space: nowrap; }
.brand b { font-size: 30px; font-style: italic; letter-spacing: -2px; }
.brand span { font-size: 11px; border: 1px solid #ffffff99; padding: 2px 5px; border-radius: 3px; }
.desktop-nav { align-self: stretch; display: flex; }
.desktop-nav a { display: flex; align-items: center; padding: 0 24px; font-size: 16px; }
.desktop-nav a.router-link-active { background: rgba(255, 255, 255, .14); font-weight: 700; }
.desktop-nav a:hover { background: rgba(255, 255, 255, .1); }
.account-nav { display: flex; align-items: center; gap: 23px; margin-left: auto; font-size: 14px; white-space: nowrap; }
.account-menu { position: relative; }
.account-menu summary { cursor: pointer; list-style: none; padding: 15px 0; }
.account-menu summary::-webkit-details-marker { display: none; }
.account-dropdown { position: absolute; top: 100%; right: 0; z-index: 20; min-width: 150px; border: 1px solid var(--line); border-radius: var(--radius-control); box-shadow: var(--shadow-hover); background: white; color: var(--ink); padding: 6px; }
.account-dropdown a, .account-dropdown button { display: block; padding: 12px 14px; text-align: left; width: 100%; font: inherit; border: 0; border-radius: 6px; background: white; color: var(--ink); cursor: pointer; }
.account-dropdown a:hover, .account-dropdown button:hover { background: var(--brand-soft); color: var(--brand); }
.food-main { max-width: 1200px; margin: auto; padding: 28px 0 0; min-height: calc(100vh - 150px); }
.food-footer { max-width: 1200px; padding: 24px 0; margin: auto; display: flex; gap: 20px; justify-content: center; color: var(--muted); font-size: 12px; }
.food-footer a { color: var(--muted); text-decoration: none; }
.mobile-nav { display: none; }
@media (max-width: 1260px) { .header-inner { padding: 0 24px; gap: 25px; } .food-main { padding: 24px 24px 0; } }
@media (max-width: 800px) { .desktop-nav a { padding: 0 12px; } .brand b { font-size: 26px; } .brand span { display: none; } .account-nav { gap: 14px; } }
@media (max-width: 600px) {
  .header-inner { min-height: 58px; padding: 0 16px; gap: 12px; }
  .brand b { font-size: 25px; }
  .brand span { display: inline; font-size: 10px; }
  .desktop-nav { display: none; }
  .account-nav { font-size: 12px; gap: 14px; }
  .food-main { padding: 16px 16px 0; min-height: calc(100vh - 170px); }
  .food-footer { padding: 25px 16px calc(90px + env(safe-area-inset-bottom)); }
  .mobile-nav { display: flex; position: fixed; bottom: 0; left: 0; right: 0; z-index: 30; background: #fff; border-top: 1px solid var(--line); padding-bottom: env(safe-area-inset-bottom); }
  .mobile-nav a { flex: 1; min-height: 58px; display: grid; justify-items: center; align-content: center; gap: 3px; padding: 6px 0; color: var(--muted); text-decoration: none; font-size: 12px; }
  .mobile-nav svg { width: 22px; height: 22px; stroke: currentColor; stroke-width: 1.7; fill: none; stroke-linejoin: round; stroke-linecap: round; }
  .mobile-nav a.router-link-active { color: var(--brand); font-weight: 700; }
}
</style>
