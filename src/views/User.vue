<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h2>用户管理</h2>
        <p class="u-hint">维护系统用户信息，支持搜索、新增、编辑与删除</p>
      </div>
      <span class="u-label">{{ tableData.length }} records</span>
    </header>

    <section v-reveal class="panel">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="query.keyword"
            placeholder="搜索用户名"
            class="search"
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
          <el-button v-if="can('user:add')" type="primary" plain @click="openAddDialog">
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
            批量删除（{{ selectedRows.length }}）
          </el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        class="table"
        :header-cell-style="{ background: '#fafbfc', color: '#98a2ae', fontWeight: '600', fontSize: '11px', letterSpacing: '.08em', textTransform: 'uppercase' }"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column label="ID" prop="id" width="80">
          <template #default="{ row }">
            <span class="mono dim">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户名" prop="username" min-width="130">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="mini-avatar">{{ (row.username || 'U').charAt(0) }}</span>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" prop="phone" min-width="140">
          <template #default="{ row }">
            <span class="mono dim">{{ row.phone }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" prop="role" width="120">
          <template #default="{ row }">
            <span class="chip" :class="row.role === 'admin' ? 'chip-admin' : 'chip-user'">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="right">
          <template #default="{ row }">
            <el-button v-if="can('user:edit')" link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="can('user:delete')" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" :image-size="72" />
        </template>
      </el-table>
    </section>

    <UserFormDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :row-data="currentRow"
      :submitting="submitting"
      @submit="submitForm"
    />
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
      loading: false,
      submitting: false,
      deleting: false,
      selectedRows: [],
      dialogVisible: false,
      dialogTitle: "新增用户",
      currentRow: {}
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
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
.page { display: flex; flex-direction: column; gap: 20px; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; }
.page-head h2 { font-size: 22px; font-weight: 650; letter-spacing: -.02em; margin-bottom: 4px; }

.panel {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-xs);
  padding: 18px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 10px; }
.search { width: 250px; }
.btn-icon { margin-right: 4px; }

.table :deep(.el-table__inner-wrapper::before) { display: none; }
.table :deep(td.el-table__cell) { border-bottom: 1px solid var(--line); padding: 12px 0; }
.table :deep(.el-table__row) { transition: background var(--dur-1) var(--ease-2); }
.table :deep(.el-table__row:hover > td) { background: var(--accent-soft) !important; }

.user-cell { display: flex; align-items: center; gap: 9px; }
.mini-avatar {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--bg-inset);
  color: var(--text-2);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.mono { font-family: var(--mono); font-size: 12px; }
.dim { color: var(--text-3); }

.chip {
  display: inline-block;
  font-size: 11px;
  font-weight: 500;
  padding: 3px 9px;
  border-radius: 999px;
  border: 1px solid transparent;
}
.chip-admin { color: var(--accent); background: var(--accent-soft); border-color: var(--accent-line); }
.chip-user { color: var(--text-2); background: var(--bg-inset); border-color: var(--line-2); }
</style>
