import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getAccountType } from '@/utils/auth'

// 基础路由：登录/注册页独立，其余页面挂在 DefaultLayout 下。
// 后续页面任务在自己的模块内追加，共享修改需先与龙确认。
const routes = [
  { path: '/merchant/login', component: () => import('@/views/auth/MerchantAuthView.vue') },
  { path: '/rider/login', component: () => import('@/views/rider/RiderAuthView.vue') },
  { path: '/rider', component: () => import('@/views/rider/RiderTaskView.vue'), meta: { accountType: 'RIDER' } },
  { path: '/admin/login', name: 'admin-login', component: () => import('@/views/admin/AdminLoginView.vue') },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/admin/AdminConsoleView.vue'),
    meta: { accountType: 'ADMIN' }
  },
  { path: '/', redirect: '/home' },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue')
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      { path: 'profile', component: () => import('@/views/auth/ProfileView.vue'), meta: { accountType: 'USER' } },
      { path: 'merchant', component: () => import('@/views/shop/MerchantDashboardView.vue'), meta: { accountType: 'MERCHANT' } },
      { path: 'merchant/orders', component: () => import('@/views/order/MerchantOrderListView.vue'), meta: { accountType: 'MERCHANT' } },
      { path: 'merchant/orders/:id', component: () => import('@/views/order/MerchantOrderDetailView.vue'), meta: { accountType: 'MERCHANT' } },
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/home/HomeView.vue')
      },
      {
        path: 'shops',
        name: 'shops',
        component: () => import('@/views/shop/ShopListView.vue')
      },
      {
        path: 'shops/:id',
        name: 'shop-detail',
        component: () => import('@/views/shop/ShopDetailView.vue')
      },
      {
        path: 'shops/:id/categories',
        name: 'shop-categories',
        component: () => import('@/views/category/CategoryView.vue')
      },
      {
        path: 'shops/:id/products',
        name: 'shop-products',
        component: () => import('@/views/product/ProductListView.vue')
      },
      {
        path: 'products/:id',
        name: 'product-detail',
        component: () => import('@/views/product/ProductDetailView.vue')
      },
      {
        path: 'addresses',
        name: 'addresses',
        meta: { accountType: 'USER' },
        component: () => import('@/views/address/AddressView.vue')
      },
      {
        path: 'cart',
        name: 'cart',
        meta: { accountType: 'USER' },
        component: () => import('@/views/cart/CartView.vue')
      },
      {
        path: 'checkout',
        name: 'checkout',
        meta: { accountType: 'USER' },
        component: () => import('@/views/order/CheckoutView.vue')
      },
      {
        path: 'coupons/mine',
        name: 'my-coupons',
        meta: { accountType: 'USER' },
        component: () => import('@/views/coupon/MyCouponsView.vue')
      },
      {
        path: 'orders',
        name: 'orders',
        meta: { accountType: 'USER' },
        component: () => import('@/views/order/OrderListView.vue')
      },
      {
        path: 'orders/:id',
        name: 'order-detail',
        meta: { accountType: 'USER' },
        component: () => import('@/views/order/OrderDetailView.vue')
      },
      {
        path: 'mock-demo',
        name: 'mock-demo',
        component: () => import('@/views/demo/MockDemoView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

router.beforeEach((to) => {
  const type = to.meta.accountType
  if (!type) return true
  if (!getToken()) {
    const loginPath = type === 'MERCHANT' ? '/merchant/login' : type === 'ADMIN' ? '/admin/login' : type === 'RIDER' ? '/rider/login' : '/login'
    return { path: loginPath, query: { redirect: to.fullPath } }
  }
  if (getAccountType() !== type) {
    const current = getAccountType()
    return current === 'MERCHANT' ? '/merchant' : current === 'ADMIN' ? '/admin' : current === 'RIDER' ? '/rider' : '/home'
  }
  return true
})
