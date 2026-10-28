<script setup>
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { workPayload } from './workModel'
const props = defineProps({ modelValue: Boolean, config: { type: Object, required: true }, record: { type: Object, default: null }, people: { type: Array, default: () => [] }, busy: Boolean })
const emit = defineEmits(['update:modelValue', 'save'])
const form = reactive({})
watch(() => props.modelValue, open => { if (open) { Object.keys(form).forEach(key => delete form[key]); Object.assign(form, props.config.defaults(), props.record || {}) } })
function submit() {
  if (props.busy) return
  try { emit('save', workPayload(props.config, form)) } catch (error) { ElMessage.warning(error.message) }
}
</script>
<template>
  <el-dialog :model-value="modelValue" :title="`${record ? '编辑' : '新增'}${config.singular}`" width="min(660px, 94vw)" :show-close="!busy" :close-on-click-modal="!busy" :close-on-press-escape="!busy" @update:model-value="!busy && emit('update:modelValue', $event)">
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item v-for="field in config.fields" :key="field.key" :label="field.label" :required="field.required">
        <el-switch v-if="field.type === 'switch'" v-model="form[field.key]" :disabled="busy" />
        <el-date-picker v-else-if="field.type === 'date'" v-model="form[field.key]" type="date" value-format="YYYY-MM-DD" :disabled="busy" :placeholder="field.label" style="width:100%" />
        <el-select v-else-if="field.options || field.people" v-model="form[field.key]" :disabled="busy" :placeholder="`选择${field.label}`" :clearable="field.people" filterable style="width:100%" @clear="form[field.key] = null">
          <el-option v-for="option in field.people ? people.map(p => ({ value: p.id, label: `${p.name || p.account} (${p.account})` })) : field.options" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-input v-else v-model="form[field.key]" :type="field.type === 'textarea' ? 'textarea' : 'text'" :rows="field.type === 'textarea' ? 5 : undefined" :maxlength="field.max" show-word-limit :disabled="busy" :placeholder="field.label" />
      </el-form-item>
    </el-form>
    <template #footer><el-button :disabled="busy" @click="emit('update:modelValue', false)">取消</el-button><el-button type="primary" :loading="busy" @click="submit">保存{{ config.singular }}</el-button></template>
  </el-dialog>
</template>
