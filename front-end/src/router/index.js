import { createRouter, createWebHistory } from 'vue-router'

// 基础路由：登录/注册页独立，其余页面挂在 DefaultLayout 下。
// 后续页面任务在自己的模块内追加，共享修改需先与龙确认。
const routes = [
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
        component: () => import('@/views/address/AddressView.vue')
      },
      {
        path: 'cart',
        name: 'cart',
        component: () => import('@/views/cart/CartView.vue')
      },
      {
        path: 'checkout',
        name: 'checkout',
        component: () => import('@/views/order/CheckoutView.vue')
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/order/OrderListView.vue')
      },
      {
        path: 'orders/:id',
        name: 'order-detail',
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
