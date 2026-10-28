<script>
import { getMenus, createMenu, updateMenu, deleteMenu } from '../../api/access'
import { toTree } from '../../utils/menu'
import { menuPayload, deletionReason } from './menuModel'
import MenuTable from './MenuTable.vue'
import MenuFormDialog from './MenuFormDialog.vue'

export default {
  name: 'MenuManagement',
  components: { MenuTable, MenuFormDialog },
  data: () => ({ rows: [], loading: false, saving: false, deleting: false, error: '', visible: false, editingNode: null, parentNode: null }),
  computed: {
    busy() { return this.loading || this.saving || this.deleting },
    tree() { return toTree(this.rows) },
    counts() { return Object.fromEntries(['DIRECTORY', 'MENU', 'BUTTON'].map(type => [type, this.rows.filter(node => node.type === type).length])) }
  },
  created() { this.load() },
  methods: {
    can(permission) { return this.$store.getters.can(permission) },
    async load() {
      if (this.loading || !this.can('menu:read')) return false
      this.loading = true
      this.error = ''
      try { this.rows = await getMenus(); return true }
      catch (error) { this.error = error.userMessage || '菜单加载失败，请重试'; return false }
      finally { this.loading = false }
    },
    open(node = null, parent = null) {
      if (this.busy || !this.can(node ? 'menu:update' : 'menu:create')) return
      if (parent?.type === 'BUTTON') return
      this.editingNode = node ? { ...node } : null
      this.parentNode = parent ? { ...parent } : null
      this.visible = true
    },
    async changed() {
      this.visible = false
      const token = this.$store.state.token
      try {
        await this.$store.dispatch('refreshAccess')
        if (!this.can('menu:read')) { await this.$router.replace('/forbidden'); return }
      } catch {
        if (token !== this.$store.state.token) return
        this.$store.commit('setAccess', { role: null, permissions: [], menus: [] })
        this.$message.warning('菜单已保存，权限刷新失败，请点击顶部“刷新权限”重试')
        await this.$router.replace('/forbidden')
        return
      }
      await this.load()
    },
    async save(form) {
      if (this.busy || !this.can(this.editingNode ? 'menu:update' : 'menu:create')) return
      let body
      try { body = menuPayload(form, this.editingNode, this.rows) }
      catch (error) { this.$message.warning(error.message); return }
      this.saving = true
      try {
        if (this.editingNode) await updateMenu(this.editingNode.id, body)
        else await createMenu(body)
        this.$message.success(this.editingNode ? '节点已更新' : '节点已创建')
        await this.changed()
      } catch { /* 请求层提示失败原因，保留表单供重试。 */ }
      finally { this.saving = false }
    },
    async remove(node) {
      if (this.busy || !this.can('menu:delete')) return
      const reason = deletionReason(node, this.rows)
      if (reason) { this.$message.warning(reason); return }
      this.deleting = true
      const token = this.$store.state.token
      try {
        await this.$confirm(`确定删除“${node.name}”吗？相关角色对此节点的授权也会被清理。`, '删除菜单节点', {
          type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
        })
        if (token !== this.$store.state.token || !this.can('menu:delete')) return
        await deleteMenu(node.id)
        this.$message.success('节点已删除')
        await this.changed()
      } catch { /* 取消不发送请求；接口冲突由请求层提示。 */ }
      finally { this.deleting = false }
    }
  }
}
</script>

<template>
  <section class="menus-page">
    <div class="page-heading">
      <div><h1>菜单管理</h1><p>组织导航目录、业务页面与按钮权限，调整应用的访问范围。</p></div>
      <el-button v-if="can('menu:create')" type="primary" :disabled="busy" @click="open()"><el-icon><Plus /></el-icon>新增节点</el-button>
    </div>
    <div class="menu-summary"><span>目录 <b>{{ counts.DIRECTORY }}</b></span><span>页面 <b>{{ counts.MENU }}</b></span><span>按钮 <b>{{ counts.BUTTON }}</b></span></div>
    <el-card shadow="never" class="menu-card">
      <div class="toolbar"><span>共 {{ rows.length }} 个节点 / 按排序值展示层级</span><el-button :disabled="busy" :loading="loading" @click="load"><el-icon><Refresh /></el-icon>刷新列表</el-button></div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="list-error" />
      <MenuTable :tree="tree" :rows="rows" :busy="busy" :loading="loading" :can-create="can('menu:create')" :can-update="can('menu:update')" :can-delete="can('menu:delete')" @edit="open" @add-child="parent => open(null, parent)" @remove="remove" />
      <p class="table-note">内置节点仅可改名称和排序；删除节点前请先移除其下级。停用上级后，整条分支的业务权限同步失效。</p>
    </el-card>
    <MenuFormDialog v-model="visible" :node="editingNode" :parent="parentNode" :rows="rows" :busy="saving" @save="save" />
  </section>
</template>

<style scoped>
.menus-page { max-width: 1700px; margin: 0 auto; }
.page-heading { display: flex; justify-content: space-between; align-items: center; gap: 20px; margin: 8px 0 24px; }
.page-heading h1 { color: #24314a; font-size: 26px; line-height: 1.5; }
.page-heading p { margin-top: 7px; color: #8590a4; font-size: 13px; line-height: 1.7; }
.page-heading .el-icon, .toolbar .el-icon { margin-right: 6px; }
.menu-summary { display: flex; gap: 30px; margin-bottom: 22px; color: #8590a4; font-size: 13px; }
.menu-summary b { margin-left: 9px; color: #34415a; font-size: 20px; }
.menu-card { border: 1px solid #e7ecf4; border-radius: 12px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 18px; color: #939eb0; font-size: 12px; }
.table-note { margin-top: 18px; color: #939eb0; font-size: 12px; line-height: 1.7; }
.list-error { margin-bottom: 16px; }
@media (max-width: 640px) { .page-heading { align-items: flex-start; } .page-heading > .el-button { flex-shrink: 0; } }
</style>
