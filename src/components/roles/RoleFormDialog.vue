<script>
export default {
  name: 'RoleFormDialog',
  props: { modelValue: Boolean, role: { type: Object, default: null }, busy: Boolean },
  emits: ['update:modelValue', 'save'],
  data: () => ({
    form: { name: '', code: '', description: '', enabled: true },
    validating: false,
    rules: {
      name: [
        { required: true, whitespace: true, message: '请输入角色名称', trigger: 'blur' },
        { max: 100, message: '角色名称最多 100 个字符', trigger: 'blur' }
      ],
      code: [
        { required: true, message: '请输入角色编码', trigger: 'blur' },
        { pattern: /^[a-zA-Z][a-zA-Z0-9_:-]{0,63}$/, message: '以字母开头，仅限字母、数字、下划线、冒号或连字符，最多 64 位', trigger: 'blur' }
      ],
      description: [{ max: 255, message: '说明最多 255 个字符', trigger: 'blur' }]
    }
  }),
  methods: {
    async initialize() {
      this.form = {
        name: this.role?.name || '', code: this.role?.code || '',
        description: this.role?.description || '', enabled: this.role?.enabled ?? true
      }
      await this.$nextTick()
      this.$refs.formRef?.clearValidate()
    },
    close(value = false) { if (!this.busy && !this.validating) this.$emit('update:modelValue', value) },
    async submit() {
      if (this.busy || this.validating) return
      this.validating = true
      try {
        const valid = await this.$refs.formRef.validate().catch(() => false)
        if (valid) this.$emit('save', { ...this.form })
      } finally { this.validating = false }
    }
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue" :title="role ? '编辑角色' : '新增角色'" width="min(520px, 92vw)"
    :show-close="!busy" :close-on-click-modal="!busy" :close-on-press-escape="!busy"
    @update:model-value="close" @open="initialize"
  >
    <el-alert v-if="role?.builtin" title="内置角色的编码和启用状态不可修改" type="info" :closable="false" class="form-notice" />
    <el-form ref="formRef" :model="form" :rules="rules" :disabled="busy" label-position="top" @submit.prevent="submit">
      <el-form-item label="角色名称" prop="name"><el-input v-model.trim="form.name" maxlength="100" placeholder="例如：用户只读" /></el-form-item>
      <el-form-item label="角色编码" prop="code">
        <el-input v-model.trim="form.code" :disabled="role?.builtin" maxlength="64" placeholder="例如：student01_user_reader" />
        <p class="field-hint">以英文字母开头，建议使用学号或团队前缀避免重复。</p>
      </el-form-item>
      <el-form-item label="说明" prop="description"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="选填，描述此角色的职责和使用范围" /></el-form-item>
      <el-form-item label="状态"><el-switch v-model="form.enabled" :disabled="role?.builtin" active-text="启用" inactive-text="停用" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="busy" @click="close()">取消</el-button>
      <el-button type="primary" :loading="busy || validating" @click="submit">保存角色</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.field-hint { margin-top: 6px; color: #8a94a6; font-size: 12px; line-height: 1.6; }
.form-notice { margin-bottom: 20px; }
</style>
