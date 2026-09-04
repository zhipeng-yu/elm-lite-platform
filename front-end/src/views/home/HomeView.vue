<template>
  <div class="home">
    <h2>首页</h2>
    <p class="desc">基础布局已就绪，注册、登录和店铺页面已完成（D3）。</p>
    <div class="links">
      <router-link to="/shops">店铺列表</router-link>
      <router-link to="/mock-demo">模拟数据状态验证页</router-link>
    </div>
  </div>
</template>

<script setup>
// 首页占位页面。
</script>

<style scoped>
.home {
  padding: 8px 0;
}

.desc {
  color: #909399;
  margin: 8px 0;
}

.links {
  display: flex;
  gap: 16px;
}
</style>
