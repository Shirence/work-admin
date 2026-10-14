<template>
  <div>
    <div class="page-header">
      <h2>用户管理</h2>
      <p>维护系统用户信息，支持搜索、新增、编辑与删除</p>
    </div>

    <el-card shadow="never" class="main-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="query.keyword"
            placeholder="搜索用户名"
            class="search-input"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleSearch">
            <el-icon class="btn-icon"><Search /></el-icon>查询
          </el-button>
        </div>

        <div class="toolbar-right">
          <el-button v-if="can('user:add')" type="success" @click="openAddDialog">
            <el-icon class="btn-icon"><Plus /></el-icon>新增用户
          </el-button>
          <el-button
            v-if="can('user:delete')"
            type="danger"
            plain
            :loading="deleting"
            :disabled="selectedRows.length === 0"
            @click="handleBatchDelete"
          >
            批量删除（已选 {{ selectedRows.length }} 项）
          </el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        stripe
        class="user-table"
        :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: '600' }"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="用户名" prop="username" min-width="120" />
        <el-table-column label="手机号" prop="phone" min-width="140" />
        <el-table-column label="角色" prop="role" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" effect="light" round>
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button v-if="can('user:edit')" link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="can('user:delete')" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <UserFormDialog
        v-model="dialogVisible"
        :title="dialogTitle"
        :row-data="currentRow"
        :submitting="submitting"
        @submit="submitForm"
      />
    </el-card>
  </div>
</template>

<script>
import { getUserList, addUser, editUser, delUser } from '../api/user'
import UserFormDialog from '../components/UserFormDialog.vue'

export default {
  name: "User",
  components: { UserFormDialog },
  data() {
    return {
      query: { keyword: "" },
      tableData: [],
      loading: false,      // 表格加载状态
      submitting: false,   // 弹窗提交按钮 loading
      deleting: false,     // 批量删除 loading
      selectedRows: [],    // 表格勾选的行
      dialogVisible: false,
      dialogTitle: "新增用户",
      currentRow: {}       // 传给弹窗的那一行数据
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    // 按钮权限判断
    can(permission) {
      return this.$store.state.userInfo?.permissions?.includes(permission) || false
    },

    async fetchList() {
      this.loading = true
      try {
        const res = await getUserList({ keyword: this.query.keyword })
        if (res.code === 200) {
          this.tableData = res.data
        }
      } catch (e) {
        this.$message.error("获取列表失败")
        console.error(e)
      } finally {
        this.loading = false
      }
    },

    handleSearch() {
      this.fetchList()
    },

    openAddDialog() {
      this.dialogTitle = "新增用户"
      this.currentRow = {}
      this.dialogVisible = true
    },

    openEditDialog(row) {
      this.dialogTitle = "编辑用户"
      this.currentRow = { ...row }
      this.dialogVisible = true
    },

    async submitForm(form) {
      this.submitting = true
      try {
        const res = form.id ? await editUser(form) : await addUser(form)
        if (res.code === 200) {
          this.$message.success(res.msg || "保存成功")
          this.dialogVisible = false
          this.fetchList()
        }
      } catch (e) {
        this.$message.error(e.message || "保存失败")
      } finally {
        this.submitting = false
      }
    },

    async handleDelete(row) {
      try {
        await this.$confirm(`确定删除用户「${row.username}」吗？`, '提示', {
          confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
        })
      } catch {
        return
      }
      try {
        await delUser(row.id)
        this.$message.success('删除成功')
        await this.fetchList()
        this.clearSelection()
      } catch (error) {
        this.$message.error(error.message || '删除失败')
      }
    },

    handleSelectionChange(rows) {
      this.selectedRows = rows
    },

    async handleBatchDelete() {
      const ids = this.selectedRows.map(row => row.id)
      if (ids.length === 0) return
      try {
        await this.$confirm(`确定删除选中的 ${ids.length} 个用户吗？`, '批量删除', {
          confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
        })
      } catch {
        return
      }

      this.deleting = true
      try {
        for (const id of ids) await delUser(id)
        this.$message.success(`成功删除 ${ids.length} 条数据`)
      } catch (error) {
        this.$message.error(`删除中断：${error.message || '请求失败'}`)
      } finally {
        await this.fetchList()
        this.clearSelection()
        this.deleting = false
      }
    },

    clearSelection() {
      this.$refs.tableRef && this.$refs.tableRef.clearSelection()
      this.selectedRows = []
    }
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2d3d;
  margin-bottom: 4px;
}
.page-header p { font-size: 13px; color: #909399; }

.main-card { border: none; border-radius: 10px; }

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}
.toolbar-left { display: flex; align-items: center; gap: 10px; }
.toolbar-right { display: flex; align-items: center; gap: 10px; }
.search-input { width: 240px; }
.btn-icon { margin-right: 4px; }

.user-table { border-radius: 8px; }
.user-table :deep(th.el-table__cell) { background: #fafafa !important; }
.user-table :deep(.el-table__row:hover > td) { background: #f5f9ff !important; }
</style>
