<script setup>
import { dateTime } from './workModel'
defineProps({ config: { type: Object, required: true }, items: { type: Array, required: true }, busy: Boolean, canEdit: { type: Function, required: true }, canRemove: { type: Function, required: true } })
const emit = defineEmits(['detail', 'edit', 'remove'])
function display(column, row) {
  if (column.options) return column.options.find(option => option.value === row[column.key])?.label || row[column.key]
  if (column.boolean) return row[column.key] ? '是' : '否'
  if (column.date) return dateTime(row[column.key])
  return row[column.key] || (column.key === 'assigneeName' ? row.assigneeAccount : '') || '—'
}
</script>
<template>
  <el-table :data="items" v-loading="busy" stripe row-key="id" empty-text="暂无记录，点击新增开始使用" style="width:100%">
    <el-table-column v-for="column in config.columns" :key="column.key" :label="column.label" :min-width="column.min || 100" show-overflow-tooltip><template #default="{ row }"><el-tag v-if="['status','priority'].includes(column.key)" :type="['done','submitted'].includes(row[column.key]) ? 'success' : row.priority === 'high' ? 'danger' : 'info'">{{ display(column, row) }}</el-tag><span v-else>{{ display(column, row) }}</span></template></el-table-column>
    <el-table-column label="操作" min-width="190"><template #default="{ row }"><el-button link type="primary" :disabled="busy" @click="emit('detail', row)">详情</el-button><el-button v-if="canEdit(row)" link type="primary" :disabled="busy" @click="emit('edit', row)">编辑</el-button><el-button v-if="canRemove(row)" link type="danger" :disabled="busy" @click="emit('remove', row)">删除</el-button></template></el-table-column>
  </el-table>
</template>
