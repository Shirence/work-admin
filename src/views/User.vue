<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h2>用户管理</h2>
        <p class="u-hint">统一管理本地 H2 与远程 MySQL 登录账号、角色绑定</p>
      </div>
      <span class="u-label">{{ scope === 'local' ? accounts.length : remoteUsers.length }} accounts</span>
    </header>

    <div v-if="can('remote:user:read')" class="scope-tabs">
      <button type="button" class="scope-tab" :class="{ active: scope === 'local' }" @click="scope = 'local'">
        <span class="mono">LOCAL</span> 本地账号 <em>{{ accounts.length }}</em>
      </button>
      <button type="button" class="scope-tab" :class="{ active: scope === 'remote' }" @click="scope = 'remote'">
        <span class="mono">REMOTE</span> 远程账号 <em>{{ remoteUsers.length }}</em>
      </button>
    </div>

    <section v-if="scope === 'local'" class="panel">
      <div class="toolbar">
        <div>
          <h3>本地登录账号</h3>
          <p class="u-hint">sys_account · 角色来自本地 RBAC</p>
        </div>
        <div class="toolbar-right">
          <el-button v-if="can('user:create')" type="primary" plain @click="openLocalCreate">新增本地账号</el-button>
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
            <el-button v-if="canEditLocal(row)" link type="primary" :disabled="localSaving" @click="openLocalEdit(row)">编辑</el-button>
            <el-button v-if="can('user:assign-role')" link type="primary" :disabled="roleSaving" @click="openLocalRole(row)">绑定角色</el-button>
            <el-button v-if="can('user:delete')" link type="danger" :disabled="localSaving" @click="removeLocalAccount(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无本地账号" :image-size="64" /></template>
      </el-table>
    </section>

    <section v-else class="panel">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="remoteKeyword"
            class="search"
            placeholder="搜索远程昵称或账号"
            clearable
            @keyup.enter="loadRemoteUsers"
            @clear="loadRemoteUsers"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button :disabled="remoteLoading" @click="loadRemoteUsers">查询</el-button>
        </div>
        <div class="toolbar-right">
          <el-button :loading="remoteLoading" @click="loadRemoteUsers">刷新</el-button>
          <el-button v-if="can('remote:user:create')" type="primary" plain @click="openRemoteCreate">新增远程账号</el-button>
        </div>
      </div>

      <el-table v-loading="remoteLoading" :data="remoteUsers" class="table">
        <el-table-column prop="id" label="ID" width="70">
          <template #default="{ row }"><span class="mono dim">{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="昵称" min-width="140" />
        <el-table-column prop="account" label="账号" min-width="150">
          <template #default="{ row }"><span class="mono">{{ row.account }}</span></template>
        </el-table-column>
        <el-table-column label="远程角色" min-width="180">
          <template #default="{ row }">
            <span v-if="!row.roles.length" class="u-hint">未分配</span>
            <span v-for="role in row.roles" :key="role.id" class="chip chip-remote">{{ role.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" align="right">
          <template #default="{ row }">
            <el-button v-if="can('remote:user:update')" link type="primary" @click="openRemoteEdit(row)">编辑</el-button>
            <el-button v-if="can('remote:user:assign-role')" link type="primary" @click="openRemoteRole(row)">绑定角色</el-button>
            <el-button v-if="can('remote:user:delete')" link type="danger" @click="removeRemoteUser(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无远程账号" :image-size="64" /></template>
      </el-table>
    </section>

    <el-dialog v-model="localRoleVisible" title="替换本地账号角色" width="420px" :close-on-click-modal="!roleSaving">
      <div class="dialog-form">
        <p class="u-hint">账号：{{ localRoleAccount?.username }} · {{ localRoleAccount?.email }}</p>
        <el-select v-model="localRoleId" :disabled="roleSaving" placeholder="选择角色" style="width: 100%">
          <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
        </el-select>
      </div>
      <template #footer>
        <el-button :disabled="roleSaving" @click="localRoleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" :disabled="!localRoleId" @click="saveLocalRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="localDialogVisible" :title="localEditId ? '编辑本地账号' : '新增本地账号'" width="460px" :close-on-click-modal="!localSaving">
      <el-form label-width="80px" @submit.prevent>
        <el-form-item label="账号"><el-input v-model="localForm.username" maxlength="50" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="localForm.email" maxlength="100" /></el-form-item>
        <el-form-item label="密码">
          <el-input v-model="localForm.password" type="password" show-password maxlength="32" :placeholder="localEditId ? '留空则不修改' : '6-32 位'" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="localForm.roleId" :disabled="!can('user:assign-role')" placeholder="选择角色" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="localForm.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="localSaving" @click="localDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="localSaving" @click="saveLocalAccount">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="remoteDialogVisible" :title="remoteEditId ? '编辑远程账号' : '新增远程账号'" width="480px" :close-on-click-modal="!remoteSaving">
      <el-form label-width="90px" @submit.prevent>
        <el-form-item label="昵称"><el-input v-model="remoteForm.name" maxlength="100" /></el-form-item>
        <el-form-item label="账号"><el-input v-model="remoteForm.account" maxlength="100" /></el-form-item>
        <el-form-item label="密码">
          <el-input v-model="remoteForm.password" type="password" show-password maxlength="32" :placeholder="remoteEditId ? '留空则不修改' : '6-32 位'" />
        </el-form-item>
        <el-form-item label="远程角色">
          <el-select v-model="remoteForm.roleId" placeholder="选择远程角色" style="width: 100%">
            <el-option v-for="role in remoteRoles" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="remoteSaving" @click="remoteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="remoteSaving" @click="saveRemoteUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="remoteRoleVisible" title="替换远程角色" width="420px" :close-on-click-modal="!remoteSaving">
      <div class="dialog-form">
        <p class="u-hint">账号：{{ remoteRoleAccount?.account }} · {{ remoteRoleAccount?.name }}</p>
        <el-select v-model="remoteRoleId" :disabled="remoteSaving" placeholder="选择远程角色" style="width: 100%">
          <el-option v-for="role in remoteRoles" :key="role.id" :label="role.name" :value="role.id" />
        </el-select>
      </div>
      <template #footer>
        <el-button :disabled="remoteSaving" @click="remoteRoleVisible = false">取消</el-button>
        <el-button type="primary" :loading="remoteSaving" :disabled="!remoteRoleId" @click="saveRemoteRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  getAccounts, getRoleOptions, bindUserRole,
  createAccount, updateAccount, deleteAccount
} from '../api/access'
import {
  getRemoteUsers, getRemoteRoles, createRemoteUser, updateRemoteUser,
  bindRemoteRole, deleteRemoteUser
} from '../api/remoteAdmin'

export default {
  name: 'User',
  data() {
    return {
      scope: 'local',
      accounts: [],
      accountsLoading: false,
      refreshing: false,
      roleOptions: [],
      localRoleVisible: false,
      localRoleAccount: null,
      localRoleId: null,
      roleSaving: false,
      localDialogVisible: false,
      localEditId: null,
      localForm: { username: '', email: '', password: '', roleId: null, enabled: true },
      localSaving: false,
      remoteUsers: [],
      remoteRoles: [],
      remoteKeyword: '',
      remoteLoading: false,
      remoteDialogVisible: false,
      remoteEditId: null,
      remoteForm: { name: '', account: '', password: '', roleId: null },
      remoteSaving: false,
      remoteRoleVisible: false,
      remoteRoleAccount: null,
      remoteRoleId: null
    }
  },
  created() {
    if (this.can('user:read')) this.loadAccounts()
    if (this.can('remote:user:read')) {
      this.loadRemoteUsers()
      this.loadRemoteRoles()
    } else {
      this.scope = 'local'
    }
  },
  methods: {
    can(permission) {
      return this.$store.getters.can(permission)
    },

    canEditLocal(row) {
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
        this.$message.error(e.message || '获取本地账号失败')
      } finally {
        this.accountsLoading = false
      }
    },

    async loadRemoteUsers() {
      this.remoteLoading = true
      try {
        const res = await getRemoteUsers({ keyword: this.remoteKeyword })
        this.remoteUsers = res.data || []
      } catch (e) {
        this.$message.error(e.message || '获取远程账号失败')
      } finally {
        this.remoteLoading = false
      }
    },

    async loadRemoteRoles() {
      try {
        const res = await getRemoteRoles()
        this.remoteRoles = res.data || []
      } catch (e) {
        // 远程库未配置时只影响远程页；不额外弹错。
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

    async openLocalCreate() {
      try {
        await this.ensureRoles()
      } catch (e) {
        return this.$message.error(e.message || '获取角色失败')
      }
      this.localEditId = null
      this.localForm = {
        username: '',
        email: '',
        password: '',
        roleId: this.roleOptions.find(r => r.code === 'viewer')?.id || this.roleOptions[0]?.id || null,
        enabled: true
      }
      this.localDialogVisible = true
    },

    async openLocalEdit(row) {
      if (this.can('user:assign-role')) {
        try {
          await this.ensureRoles()
        } catch (e) {
          return this.$message.error(e.message || '获取角色失败')
        }
      }
      this.localEditId = row.id
      this.localForm = {
        username: row.username || '',
        email: row.email || '',
        password: '',
        roleId: this.can('user:assign-role') ? (row.roleId || null) : null,
        enabled: row.enabled !== false
      }
      this.localDialogVisible = true
    },

    async saveLocalAccount() {
      if (this.localSaving) return
      if (!this.localForm.username.trim() || !this.localForm.email.trim()) {
        return this.$message.warning('请填写账号和邮箱')
      }
      if (!this.localEditId && this.localForm.password.length < 6) {
        return this.$message.warning('新增本地账号需要 6-32 位密码')
      }
      this.localSaving = true
      try {
        const body = { ...this.localForm }
        if (this.localEditId) await updateAccount(this.localEditId, body)
        else await createAccount(body)
        this.localDialogVisible = false
        this.$message.success(this.localEditId ? '账号已更新' : '账号已创建')
        await this.loadAccounts()
        if (this.localEditId && this.localForm.username === this.$store.state.userInfo?.username) {
          await this.$store.dispatch('refreshAccess')
        }
      } catch (e) {
        this.$message.error(e.message || '保存本地账号失败')
      } finally {
        this.localSaving = false
      }
    },

    async removeLocalAccount(row) {
      if (this.localSaving) return
      try {
        await this.$confirm(`删除本地账号 ${row.username}？`, '提示', { type: 'warning' })
        this.localSaving = true
        await deleteAccount(row.id)
        this.$message.success('账号已删除')
        await this.loadAccounts()
      } catch (e) {
        if (e !== 'cancel') this.$message.error(e.message || '删除本地账号失败')
      } finally {
        this.localSaving = false
      }
    },

    async openLocalRole(row) {      if (this.roleSaving) return
      try {
        const res = await getRoleOptions()
        this.roleOptions = res.data || []
      } catch (e) {
        this.$message.error(e.message || '获取角色失败')
        return
      }
      this.localRoleAccount = row
      this.localRoleId = this.roleOptions.some(r => r.id === row.roleId) ? row.roleId : null
      this.localRoleVisible = true
    },

    async saveLocalRole() {
      if (this.roleSaving || !this.localRoleId) return
      this.roleSaving = true
      try {
        await bindUserRole(this.localRoleAccount.id, this.localRoleId)
        this.localRoleVisible = false
        this.$message.success('角色已更新')
        if (this.localRoleAccount.username === this.$store.state.userInfo?.username) {
          await this.$store.dispatch('refreshAccess')
        }
        await this.loadAccounts()
      } catch (e) {
        this.$message.error(e.message || '角色更新失败')
      } finally {
        this.roleSaving = false
      }
    },

    async ensureRemoteRoles() {
      if (!this.remoteRoles.length) await this.loadRemoteRoles()
    },

    async openRemoteCreate() {
      await this.ensureRemoteRoles()
      this.remoteEditId = null
      this.remoteForm = {
        name: '',
        account: '',
        password: '',
        roleId: this.remoteRoles.find(r => r.code === 'member')?.id || this.remoteRoles[0]?.id || null
      }
      this.remoteDialogVisible = true
    },

    async openRemoteEdit(row) {
      await this.ensureRemoteRoles()
      this.remoteEditId = row.id
      this.remoteForm = {
        name: row.name || '',
        account: row.account || '',
        password: '',
        roleId: row.roles[0]?.id || null
      }
      this.remoteDialogVisible = true
    },

    async saveRemoteUser() {
      if (this.remoteSaving) return
      if (!this.remoteForm.name.trim() || !this.remoteForm.account.trim()) {
        return this.$message.warning('请填写昵称和账号')
      }
      if (!this.remoteEditId && this.remoteForm.password.length < 6) {
        return this.$message.warning('新增远程账号需要 6-32 位密码')
      }
      this.remoteSaving = true
      try {
        const body = { ...this.remoteForm }
        if (this.remoteEditId) await updateRemoteUser(this.remoteEditId, body)
        else await createRemoteUser(body)
        this.remoteDialogVisible = false
        this.$message.success(this.remoteEditId ? '远程账号已更新' : '远程账号已创建')
        await this.loadRemoteUsers()
      } catch (e) {
        this.$message.error(e.message || '保存远程账号失败')
      } finally {
        this.remoteSaving = false
      }
    },

    async openRemoteRole(row) {
      await this.ensureRemoteRoles()
      this.remoteRoleAccount = row
      this.remoteRoleId = row.roles[0]?.id || null
      this.remoteRoleVisible = true
    },

    async saveRemoteRole() {
      if (this.remoteSaving || !this.remoteRoleId) return
      this.remoteSaving = true
      try {
        await bindRemoteRole(this.remoteRoleAccount.id, this.remoteRoleId)
        this.remoteRoleVisible = false
        this.$message.success('远程角色已更新')
        await this.loadRemoteUsers()
      } catch (e) {
        this.$message.error(e.message || '远程角色更新失败')
      } finally {
        this.remoteSaving = false
      }
    },

    async removeRemoteUser(row) {
      if (this.remoteSaving) return
      try {
        await this.$confirm(`删除远程账号 ${row.account}？`, '提示', { type: 'warning' })
        this.remoteSaving = true
        await deleteRemoteUser(row.id)
        this.$message.success('远程账号已删除')
        await this.loadRemoteUsers()
      } catch (e) {
        if (e !== 'cancel') this.$message.error(e.message || '删除远程账号失败')
      } finally {
        this.remoteSaving = false
      }
    }
  }
}
</script>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.page-head h2 { font-size: 24px; font-weight: 700; letter-spacing: -.04em; margin-bottom: 4px; }
.scope-tabs {
  display: flex;
  gap: 3px;
  padding: 3px;
  width: fit-content;
  margin-bottom: 14px;
  background: var(--bg-inset);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
}
.scope-tab {
  display: flex;
  align-items: center;
  gap: 7px;
  border: 0;
  background: transparent;
  color: var(--text-3);
  font-size: 12px;
  padding: 7px 12px;
  border-radius: 3px;
  cursor: pointer;
  transition: all var(--dur-1) var(--ease-2);
}
.scope-tab:hover { color: var(--text); }
.scope-tab.active {
  color: var(--accent);
  background: var(--surface);
  box-shadow: var(--shadow-xs);
  font-weight: 600;
}
.scope-tab em { font-family: var(--mono); font-style: normal; font-size: 9px; opacity: .65; }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: var(--r-lg); box-shadow: var(--shadow-xs); padding: 18px; }
.toolbar { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 10px; }
.toolbar h3 { font-size: 14px; margin-bottom: 3px; }
.search { width: 260px; }
.chip { display: inline-block; font-size: 10px; padding: 3px 8px; margin: 1px 4px 1px 0; border-radius: 999px; border: 1px solid var(--line-2); }
.chip-accent { color: var(--accent); background: var(--accent-soft); border-color: var(--accent-line); }
.chip-remote { color: var(--violet); background: color-mix(in srgb, var(--violet) 8%, transparent); border-color: color-mix(in srgb, var(--violet) 24%, transparent); }
.dialog-form { display: flex; flex-direction: column; gap: 12px; }
</style>
