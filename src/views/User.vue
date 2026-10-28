
<script>
import { getUserList, getUser, addUser, editUser, deleteUser } from '../api/user'
import { getRoleOptions, bindUserRole } from '../api/access'
import UserRoleDialog from '../components/users/UserRoleDialog.vue'

export default {
  name: 'UserManagement',
  components: { UserRoleDialog },
  data() {
    return {
      items: [], page: 1, total: 0,
      keyword: '', appliedKeyword: '',
      loading: false, saving: false, deletingId: null, confirmingDelete: false,
      detailVisible: false, detailUser: null,
      dialogVisible: false,
      roleOptions: [], roleVisible: false, roleUser: null, roleId: null,
      roleLoading: false, roleSaving: false,
      form: { id: null, name: '', account: '', passwd: '' }
    }
  },
  computed: {
    // 搜索和分页由后端决定，避免前端二次筛选丢弃服务器结果。
    tableData() { return this.items },
    busy() {
      return this.loading || this.saving || this.deletingId !== null || this.confirmingDelete || this.roleLoading || this.roleSaving
    },
    dialogTitle() {
      return this.form.id === null ? '新增用户' : '编辑用户'
    },
    rules() {
      return {
        name: [{ max: 100, message: '姓名最多 100 个字符', trigger: 'blur' }],
        account: [
          { required: true, whitespace: true, message: '请输入账号', trigger: 'blur' },
          { max: 100, message: '账号最多 100 个字符', trigger: 'blur' }
        ],
        passwd: [
          { max: 100, message: '密码最多 100 个字符', trigger: 'blur' },
          { validator: (_, value, done) => {
            if (this.form.id === null && !value?.trim()) done(new Error('新增用户必须填写密码'))
            else done()
          }, trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.loadUsers()
  },
  methods: {
    can(permission) { return this.$store.getters.can(permission) },
    async openRole(row) {
      if (this.busy || !this.can('user:assign-role')) return
      this.roleLoading = true
      this.roleVisible = false
      try {
        const [options, user] = await Promise.all([getRoleOptions(), getUser(row.id)])
        this.roleOptions = options.filter(role => role.enabled !== false)
        this.roleUser = user
        this.roleId = this.roleOptions.some(role => role.id === user.roleId) ? user.roleId : null
        this.roleVisible = true
      } catch { /* 请求层提示错误，未加载完整时不打开弹窗。 */ }
      finally { this.roleLoading = false }
    },
    async saveRole() {
      if (this.busy || !this.can('user:assign-role') || !this.roleUser) return
      if (!this.roleOptions.some(role => role.id === this.roleId && role.enabled !== false)) {
        this.$message.warning('请选择有效的启用角色')
        return
      }
      this.roleSaving = true
      const userId = this.roleUser.id
      const roleId = this.roleId
      const token = this.$store.state.token
      try {
        await bindUserRole(userId, roleId)
        if (token !== this.$store.state.token) return
        this.roleVisible = false
        this.$message.success('角色已更新')
        if (userId === this.$store.state.userInfo?.id) {
          // 角色已在服务端替换，先撤下旧权限，再读取新权限。
          this.$store.commit('setAccess', { role: null, permissions: [], menus: [] })
          try {
            await this.$store.dispatch('refreshAccess')
          } catch {
            if (token !== this.$store.state.token) return
            this.$message.warning('角色已保存，权限刷新失败，请点击顶部“刷新权限”重试')
            await this.$router.replace('/forbidden')
            return
          }
          if (!this.can('user:read')) {
            await this.$router.replace('/forbidden')
            return
          }
        }
        await this.loadUsers()
      } catch { /* 绑定失败保留选择和弹窗，接口层显示具体原因。 */ }
      finally { this.roleSaving = false }
    },
  async loadUsers(page = this.page) {
  this.loading = true
  try {
    let result = await getUserList(page, this.appliedKeyword)
    const lastPage = Math.max(1, result.totalPages)
    if (page > lastPage) {
      result = await getUserList(lastPage, this.appliedKeyword)
    }
    this.items = result.items
    this.page = result.page
    this.total = result.total
    return true
  } catch {
    return false
  } finally {
    this.loading = false
  }
},
  handleSearch() {
  if (this.busy) return
  this.appliedKeyword = this.keyword.trim()
  this.loadUsers(1)
},
resetSearch() {
  this.keyword = ''
  this.appliedKeyword = ''
},
handleResetSearch() {
  if (this.busy) return
  this.resetSearch()
  this.loadUsers(1)
},
handlePageChange(page) {
  if (this.busy) return
  this.loadUsers(page)
},
    async openAddDialog() {
      if (this.busy || !this.can('user:create')) return
      this.form = { id: null, name: '', account: '', passwd: '' }
      this.dialogVisible = true
      await this.$nextTick()
      this.$refs.userFormRef.clearValidate()
    },
    async openDetail(row) {
      if (this.busy) return
      this.loading = true
      try {
        this.detailUser = await getUser(row.id)
        this.detailVisible = true
      } catch {
        // 查询失败时不展示旧详情。
      } finally {
        this.loading = false
      }
    },
    async openEditDialog(row) {
      if (this.busy || !this.can('user:update')) return
      this.loading = true
      try {
        const user = await getUser(row.id)
        this.form = { id: user.id, name: user.name || '', account: user.account, passwd: '' }
        this.dialogVisible = true
        await this.$nextTick()
        this.$refs.userFormRef.clearValidate()
      } catch {
        // 详情请求失败时不打开弹窗。
      } finally {
        this.loading = false
      }
    },
    async submitForm() {
      if (this.busy || !this.can(this.form.id === null ? 'user:create' : 'user:update')) return
      this.saving = true
      try {
        const valid = await this.$refs.userFormRef.validate().catch(() => false)
        if (!valid) return
        const editing = this.form.id !== null
        const data = { name: this.form.name, account: this.form.account.trim() }
        if (!editing || this.form.passwd.trim()) data.passwd = this.form.passwd
        const user = editing
          ? await editUser(this.form.id, data)
          : await addUser(data)
        this.dialogVisible = false
        const editingSelf = editing && user.id === this.$store.state.userInfo?.id
        if (editingSelf && data.passwd) {
          this.$store.commit('clearUserInfo')
          this.$message.success('密码已修改，请重新登录')
          await this.$router.replace('/login')
          return
        }
        if (editingSelf) this.$store.commit('setUserInfo', user)
        this.$message.success(editing ? '修改成功' : '新增成功')
        this.resetSearch()
        // 保存接口成功和列表刷新成功是两件事，刷新失败仍可手动重试。
        await this.loadUsers(editing ? this.page : 1)
      } catch {
        // 保存失败保留弹窗和输入内容；请求封装已提示原因。
      } finally {
        this.saving = false
      }
    },
    async handleDelete(row) {
      if (this.busy || !this.can('user:delete')) return
      if (row.id === this.$store.state.userInfo?.id) {
        this.$message.warning('不能删除当前登录账号')
        return
      }
      this.confirmingDelete = true
      try {
        await this.$confirm(`确定删除账号“${row.account}”吗？`, '删除确认', {
          confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
        })
      } catch {
        return
      } finally {
        this.confirmingDelete = false
      }
      this.deletingId = row.id
      try {
        await deleteUser(row.id)
        this.$message.success('删除成功')
        this.resetSearch()
        await this.loadUsers()
      } catch {
        // 请求失败和取消确认分开处理，避免把错误当作取消。
      } finally {
        this.deletingId = null
      }
    }
  }
}
</script>

<template>
  <el-card>
    <h1 class="page-title">用户管理</h1>
    <p class="page-description">管理团队成员、账号资料与角色权限，保持访问范围清晰有序。</p>
    <div class="toolbar">
      <el-input v-model="keyword" class="search-input" clearable placeholder="按姓名或账号搜索" @keyup.enter="handleSearch" />
      <el-button :disabled="busy" @click="handleSearch">搜索用户</el-button>
      <el-button :disabled="busy" @click="handleResetSearch">重置筛选</el-button>
      <el-button :disabled="busy" @click="loadUsers()">刷新列表</el-button>
      <el-button v-if="can('user:create')" type="primary" :disabled="busy" @click="openAddDialog">新增用户</el-button>
    </div>
    <p class="hint">每页 20 条；按姓名或账号搜索，结果与总数由服务器返回。</p>
    <div class="table-panel">
      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="id"
        border
        size="large"
        max-height="520"
        highlight-current-row
        show-overflow-tooltip
      >
        <el-table-column prop="id" label="ID" width="88" align="center">
          <template #default="{ row }"><span class="cell-id">#{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="150">
          <template #default="{ row }">
            <span v-if="row.name">{{ row.name }}</span>
            <span v-else class="cell-muted">未填写</span>
          </template>
        </el-table-column>
        <el-table-column prop="account" label="账号" min-width="220">
          <template #default="{ row }"><span class="cell-account">{{ row.account }}</span></template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" min-width="140">
          <template #default="{ row }"><el-tag v-if="row.roleName" effect="plain">{{ row.roleName }}</el-tag><span v-else class="cell-muted">未分配角色</span></template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link :disabled="busy" @click="openDetail(row)">详情</el-button>
            <el-button v-if="can('user:update')" link type="primary" :disabled="busy" @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="can('user:assign-role')" link type="primary" :disabled="busy" @click="openRole(row)">绑定角色</el-button>
            <el-button v-if="can('user:delete')" link type="danger" :disabled="busy" :loading="deletingId === row.id" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="table-empty">
            <span class="table-empty-title">没有找到匹配用户</span>
            <span class="table-empty-sub">换个关键词，或点「重置筛选」看全部</span>
          </div>
        </template>
      </el-table>
    </div>
    <el-pagination class="pagination" :current-page="page" :page-size="20" :total="total" :disabled="busy" layout="total, prev, pager, next" @update:current-page="handlePageChange" />
    <el-dialog v-model="detailVisible" title="用户详情" width="480px">
      <el-descriptions v-if="detailUser" :column="1" border>
        <el-descriptions-item label="ID">{{ detailUser.id }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detailUser.name || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="账号">{{ detailUser.account }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ detailUser.roleName || '未分配角色' }}</el-descriptions-item>
      </el-descriptions>
      <p class="hint">来自 GET /api/users/{id}。后端不返回密码。</p>
    </el-dialog>
    <UserRoleDialog
      v-model="roleVisible" v-model:role-id="roleId" :user="roleUser" :options="roleOptions"
      :saving="roleSaving" :is-self="!!roleUser && roleUser.id === $store.state.userInfo?.id"
      @save="saveRole"
    />
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
      <el-form ref="userFormRef" :model="form" :rules="rules" :disabled="saving" label-width="80px" @submit.prevent>
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" maxlength="100" /></el-form-item>
        <el-form-item label="账号" prop="account"><el-input v-model.trim="form.account" maxlength="100" /></el-form-item>
        <el-form-item label="密码" prop="passwd"><el-input v-model="form.passwd" type="password" show-password maxlength="100" :placeholder="form.id === null ? '新增时必填' : '留空保留原密码'" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.page-title{
    font-size: 50px; font-weight: bold; margin-bottom: 8px;
}
.page-description { margin: 0 0 16px; color: #909399; font-size: 13px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 10px; }
.search-input { width: 240px; }
.hint { color: #606266; font-size: 13px; margin: 8px 0 12px; }

.table-panel {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  overflow: hidden;
}
.table-panel :deep(.el-table) {
  --el-table-border-color: var(--el-border-color-lighter);
  --el-table-header-bg-color: #f5f7fa;
  --el-table-header-text-color: #303133;
  --el-table-row-hover-bg-color: #f0f7ff;
  font-size: 14px;
}
.table-panel :deep(.el-table th.el-table__cell) {
  font-weight: 600;
  letter-spacing: 0.3px;
}
.table-panel :deep(.el-table .el-table__cell) {
  padding: 11px 0;
}
.table-panel :deep(.el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: #fcfcfd;
}
.table-panel :deep(.el-table .el-table__cell:first-child .cell) {
  padding-left: 16px;
}

.cell-id {
  display: inline-block;
  min-width: 40px;
  padding: 1px 8px;
  border-radius: 10px;
  background: #f0f2f5;
  color: #606266;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.cell-account {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  letter-spacing: 0.2px;
}
.cell-muted { color: #c0c4cc; }

.table-empty { padding: 30px 0; }
.table-empty-title { display: block; color: #909399; font-size: 14px; }
.table-empty-sub { display: block; margin-top: 6px; color: #c0c4cc; font-size: 12px; }

.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
