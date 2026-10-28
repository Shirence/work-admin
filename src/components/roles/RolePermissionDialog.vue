<script>
import { permissionKeys } from './roleModel'

export default {
  name: 'RolePermissionDialog',
  props: {
    modelValue: Boolean,
    role: { type: Object, default: null },
    tree: { type: Array, required: true },
    checkedKeys: { type: Array, required: true },
    busy: Boolean
  },
  emits: ['update:modelValue', 'save'],
  data: () => ({ keyword: '', selectedCount: 0, initializing: false }),
  watch: { keyword(value) { this.$refs.treeRef?.filter(value.trim()) } },
  methods: {
    async initialize() {
      this.initializing = true
      this.keyword = ''
      try {
        await this.$nextTick()
        this.$refs.treeRef.setCheckedKeys(this.checkedKeys)
        this.updateCount()
      } finally { this.initializing = false }
    },
    filterNode(value, node) {
      return !value || `${node.name} ${node.permission || ''}`.toLowerCase().includes(value.toLowerCase())
    },
    updateCount() { this.selectedCount = this.$refs.treeRef.getCheckedKeys().length },
    selectAll(clear = false) {
      if (this.busy || this.initializing) return
      this.$refs.treeRef.setCheckedKeys(clear ? [] : permissionKeys(this.tree))
      this.updateCount()
    },
    submit() {
      if (!this.busy && !this.initializing) this.$emit('save', this.$refs.treeRef.getCheckedKeys())
    },
    close(value = false) { if (!this.busy && !this.initializing) this.$emit('update:modelValue', value) }
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue" :title="`分配权限 / ${role?.name || ''}`" width="min(680px, 94vw)" destroy-on-close
    :show-close="!busy" :close-on-click-modal="!busy" :close-on-press-escape="!busy"
    @update:model-value="close" @open="initialize"
  >
    <el-alert title="各节点独立勾选，保存后自动补齐所属页面和上级目录；空选将清空此角色授权。" type="info" :closable="false" show-icon />
    <div class="tree-toolbar">
      <el-input v-model="keyword" placeholder="筛选节点名称或权限标识" clearable :disabled="busy" aria-label="筛选权限节点" />
      <el-button :disabled="busy || initializing || !tree.length" @click="selectAll()">全选</el-button>
      <el-button :disabled="busy || initializing || !tree.length" @click="selectAll(true)">清空</el-button>
    </div>
    <div v-loading="initializing" class="tree-panel">
      <el-tree
        ref="treeRef" :data="tree" node-key="id" :props="{ label: 'name', children: 'children', disabled: () => busy || initializing }"
        :default-checked-keys="checkedKeys" :filter-node-method="filterNode"
        show-checkbox check-strictly default-expand-all :expand-on-click-node="false" @check="updateCount"
      >
        <template #default="{ data }">
          <span class="permission-node">
            <el-tag size="small" :type="data.type === 'BUTTON' ? 'warning' : 'info'">{{ { DIRECTORY: '目录', MENU: '页面', BUTTON: '按钮' }[data.type] || '页面' }}</el-tag>
            <span>{{ data.name }}</span><code v-if="data.permission">{{ data.permission }}</code>
            <span v-if="data.enabled === false" class="node-disabled">已停用</span>
          </span>
        </template>
        <template #empty><el-empty description="暂无可分配的权限节点" :image-size="55" /></template>
      </el-tree>
    </div>
    <template #footer>
      <span class="selection-count">已选 {{ selectedCount }} 个节点</span>
      <el-button :disabled="busy || initializing" @click="close()">取消</el-button>
      <el-button type="primary" :disabled="initializing" :loading="busy" @click="submit">保存权限</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.tree-toolbar { display: flex; gap: 8px; margin: 18px 0 12px; }
.tree-toolbar .el-button { margin-left: 0; }
.tree-panel { max-height: 45vh; overflow: auto; border: 1px solid #e8edf5; border-radius: 8px; padding: 12px; }
.permission-node { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; }
.permission-node code, .node-disabled { color: #939eb0; font-size: 12px; }
.selection-count { float: left; color: #7b879a; line-height: 32px; font-size: 13px; }
.tree-panel :deep(.el-tree-node__content) { height: 34px; }
</style>
