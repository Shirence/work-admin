<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h2>用户管理</h2>
        <p class="u-hint">管理本地登录账号、角色绑定与启用状态</p>
      </div>
      <span class="u-label">{{ accounts.length }} accounts</span>
    </header>

    <section class="panel">
      <div class="toolbar">
        <div>
          <h3>登录账号</h3>
          <p class="u-hint">wa_sys_account · 角色来自 MySQL RBAC</p>
        </div>
        <div class="toolbar-right">
          <el-button v-if="can('user:create')" type="primary" plain @click="openCreate">新增账号</el-button>
          <el-button :loading="accountsLoading" @click="loadAccounts">刷新</el-button>
          <el-button :loading="refreshing" @click="refreshAccess">刷新权限</el-button>
        </div>
      </div>
      <el-table v-loading="accountsLoading" :data="accounts" class="table">
        <el-table-column prop="id" label="ID" width="70">
          <template #default="{ row }"><span class="mono dim">{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="username" label="账号" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column label="角色" width="160">
          <template #default="{ row }">
            <span class="chip chip-accent">{{ row.roleName || row.roleCode || '未分配' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">{{ row.enabled ? '启用' : '停用' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="right">
          <template #default="{ row }">
            <el-button v-if="canEdit(row)" link type="primary" :disabled="saving" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('user:assign-role')" link type="primary" :disabled="roleSaving" @click="openRole(row)">绑定角色</el-button>
            <el-button v-if="can('user:delete')" link type="danger" :disabled="saving" @click="removeAccount(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无账号" :image-size="64" /></template>
      </el-table>
    </section>

    <el-dialog v-model="roleVisible" title="替换账号角色" width="420px" :close-on-click-modal="!roleSaving">
      <div class="dialog-form">
        <p class="u-hint">账号：{{ roleAccount?.username }} · {{ roleAccount?.email }}</p>
        <el-select v-model="roleId" :disabled="roleSaving" placeholder="选择角色" style="width: 100%">
          <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
        </el-select>
      </div>
      <template #footer>
        <el-button :disabled="roleSaving" @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" :disabled="!roleId" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑账号' : '新增账号'" width="460px" :close-on-click-modal="!saving">
      <el-form label-width="80px" @submit.prevent>
        <el-form-item label="账号"><el-input v-model="form.username" maxlength="50" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" maxlength="100" /></el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password maxlength="32" :placeholder="editId ? '留空则不修改' : '6-32 位'" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" :disabled="!can('user:assign-role')" placeholder="选择角色" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAccount">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  getAccounts, getRoleOptions, bindUserRole,
  createAccount, updateAccount, deleteAccount
} from '../api/access'

export default {
  name: 'User',
  data() {
    return {
      accounts: [],
      accountsLoading: false,
      refreshing: false,
      roleOptions: [],
      roleVisible: false,
      roleAccount: null,
      roleId: null,
      roleSaving: false,
      dialogVisible: false,
      editId: null,
      form: { username: '', email: '', password: '', roleId: null, enabled: true },
      saving: false
    }
  },
  created() {
    if (this.can('user:read')) this.loadAccounts()
  },
  methods: {
    can(permission) {
      return this.$store.getters.can(permission)
    },

    canEdit(row) {
      if (!this.can('user:update')) return false
      if (this.can('user:assign-role')) return true
      return row.roleCode !== 'admin'
    },

    async loadAccounts() {
      this.accountsLoading = true
      try {
        const res = await getAccounts()
        this.accounts = res.data || []
      } catch (e) {
        this.$message.error(e.message || '获取账号失败')
      } finally {
        this.accountsLoading = false
      }
    },

    async refreshAccess() {
      this.refreshing = true
      try {
        await this.$store.dispatch('refreshAccess')
        this.$message.success('权限已刷新')
      } catch (e) {
        this.$message.error(e.message || '刷新权限失败')
      } finally {
        this.refreshing = false
      }
    },

    async ensureRoles() {
      if (!this.roleOptions.length) {
        const res = await getRoleOptions()
        this.roleOptions = res.data || []
      }
    },

    async openCreate() {
      try {
        await this.ensureRoles()
      } catch (e) {
        return this.$message.error(e.message || '获取角色失败')
      }
      this.editId = null
      this.form = {
        username: '',
        email: '',
        password: '',
        roleId: this.roleOptions.find(r => r.code === 'viewer')?.id || this.roleOptions[0]?.id || null,
        enabled: true
      }
      this.dialogVisible = true
    },

    async openEdit(row) {
      if (this.can('user:assign-role')) {
        try {
          await this.ensureRoles()
        } catch (e) {
          return this.$message.error(e.message || '获取角色失败')
        }
      }
      this.editId = row.id
      this.form = {
        username: row.username || '',
        email: row.email || '',
        password: '',
        roleId: this.can('user:assign-role') ? (row.roleId || null) : null,
        enabled: row.enabled !== false
      }
      this.dialogVisible = true
    },

    async saveAccount() {
      if (this.saving) return
      if (!this.form.username.trim() || !this.form.email.trim()) {
        return this.$message.warning('请填写账号和邮箱')
      }
      if (!this.editId && this.form.password.length < 6) {
        return this.$message.warning('新增账号需要 6-32 位密码')
      }
      this.saving = true
      try {
        const body = { ...this.form }
        if (this.editId) await updateAccount(this.editId, body)
        else await createAccount(body)
        this.dialogVisible = false
        this.$message.success(this.editId ? '账号已更新' : '账号已创建')
        await this.loadAccounts()
        if (this.editId && this.form.username === this.$store.state.userInfo?.username) {
          await this.$store.dispatch('refreshAccess')
        }
      } catch (e) {
        this.$message.error(e.message || '保存账号失败')
      } finally {
        this.saving = false
      }
    },

    async removeAccount(row) {
      if (this.saving) return
      try {
        await this.$confirm(`删除账号 ${row.username}？`, '提示', { type: 'warning' })
        this.saving = true
        await deleteAccount(row.id)
        this.$message.success('账号已删除')
        await this.loadAccounts()
      } catch (e) {
        if (e !== 'cancel') this.$message.error(e.message || '删除账号失败')
      } finally {
        this.saving = false
      }
    },

    async openRole(row) {
      if (this.roleSaving) return
      try {
        const res = await getRoleOptions()
        this.roleOptions = res.data || []
      } catch (e) {
        this.$message.error(e.message || '获取角色失败')
        return
      }
      this.roleAccount = row
      this.roleId = this.roleOptions.some(r => r.id === row.roleId) ? row.roleId : null
      this.roleVisible = true
    },

    async saveRole() {
      if (this.roleSaving || !this.roleId) return
      this.roleSaving = true
      try {
        await bindUserRole(this.roleAccount.id, this.roleId)
        this.roleVisible = false
        this.$message.success('角色已更新')
        if (this.roleAccount.username === this.$store.state.userInfo?.username) {
          await this.$store.dispatch('refreshAccess')
        }
        await this.loadAccounts()
      } catch (e) {
        this.$message.error(e.message || '角色更新失败')
      } finally {
        this.roleSaving = false
      }
    }
  }
}
</script>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.page-head h2 { font-size: 24px; font-weight: 700; letter-spacing: -.04em; margin-bottom: 4px; }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: var(--r-lg); box-shadow: var(--shadow-xs); padding: 18px; }
.toolbar { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
.toolbar-right { display: flex; align-items: center; gap: 10px; }
.toolbar h3 { font-size: 14px; margin-bottom: 3px; }
.chip { display: inline-block; font-size: 10px; padding: 3px 8px; margin: 1px 4px 1px 0; border-radius: 999px; border: 1px solid var(--line-2); }
.chip-accent { color: var(--accent); background: var(--accent-soft); border-color: var(--accent-line); }
.dialog-form { display: flex; flex-direction: column; gap: 12px; }
</style>
