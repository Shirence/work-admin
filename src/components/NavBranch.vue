<script>
export default {
  name: 'NavBranch',
  props: { node: { type: Object, required: true } },
  computed: {
    icon() {
      return { '/user': 'User', '/roles': 'Key', '/menus': 'Menu', '/chat': 'ChatDotRound', '/messages': 'ChatLineRound' }[this.node.route] || 'Document'
    }
  }
}
</script>

<template>
  <el-sub-menu v-if="node.type === 'DIRECTORY'" :index="`dir-${node.id}`">
    <template #title><el-icon><Folder /></el-icon><span>{{ node.name }}</span></template>
    <NavBranch v-for="child in node.children" :key="child.id" :node="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="node.route"><el-icon><component :is="icon" /></el-icon><span>{{ node.name }}</span></el-menu-item>
</template>
