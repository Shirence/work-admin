<script>
import { deletionReason } from './roleModel'

export default {
  name: 'RoleTable',
  props: {
    rows: { type: Array, required: true },
    loading: Boolean,
    busy: Boolean,
    canUpdate: Boolean,
    canGrant: Boolean,
    canDelete: Boolean,
    deletingId: { type: Number, default: null }
  },
  emits: ['edit', 'grant', 'remove'],
  methods: { deletionReason }
}
</script>

<template>
  <el-table v-loading="loading" :data="rows" row-key="id" stripe class="role-table">
    <el-table-column label="角色名称" min-width="180">
      <template #default="{ row }">
        <span class="role-name">{{ row.name }}</span>
        <el-tag v-if="row.builtin" size="small" type="info" class="builtin-tag">内置</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="角色编码" min-width="180">
      <template #default="{ row }"><code class="role-code">{{ row.code }}</code></template>
    </el-table-column>
    <el-table-column prop="description" label="说明" min-width="210" show-overflow-tooltip>
      <template #default="{ row }">{{ row.description || '—' }}</template>
    </el-table-column>
    <el-table-column prop="userCount" label="绑定用户数" width="115" align="center" />
    <el-table-column label="状态" width="100">
      <template #default="{ row }">
        <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">{{ row.enabled ? '启用' : '停用' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column v-if="canUpdate || canGrant || canDelete" label="操作" width="250" fixed="right">
      <template #default="{ row }">
        <div class="role-actions">
        <el-button v-if="canUpdate" link type="primary" :disabled="busy" @click="$emit('edit', row)">编辑</el-button>
        <el-tooltip v-if="canGrant" content="超级管理员始终拥有全部权限" :disabled="row.code !== 'super_admin'">
          <span><el-button link type="primary" :disabled="busy || row.code === 'super_admin'" @click="$emit('grant', row)">分配权限</el-button></span>
        </el-tooltip>
        <el-tooltip v-if="canDelete" :content="deletionReason(row)" :disabled="!deletionReason(row)">
          <span><el-button link type="danger" :loading="deletingId === row.id" :disabled="busy || !!deletionReason(row)" @click="$emit('remove', row)">删除</el-button></span>
        </el-tooltip>
        </div>
      </template>
    </el-table-column>
    <template #empty><el-empty description="暂无匹配角色，请调整筛选条件或新增角色" :image-size="70" /></template>
  </el-table>
</template>

<style scoped>
.role-table { --el-table-header-bg-color: #f7f9fc; --el-table-header-text-color: #606b80; }
.role-name { color: #25324b; font-weight: 600; }
.builtin-tag { margin-left: 8px; }
.role-code { padding: 3px 7px; border-radius: 5px; background: #f2f5f9; color: #65718a; font-size: 12px; }
.role-table :deep(.el-table__cell) { padding: 16px 0; }
.role-actions { display: flex; align-items: center; gap: 12px; }
.role-actions .el-button { margin-left: 0; }
</style>
