<script>
import { typeLabels, deletionReason, hasDisabledAncestor } from './menuModel'
export default {
  name: 'MenuTable',
  props: {
    tree: { type: Array, required: true }, rows: { type: Array, required: true },
    busy: Boolean, loading: Boolean, canCreate: Boolean, canUpdate: Boolean, canDelete: Boolean
  },
  emits: ['add-child', 'edit', 'remove'],
  data: () => ({ typeLabels }),
  methods: { deletionReason, hasDisabledAncestor }
}
</script>

<template>
  <el-table v-loading="loading" :data="tree" row-key="id" default-expand-all class="menu-table">
    <el-table-column label="节点名称" min-width="240">
      <template #default="{ row }"><span class="node-name">{{ row.name }}</span><el-tag v-if="row.builtin" size="small" type="info" class="builtin-tag">内置</el-tag></template>
    </el-table-column>
    <el-table-column label="类型" width="90">
      <template #default="{ row }"><el-tag :type="row.type === 'BUTTON' ? 'warning' : row.type === 'DIRECTORY' ? 'info' : 'primary'" size="small">{{ typeLabels[row.type] }}</el-tag></template>
    </el-table-column>
    <el-table-column label="页面路径" min-width="160" show-overflow-tooltip>
      <template #default="{ row }"><code v-if="row.path">{{ row.path }}</code><span v-else class="muted">—</span></template>
    </el-table-column>
    <el-table-column label="权限标识" min-width="170" show-overflow-tooltip>
      <template #default="{ row }"><code v-if="row.permission">{{ row.permission }}</code><span v-else class="muted">—</span></template>
    </el-table-column>
    <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
    <el-table-column label="状态" width="125">
      <template #default="{ row }">
        <el-tag v-if="!row.enabled" type="info" size="small">停用</el-tag>
        <el-tag v-else-if="hasDisabledAncestor(row, rows)" type="warning" size="small">上级已停用</el-tag>
        <el-tag v-else type="success" size="small">启用</el-tag>
      </template>
    </el-table-column>
    <el-table-column v-if="canCreate || canUpdate || canDelete" label="操作" width="245" fixed="right">
      <template #default="{ row }">
        <div class="node-actions">
          <el-button v-if="canCreate && row.type !== 'BUTTON'" link type="primary" :disabled="busy" @click="$emit('add-child', row)">新增子节点</el-button>
          <el-button v-if="canUpdate" link type="primary" :disabled="busy" @click="$emit('edit', row)">编辑</el-button>
          <el-tooltip v-if="canDelete" :content="deletionReason(row)" :disabled="!deletionReason(row)">
            <span><el-button link type="danger" :disabled="busy || !!deletionReason(row)" @click="$emit('remove', row)">删除</el-button></span>
          </el-tooltip>
        </div>
      </template>
    </el-table-column>
    <template #empty><el-empty description="暂无菜单节点" :image-size="70" /></template>
  </el-table>
</template>

<style scoped>
.menu-table { --el-table-header-bg-color: #f7f9fc; --el-table-header-text-color: #606b80; }
.menu-table :deep(.el-table__cell) { padding: 13px 0; }
.node-name { color: #25324b; font-weight: 500; }
.builtin-tag { margin-left: 7px; }
code, .muted { color: #8a94a6; font-size: 12px; }
.node-actions { display: flex; align-items: center; gap: 12px; }
.node-actions .el-button { margin-left: 0; }
</style>
