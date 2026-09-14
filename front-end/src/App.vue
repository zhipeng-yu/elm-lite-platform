<template>
  <router-view v-slot="{ Component, route }">
    <Transition name="route" mode="out-in">
      <component
        :is="Component"
        :key="route.matched[0]?.path || route.path"
      />
    </Transition>
  </router-view>
</template>

<script setup>
// 顶层出口只在四种身份布局之间切换；普通页面由 DefaultLayout 内部过渡。
</script>
