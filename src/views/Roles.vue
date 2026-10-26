<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h2>角色管理</h2>
      </div>
      <span class="u-label">{{ rows.length }} roles</span>
    </header>

    <section class="panel">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="筛选角色名或编码" clearable class="search" />
          <el-button :disabled="busy" @click="load">刷新</el-button>
        </div>
        <el-button v-if="can('role:create')" type="primary" plain :disabled="busy" @click="open()">新增角色</el-button>
      </div>

      <el-table v-loading="busy" :data="filtered" row-key="id" class="table">
        <el-table-column prop="name" label="角色名" min-width="140" />
        <el-table-column prop="code" label="编码" min-width="120">
          <template #default="{ row }"><span class="mono dim">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column prop="userCount" label="账号数" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="chip" :class="row.enabled ? 'chip-admin' : 'chip-user'">{{ row.enabled ? '启用' : '停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="right">
          <template #default="{ row }">
            <el-button v-if="can('role:update')" link type="primary" :disabled="busy" @click="open(row)">编辑</el-button>
            <el-button v-if="can('role:grant')" link type="primary" :disabled="busy || row.code === 'super_admin'" @click="openGrant(row)">分配权限</el-button>
            <el-button v-if="can('role:delete')" link type="danger" :disabled="busy || row.builtin || row.userCount > 0" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无角色" :image-size="64" /></template>
      </el-table>
    </section>

    <el-dialog v-model="visible" title="角色资料" width="440px" :close-on-click-modal="!busy">
      <el-form label-width="90px" @submit.prevent>
        <el-form-item label="角色名"><el-input v-model="form.name" maxlength="100" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" :disabled="form.builtin" maxlength="64" placeholder="以英文字母开头" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" maxlength="255" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" :disabled="form.builtin" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="busy" @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="grantVisible" :title="`分配权限：${grantRole?.name || ''}`" width="520px" :close-on-click-modal="!busy">
      <p class="u-hint tree-tip">勾选角色可访问的节点，保存时自动补齐上级目录。</p>
      <el-tree
        ref="treeRef"
        :data="tree"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        show-checkbox
        check-strictly
        default-expand-all
        class="perm-tree"
      >
        <template #default="{ data }">
          <span class="tree-node">
            <span>{{ data.name }}</span>
            <span class="mono tree-type">{{ data.type }}</span>
            <span v-if="data.permission" class="mono tree-perm">{{ data.permission }}</span>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button :disabled="busy" @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="saveGrant">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  getRoles, createRole, updateRole, deleteRole,
  getRolePermissions, saveRolePermissions, getPermissionTree
} from '../api/access'
import { toTree } from '../utils/menu'

export default {
  name: 'RoleManagement',
  data() {
    return {
      rows: [],
      keyword: '',
      busy: false,
      visible: false,
      form: { name: '', code: '', description: '', enabled: true },
      grantVisible: false,
      grantRole: null,
      tree: []
    }
  },
  computed: {
    filtered() {
      const k = this.keyword.trim().toLowerCase()
      return this.rows.filter(r => `${r.name} ${r.code}`.toLowerCase().includes(k))
    }
  },
  created() {
    this.load()
  },
  methods: {
    can(permission) {
      return this.$store.getters.can(permission)
    },

    async load() {
      this.busy = true
      try {
        const res = await getRoles()
        this.rows = res.data || []
      } catch (e) {
        this.$message.error(e.message || '获取角色失败')
      } finally {
        this.busy = false
      }
    },

    open(row) {
      this.form = row
        ? { ...row }
        : { name: '', code: '', description: '', enabled: true, builtin: false }
      this.visible = true
    },

    async save() {
      if (this.busy) return
      if (!this.form.name.trim() || !/^[a-zA-Z][a-zA-Z0-9_:-]{0,63}$/.test(this.form.code)) {
        return this.$message.warning('填写角色名；编码以英文字母开头')
      }
      this.busy = true
      try {
        const body = {
          name: this.form.name,
          code: this.form.code,
          description: this.form.description,
          enabled: this.form.enabled
        }
        if (this.form.id) await updateRole(this.form.id, body)
        else await createRole(body)
        this.visible = false
        await this.load()
        await this.$store.dispatch('refreshAccess')
        this.$message.success('角色已保存')
      } catch (e) {
        this.$message.error(e.message || '保存失败')
      } finally {
        this.busy = false
      }
    },

    async remove(row) {
      if (this.busy) return
      try {
        await this.$confirm(`删除角色 ${row.name}？`, '提示', { type: 'warning' })
        this.busy = true
        await deleteRole(row.id)
        await this.load()
        this.$message.success('角色已删除')
      } catch (e) {
        if (e !== 'cancel') this.$message.error(e.message || '删除失败')
      } finally {
        this.busy = false
      }
    },

    async openGrant(row) {
      if (this.busy) return
      this.busy = true
      try {
        const [nodesRes, idsRes] = await Promise.all([getPermissionTree(), getRolePermissions(row.id)])
        this.tree = toTree(nodesRes.data || [])
        this.grantRole = row
        this.grantVisible = true
        await this.$nextTick()
        this.$refs.treeRef.setCheckedKeys(idsRes.data || [])
      } catch (e) {
        this.$message.error(e.message || '获取权限失败')
      } finally {
        this.busy = false
      }
    },

    async saveGrant() {
      if (this.busy) return
      this.busy = true
      try {
        await saveRolePermissions(this.grantRole.id, this.$refs.treeRef.getCheckedKeys())
        this.grantVisible = false
        await this.$store.dispatch('refreshAccess')
        this.$message.success('权限已保存')
      } catch (e) {
        this.$message.error(e.message || '保存权限失败')
      } finally {
        this.busy = false
      }
    }
  }
}
</script>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.page-head h2 { font-size: 24px; font-weight: 700; letter-spacing: -.04em; margin-bottom: 4px; }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: var(--r-lg); box-shadow: var(--shadow-xs); padding: 18px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
.toolbar-left { display: flex; align-items: center; gap: 10px; }
.search { width: 260px; }
.chip { font-size: 10px; padding: 3px 8px; border-radius: 999px; border: 1px solid var(--line-2); }
.chip-admin { color: var(--accent); background: var(--accent-soft); border-color: var(--accent-line); }
.chip-user { color: var(--text-2); background: var(--bg-inset); }
.tree-tip { margin-bottom: 10px; }
.perm-tree { max-height: 430px; overflow: auto; border: 1px solid var(--line); border-radius: var(--r-md); padding: 8px; background: var(--bg-inset); }
.tree-node { display: flex; align-items: center; gap: 10px; }
.tree-type { font-size: 9px; color: var(--text-3); }
.tree-perm { font-size: 9px; color: var(--accent); opacity: .8; }
</style>
