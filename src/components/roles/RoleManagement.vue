<script>
import { getRoles, createRole, updateRole, deleteRole, getRolePermissions, saveRolePermissions, getPermissionTree } from '../../api/access'
import { toTree } from '../../utils/menu'
import { rolePayload, deletionReason } from './roleModel'
import RoleTable from './RoleTable.vue'
import RoleFormDialog from './RoleFormDialog.vue'
import RolePermissionDialog from './RolePermissionDialog.vue'

export default {
  name: 'RoleManagement',
  components: { RoleTable, RoleFormDialog, RolePermissionDialog },
  data: () => ({
    rows: [], keyword: '', loading: false, saving: false, error: '',
    deletingId: null, confirmingDeleteId: null, grantLoadingId: null,
    formVisible: false, editingRole: null, grantVisible: false, grantRole: null,
    tree: [], checkedKeys: []
  }),
  computed: {
    busy() { return this.loading || this.saving || this.deletingId !== null || this.confirmingDeleteId !== null || this.grantLoadingId !== null },
    filtered() {
      const keyword = this.keyword.trim().toLowerCase()
      return this.rows.filter(role => `${role.name} ${role.code}`.toLowerCase().includes(keyword))
    },
    enabledCount() { return this.rows.filter(role => role.enabled).length },
    builtinCount() { return this.rows.filter(role => role.builtin).length }
  },
  created() { this.load() },
  methods: {
    can(permission) { return this.$store.getters.can(permission) },
    async load() {
      if (this.loading || !this.can('role:read')) return false
      this.loading = true
      this.error = ''
      try {
        this.rows = await getRoles()
        return true
      } catch (error) {
        this.error = error.userMessage || '角色列表加载失败，请重试'
        return false
      } finally { this.loading = false }
    },
    open(role = null) {
      if (this.busy || !this.can(role ? 'role:update' : 'role:create')) return
      this.editingRole = role ? { ...role } : null
      this.formVisible = true
    },
    async refreshAccess() {
      try {
        await this.$store.dispatch('refreshAccess')
        if (!this.can('role:read')) await this.$router.replace('/forbidden')
      } catch {
        if (this.$store.state.token) this.$message.warning('操作已保存，但权限刷新失败，请点击顶部“刷新权限”重试')
      }
    },
    async save(form) {
      if (this.busy || !this.can(this.editingRole ? 'role:update' : 'role:create')) return
      let payload
      try { payload = rolePayload(form, this.editingRole) }
      catch (error) { this.$message.warning(error.message); return }
      this.saving = true
      try {
        if (this.editingRole) await updateRole(this.editingRole.id, payload)
        else await createRole(payload)
        this.formVisible = false
        this.$message.success(this.editingRole ? '角色已更新' : '角色已创建')
        await this.load()
        await this.refreshAccess()
      } catch { /* 请求层提示错误，保留表单内容以便重试。 */ }
      finally { this.saving = false }
    },
    async remove(role) {
      if (this.busy || !this.can('role:delete')) return
      const reason = deletionReason(role)
      if (reason) { this.$message.warning(reason); return }
      this.confirmingDeleteId = role.id
      const token = this.$store.state.token
      try {
        await this.$confirm(`确定删除角色“${role.name}”吗？`, '删除角色', {
          confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
        })
        if (token !== this.$store.state.token || !this.can('role:delete')) return
        this.deletingId = role.id
        await deleteRole(role.id)
        this.$message.success('角色已删除')
        await this.load()
      } catch { /* 取消不发送请求；接口冲突由请求层提示。 */ }
      finally { this.confirmingDeleteId = null; this.deletingId = null }
    },
    async openGrant(role) {
      if (this.busy || !this.can('role:grant') || role.code === 'super_admin') return
      this.grantLoadingId = role.id
      try {
        const [nodes, keys] = await Promise.all([getPermissionTree(), getRolePermissions(role.id)])
        this.tree = toTree(nodes)
        this.checkedKeys = keys
        this.grantRole = { ...role }
        this.grantVisible = true
      } catch { /* 两个请求都成功后才显示弹窗，避免误存旧权限。 */ }
      finally { this.grantLoadingId = null }
    },
    async saveGrant(keys) {
      if (this.busy || !this.can('role:grant') || !this.grantRole || this.grantRole.code === 'super_admin') return
      this.saving = true
      try {
        await saveRolePermissions(this.grantRole.id, keys)
        this.grantVisible = false
        this.$message.success('授权已保存')
        await this.refreshAccess()
      } catch { /* 保存失败时保留当前勾选。 */ }
      finally { this.saving = false }
    }
  }
}
</script>

<template>
  <section class="roles-page">
    <div class="page-heading">
      <div><h1>角色管理</h1><p>管理角色资料与访问权限，为不同职责分配合适的工作范围。</p></div>
      <el-button v-if="can('role:create')" type="primary" :disabled="busy" @click="open()"><el-icon><Plus /></el-icon>新增角色</el-button>
    </div>
    <div class="role-summary">
      <span>全部角色 <b>{{ rows.length }}</b></span><span>已启用 <b>{{ enabledCount }}</b></span><span>内置角色 <b>{{ builtinCount }}</b></span>
    </div>
    <el-card shadow="never" class="role-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="筛选角色名称或编码" clearable aria-label="筛选角色名称或编码" class="search-input"><template #prefix><el-icon><Search /></el-icon></template></el-input>
        <span class="result-count">显示 {{ filtered.length }} / {{ rows.length }} 个角色</span>
        <el-button :disabled="busy" :loading="loading" @click="load"><el-icon><Refresh /></el-icon>刷新列表</el-button>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="list-error" />
      <RoleTable :rows="filtered" :loading="loading" :busy="busy" :deleting-id="deletingId" :can-update="can('role:update')" :can-grant="can('role:grant')" :can-delete="can('role:delete')" @edit="open" @grant="openGrant" @remove="remove" />
      <p class="table-note">内置角色不可删除或停用；已绑定用户的角色需先解除绑定，才能删除。</p>
    </el-card>
    <RoleFormDialog v-model="formVisible" :role="editingRole" :busy="saving" @save="save" />
    <RolePermissionDialog v-model="grantVisible" :role="grantRole" :tree="tree" :checked-keys="checkedKeys" :busy="saving" @save="saveGrant" />
  </section>
</template>

<style scoped>
.roles-page { max-width: 1600px; margin: 0 auto; }
.page-heading { display: flex; justify-content: space-between; align-items: center; gap: 20px; margin: 8px 0 24px; }
.page-heading h1 { margin: 0; color: #24314a; font-size: 26px; line-height: 1.5; }
.page-heading p { margin-top: 7px; color: #8590a4; font-size: 13px; line-height: 1.7; }
.page-heading .el-icon, .toolbar .el-button .el-icon { margin-right: 6px; }
.role-summary { display: flex; gap: 28px; margin-bottom: 22px; color: #8590a4; font-size: 13px; }
.role-summary b { margin-left: 9px; color: #34415a; font-size: 20px; }
.role-card { border: 1px solid #e7ecf4; border-radius: 12px; }
.toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 14px; margin-bottom: 20px; }
.search-input { width: 300px; max-width: 100%; }
.result-count { flex: 1; color: #939eb0; font-size: 12px; }
.table-note { margin-top: 18px; color: #939eb0; font-size: 12px; line-height: 1.7; }
.list-error { margin-bottom: 16px; }
@media (max-width: 640px) {
  .page-heading { align-items: flex-start; }
  .page-heading > .el-button { flex-shrink: 0; }
  .role-summary { gap: 18px; }
  .search-input { width: 100%; }
}
</style>
