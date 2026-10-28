<script>
export default {
  name: 'UserRoleDialog',
  props: {
    modelValue: Boolean,
    roleId: { type: [Number, String], default: null },
    user: { type: Object, default: null },
    options: { type: Array, required: true },
    saving: Boolean,
    isSelf: Boolean
  },
  emits: ['update:modelValue', 'update:roleId', 'save'],
  computed: {
    selectedRole() { return this.options.find(role => role.id === this.roleId && role.enabled !== false) },
    missingCurrentRole() { return this.user?.roleId != null && !this.options.some(role => role.id === this.user.roleId) }
  },
  methods: {
    close(value = false) { if (!this.saving) this.$emit('update:modelValue', value) },
    select(value) { if (!this.saving) this.$emit('update:roleId', value) },
    save() { if (!this.saving && this.selectedRole) this.$emit('save') }
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue" title="绑定用户角色" width="min(500px, 92vw)"
    :show-close="!saving" :close-on-click-modal="!saving" :close-on-press-escape="!saving"
    @update:model-value="close"
  >
    <el-descriptions :column="1" class="user-summary">
      <el-descriptions-item label="账号">{{ user?.account }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ user?.name || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="当前角色">{{ user?.roleName || '未分配角色' }}</el-descriptions-item>
    </el-descriptions>
    <el-alert v-if="isSelf" title="正在修改当前登录账号的角色。保存后将立即刷新权限，可能离开用户管理页面。" type="warning" :closable="false" show-icon class="role-notice" />
    <el-alert v-if="!options.length" title="暂无启用角色，请先在角色管理中启用可分配的角色。" type="info" :closable="false" show-icon class="role-notice" />
    <el-alert v-else-if="missingCurrentRole" title="当前角色已停用或不可分配，请重新选择一个启用角色。" type="info" :closable="false" class="role-notice" />
    <el-form label-position="top" @submit.prevent="save">
      <el-form-item label="分配角色" required>
        <el-select
          :model-value="roleId" :disabled="saving || !options.length" filterable clearable
          placeholder="请选择启用角色" aria-label="分配角色" @update:model-value="select"
        >
          <el-option v-for="role in options" :key="role.id" :label="role.name" :value="role.id">
            <span>{{ role.name }}</span><span class="option-code">{{ role.code }}</span>
          </el-option>
        </el-select>
        <p v-if="selectedRole?.description" class="role-description">{{ selectedRole.description }}</p>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="close()">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="!selectedRole" @click="save">保存角色</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.user-summary { margin-bottom: 16px; }
.role-notice { margin-bottom: 18px; }
.role-description { margin-top: 8px; color: #8a94a6; font-size: 12px; line-height: 1.6; }
.option-code { float: right; margin-left: 16px; color: #939eb0; font-size: 12px; }
</style>
