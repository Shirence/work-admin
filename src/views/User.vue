<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h2>用户管理</h2>
        <p class="u-hint">stu 项目的真实用户，数据在 MySQL 的 user 表；角色来自 sys_role</p>
      </div>
      <span class="u-label">{{ accounts.length }} users</span>
    </header>

    <section class="panel">
      <div class="toolbar">
        <div>
          <h3>真实用户</h3>
          <p class="u-hint">user · sys_user_role · 与 stu 共用账号</p>
        </div>
        <div class="toolbar-right">
          <el-input
            v-model="keyword"
            class="search"
            placeholder="搜索姓名或账号"
            clearable
            @keyup.enter="loadAccounts"
            @clear="loadAccounts"
          />
          <el-button v-if="can('user:create')" type="primary" plain @click="openCreate">新增用户</el-button>
          <el-button :loading="accountsLoading" @click="loadAccounts">刷新</el-button>
          <el-button :loading="refreshing" @click="refreshAccess">刷新权限</el-button>
        </div>
      </div>
      <el-table v-loading="accountsLoading" :data="accounts" class="table">
        <el-table-column prop="id" label="ID" width="80">
          <template #default="{ row }"><span class="mono dim">{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="150">
          <template #default="{ row }">{{ row.name || '—' }}</template>
        </el-table-column>
        <el-table-column prop="account" label="账号" min-width="160">
          <template #default="{ row }"><span class="mono">{{ row.account }}</span></template>
        </el-table-column>
        <el-table-column label="角色" width="200">
          <template #default="{ row }">
            <span class="chip chip-accent">{{ row.roleName || row.roleCode || '未分配' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="right">
          <template #default="{ row }">
            <el-button v-if="canEdit(row)" link type="primary" :disabled="saving" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('user:assign-role')" link type="primary" :disabled="roleSaving" @click="openRole(row)">绑定角色</el-button>
            <el-button v-if="canDelete(row)" link type="danger" :disabled="saving" @click="removeAccount(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有匹配的用户" :image-size="64" /></template>
      </el-table>
    </section>

    <el-dialog v-model="roleVisible" title="替换用户角色" width="420px" :close-on-click-modal="!roleSaving">
      <div class="dialog-form">
        <p class="u-hint">用户：{{ roleAccount?.name }} · {{ roleAccount?.account }}</p>
        <el-select v-model="roleId" :disabled="roleSaving" placeholder="选择角色" style="width: 100%">
          <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
        </el-select>
      </div>
      <template #footer>
        <el-button :disabled="roleSaving" @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" :disabled="!roleId" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑用户' : '新增用户'" width="460px" :close-on-click-modal="!saving">
      <el-form label-width="72px" @submit.prevent>
        <el-form-item label="姓名"><el-input v-model="form.name" maxlength="50" placeholder="不填则与账号相同" /></el-form-item>
        <el-form-item label="账号"><el-input v-model="form.account" maxlength="50" /></el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password maxlength="32" :placeholder="editId ? '留空则不修改' : '6-32 位'" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" :disabled="!can('user:assign-role')" placeholder="选择角色" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
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

const SUPER_ADMIN = 'super_admin'

export default {
  name: 'User',
  data() {
    return {
      accounts: [],
      keyword: '',
      accountsLoading: false,
      refreshing: false,
      roleOptions: [],
      roleVisible: false,
      roleAccount: null,
      roleId: null,
      roleSaving: false,
      dialogVisible: false,
      editId: null,
      form: { name: '', account: '', password: '', roleId: null },
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

    isSelf(row) {
      return row.account === this.$store.state.userInfo?.username
    },

    /** 只有 super_admin 能改 super_admin；自己不能改自己，避免把自己锁在门外 */
    canEdit(row) {
      if (!this.can('user:update')) return false
      if (this.isSelf(row)) return false
      if (row.roleCode === SUPER_ADMIN) return this.can('user:assign-role')
      return true
    },

    canDelete(row) {
      if (!this.can('user:delete')) return false
      if (this.isSelf(row)) return false
      if (row.roleCode === SUPER_ADMIN) return this.can('user:assign-role')
      return true
    },

    async loadAccounts() {
      this.accountsLoading = true
      try {
        const res = await getAccounts(this.keyword.trim())
        this.accounts = res.data || []
      } catch (e) {
        this.$message.error(e.message || '获取用户失败')
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
        name: '',
        account: '',
        password: '',
        roleId: this.roleOptions.find(r => r.code === 'member')?.id || this.roleOptions[0]?.id || null
      }
      this.dialogVisible = true
    },

    async openEdit(row) {
      try {
        await this.ensureRoles()
      } catch (e) {
        return this.$message.error(e.message || '获取角色失败')
      }
      this.editId = row.id
      this.form = {
        name: row.name || '',
        account: row.account || '',
        password: '',
        roleId: this.can('user:assign-role') ? (row.roleId || null) : null
      }
      this.dialogVisible = true
    },

    async saveAccount() {
      if (this.saving) return
      if (!this.form.account.trim()) {
        return this.$message.warning('请填写账号')
      }
      if (!this.editId && this.form.password.length < 6) {
        return this.$message.warning('新增用户需要 6-32 位密码')
      }
      this.saving = true
      try {
        const body = { ...this.form, account: this.form.account.trim(), name: this.form.name.trim() }
        if (this.editId) await updateAccount(this.editId, body)
        else await createAccount(body)
        this.dialogVisible = false
        this.$message.success(this.editId ? '用户已更新' : '用户已创建')
        await this.loadAccounts()
      } catch (e) {
        this.$message.error(e.message || '保存用户失败')
      } finally {
        this.saving = false
      }
    },

    async removeAccount(row) {
      if (this.saving) return
      try {
        await this.$confirm(`删除用户 ${row.name || row.account}？会直接改动 MySQL 的 user 表`, '提示', { type: 'warning' })
        this.saving = true
        await deleteAccount(row.id)
        this.$message.success('用户已删除')
        await this.loadAccounts()
      } catch (e) {
        if (e !== 'cancel') this.$message.error(e.message || '删除用户失败')
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
        if (this.roleAccount.account === this.$store.state.userInfo?.username) {
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
.search { width: 200px; }
.chip { display: inline-block; font-size: 10px; padding: 3px 8px; margin: 1px 4px 1px 0; border-radius: 999px; border: 1px solid var(--line-2); }
.chip-accent { color: var(--accent); background: var(--accent-soft); border-color: var(--accent-line); }
.dialog-form { display: flex; flex-direction: column; gap: 12px; }
</style>
