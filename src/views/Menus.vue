<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h2>菜单与按钮权限</h2>
        <p class="u-hint">目录 → 页面 → 按钮；权限标识同时用于路由守卫和后端接口</p>
      </div>
      <span class="u-label">{{ rows.length }} nodes</span>
    </header>

    <section class="panel">
      <div class="toolbar">
        <el-button :disabled="busy" @click="load">刷新</el-button>
        <el-button v-if="can('menu:create')" type="primary" plain :disabled="busy" @click="open()">新增节点</el-button>
      </div>

      <el-table v-loading="busy" :data="tree" row-key="id" default-expand-all class="table">
        <el-table-column prop="name" label="名称" min-width="170" />
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }"><span class="mono dim">{{ row.type }}</span></template>
        </el-table-column>
        <el-table-column prop="path" label="页面路径" min-width="130">
          <template #default="{ row }"><span class="mono dim">{{ row.path || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="permission" label="权限标识" min-width="150">
          <template #default="{ row }"><span class="mono dim">{{ row.permission || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="150" align="right">
          <template #default="{ row }">
            <el-button v-if="can('menu:update')" link type="primary" :disabled="busy" @click="open(row)">编辑</el-button>
            <el-button v-if="can('menu:delete')" link type="danger" :disabled="busy || row.builtin || row.children.length" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无菜单节点" :image-size="64" /></template>
      </el-table>
    </section>

    <el-dialog v-model="visible" title="菜单节点" width="520px" :close-on-click-modal="!busy">
      <el-form label-width="100px" @submit.prevent>
        <el-form-item label="名称"><el-input v-model="form.name" maxlength="100" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" :disabled="form.builtin" @change="typeChanged">
            <el-option label="目录" value="DIRECTORY" />
            <el-option label="页面" value="MENU" />
            <el-option label="按钮" value="BUTTON" />
          </el-select>
        </el-form-item>
        <el-form-item label="上级">
          <el-select v-model="form.parentId" clearable :disabled="form.builtin" placeholder="清空为根节点">
            <el-option v-for="m in parents" :key="m.id" :label="`${m.name}（${m.type}）`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.type === 'MENU'" label="页面路径">
          <el-input v-model="form.path" :disabled="form.builtin" maxlength="200" placeholder="/example" />
        </el-form-item>
        <el-form-item v-if="form.type !== 'DIRECTORY'" label="权限标识">
          <el-input v-model="form.permission" :disabled="form.builtin" maxlength="100" placeholder="example:read" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :disabled="form.builtin" />
        </el-form-item>
      </el-form>
      <p class="u-hint">内置节点只允许改名称和排序；新增业务页面后仍需在 router 中注册组件。</p>
      <template #footer>
        <el-button :disabled="busy" @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getMenus, createMenu, updateMenu, deleteMenu } from '../api/access'
import { toTree } from '../utils/menu'

export default {
  name: 'MenuManagement',
  data() {
    return {
      rows: [],
      busy: false,
      visible: false,
      form: { parentId: null, name: '', type: 'DIRECTORY', path: '', permission: '', sortOrder: 0, enabled: true, builtin: false }
    }
  },
  computed: {
    tree() {
      return toTree(this.rows)
    },
    parents() {
      const type = this.form.type === 'BUTTON' ? 'MENU' : this.form.type === 'MENU' ? 'DIRECTORY' : null
      if (!type) return []
      const forbidden = new Set(this.form.id ? [this.form.id] : [])
      let previous = -1
      while (previous !== forbidden.size) {
        previous = forbidden.size
        this.rows.forEach(m => { if (forbidden.has(m.parentId)) forbidden.add(m.id) })
      }
      return this.rows.filter(m => m.type === type && !forbidden.has(m.id))
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
        const res = await getMenus()
        this.rows = res.data || []
      } catch (e) {
        this.$message.error(e.message || '获取菜单失败')
      } finally {
        this.busy = false
      }
    },

    open(row) {
      this.form = row
        ? { ...row, children: undefined }
        : { parentId: null, name: '', type: 'DIRECTORY', path: '', permission: '', sortOrder: 0, enabled: true, builtin: false }
      this.visible = true
    },

    typeChanged() {
      this.form.parentId = null
      this.form.path = ''
      this.form.permission = ''
    },

    async save() {
      if (this.busy) return
      if (!this.form.name.trim()) return this.$message.warning('请输入节点名称')
      this.busy = true
      try {
        const body = {
          parentId: this.form.parentId || null,
          name: this.form.name,
          type: this.form.type,
          path: this.form.type === 'MENU' ? this.form.path : null,
          permission: this.form.type === 'DIRECTORY' ? null : this.form.permission,
          sortOrder: this.form.sortOrder,
          enabled: this.form.enabled
        }
        if (this.form.id) await updateMenu(this.form.id, body)
        else await createMenu(body)
        this.visible = false
        await this.load()
        await this.$store.dispatch('refreshAccess')
        this.$message.success('节点已保存')
      } catch (e) {
        this.$message.error(e.message || '保存失败')
      } finally {
        this.busy = false
      }
    },

    async remove(row) {
      if (this.busy) return
      try {
        await this.$confirm(`删除 ${row.name}？关联授权也会清理。`, '提示', { type: 'warning' })
        this.busy = true
        await deleteMenu(row.id)
        await this.load()
        this.$message.success('节点已删除')
      } catch (e) {
        if (e !== 'cancel') this.$message.error(e.message || '删除失败')
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
.toolbar { display: flex; align-items: center; justify-content: flex-end; gap: 10px; margin-bottom: 14px; }
</style>
