<script setup>
import NavBranch from '../NavBranch.vue'
defineProps({ menus: { type: Array, required: true }, activePath: { type: String, required: true } })
const emit = defineEmits(['navigate'])
</script>

<template>
  <nav class="app-navigation" aria-label="应用导航">
    <p class="navigation-label">工作空间</p>
    <el-menu :default-active="activePath" router background-color="transparent" text-color="#aebed0" active-text-color="#fff" @select="emit('navigate')">
      <el-menu-item index="/dashboard"><el-icon><House /></el-icon><span>仪表盘</span></el-menu-item>
      <NavBranch v-for="node in menus" :key="node.id" :node="node" />
    </el-menu>
  </nav>
</template>

<style scoped>
.app-navigation { padding:12px 12px 24px; }
.navigation-label { padding:12px 14px; font-size: 12px; font-weight:600; letter-spacing:2px; color:#73879f; }
.app-navigation :deep(.el-menu) { border:0; }
.app-navigation :deep(.el-menu-item), .app-navigation :deep(.el-sub-menu__title) { height:46px; line-height:46px; margin:4px 0; border-radius:9px; font-size:13px; }
.app-navigation :deep(.el-menu-item:hover), .app-navigation :deep(.el-sub-menu__title:hover) { background:#ffffff0b; }
.app-navigation :deep(.el-menu-item.is-active) { background:linear-gradient(100deg,#3d71d8,#4d81e8); box-shadow:0 5px 13px #07162920; }
.app-navigation :deep(.el-menu-item .el-icon), .app-navigation :deep(.el-sub-menu__title .el-icon) { font-size:17px; margin-right:11px; }
.demo-label { margin-left:auto; padding:2px 5px; border-radius:4px; font-size: 12px; line-height:1.4; background:#ffffff12; color:#bfcce0; }
</style>
