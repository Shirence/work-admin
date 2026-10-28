<script>
import { parentCandidates, typeLabels } from './menuModel'
export default {
  name: 'MenuFormDialog',
  props: {
    modelValue: Boolean, node: { type: Object, default: null }, parent: { type: Object, default: null },
    rows: { type: Array, required: true }, busy: Boolean
  },
  emits: ['update:modelValue', 'save'],
  data: () => ({ form: {}, validating: false, typeLabels }),
  computed: {
    parents() { return parentCandidates(this.form, this.rows, this.node?.id) },
    rules() {
      return {
        name: [{ required: true, whitespace: true, message: '请输入节点名称', trigger: 'blur' }, { max: 100, message: '名称最多 100 个字符', trigger: 'blur' }],
        path: [{ required: this.form.type === 'MENU', whitespace: true, message: '请输入页面路径', trigger: 'blur' }],
        permission: [{ required: this.form.type !== 'DIRECTORY', whitespace: true, message: '请输入权限标识', trigger: 'blur' }],
        parentId: [{ required: this.form.type === 'BUTTON', message: '请选择按钮所属页面', trigger: 'change' }]
      }
    }
  },
  methods: {
    async initialize() {
      this.form = this.node ? { ...this.node } : {
        name: '', type: this.parent ? (this.parent.type === 'MENU' ? 'BUTTON' : 'MENU') : 'DIRECTORY',
        parentId: this.parent?.id ?? null, path: null, permission: null, sortOrder: 0, enabled: true
      }
      await this.$nextTick()
      this.$refs.formRef?.clearValidate()
    },
    async typeChanged() {
      this.form.parentId = null
      this.form.path = null
      this.form.permission = null
      await this.$nextTick()
      this.$refs.formRef?.clearValidate()
    },
    close(value = false) { if (!this.busy && !this.validating) this.$emit('update:modelValue', value) },
    async submit() {
      if (this.busy || this.validating) return
      this.validating = true
      try {
        if (await this.$refs.formRef.validate().catch(() => false)) this.$emit('save', { ...this.form })
      } finally { this.validating = false }
    }
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue" :title="node ? '编辑菜单节点' : '新增菜单节点'" width="min(570px, 94vw)"
    :show-close="!busy" :close-on-click-modal="!busy" :close-on-press-escape="!busy"
    @update:model-value="close" @open="initialize"
  >
    <el-alert v-if="node?.builtin" title="内置节点仅支持修改名称和排序。" type="info" :closable="false" class="notice" />
    <el-form ref="formRef" :model="form" :rules="rules" :validate-on-rule-change="false" :disabled="busy" label-position="top" @submit.prevent="submit">
      <el-form-item label="节点名称" prop="name"><el-input v-model.trim="form.name" maxlength="100" placeholder="请输入节点名称" /></el-form-item>
      <div class="form-columns">
        <el-form-item label="节点类型">
          <el-select v-model="form.type" :disabled="node?.builtin" aria-label="节点类型" @change="typeChanged">
            <el-option v-for="(label, value) in typeLabels" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" :precision="0" aria-label="排序" /></el-form-item>
      </div>
      <el-form-item label="上级节点" prop="parentId">
        <el-select v-model="form.parentId" clearable filterable :disabled="node?.builtin" :placeholder="form.type === 'BUTTON' ? '请选择所属页面' : '不选表示根节点'" aria-label="上级节点">
          <el-option v-for="item in parents" :key="item.id" :label="`${item.name}（${typeLabels[item.type]}）`" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.type === 'MENU'" label="页面路径" prop="path">
        <el-input v-model.trim="form.path" :disabled="node?.builtin" maxlength="200" placeholder="/example.html" />
        <p class="field-hint">路径需对应已有业务页面；新页面还需实现组件与路由映射。</p>
      </el-form-item>
      <el-form-item v-if="form.type !== 'DIRECTORY'" label="权限标识" prop="permission">
        <el-input v-model.trim="form.permission" :disabled="node?.builtin" maxlength="100" placeholder="例如：example:read" />
      </el-form-item>
      <el-form-item label="状态"><el-switch v-model="form.enabled" :disabled="node?.builtin" active-text="启用" inactive-text="停用" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="busy" @click="close()">取消</el-button>
      <el-button type="primary" :loading="busy || validating" @click="submit">保存节点</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.notice { margin-bottom: 20px; }
.form-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.form-columns .el-input-number { width: 100%; }
.field-hint { margin-top: 6px; color: #939eb0; font-size: 12px; line-height: 1.6; }
</style>
