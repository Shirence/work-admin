<script setup>
import { ref } from 'vue'
defineProps({ modelValue: { type: String, required: true }, effort: { type: String, required: true }, busy: Boolean, canSend: Boolean })
const emit = defineEmits(['update:modelValue', 'update:effort', 'send', 'stop', 'keydown'])
const composing = ref(false)
const compositionEndedAt = ref(0)
function keydown(event) {
  if (event.key === 'Enter' && !event.shiftKey && (composing.value || Date.now() - compositionEndedAt.value < 500)) {
    event.preventDefault()
    return
  }
  emit('keydown', event)
}
const efforts = [{ value: 'low', label: '快速' }, { value: 'medium', label: '标准' }, { value: 'high', label: '深入' }, { value: 'xhigh', label: '更深入' }, { value: 'max', label: '最高' }]
</script>

<template>
  <footer class="composer">
    <div class="input-box">
      <textarea :value="modelValue" maxlength="2000" rows="3" aria-label="输入 AI 问题"
        placeholder="输入你的问题…" @compositionstart="composing = true" @compositionend="composing = false; compositionEndedAt = Date.now()" @input="emit('update:modelValue', $event.target.value)" @keydown="keydown" />
      <div class="input-toolbar">
        <label class="effort">思考强度
          <select :value="effort" :disabled="busy" @change="emit('update:effort', $event.target.value)">
            <option v-for="option in efforts" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
        <div class="send-actions"><span class="count">{{ modelValue.length }} / 2000</span>
          <button v-if="busy" class="send stop" @click="emit('stop')">■ 停止生成</button>
          <button v-else class="send" :disabled="!canSend" @click="emit('send')">发送 <span aria-hidden="true">↑</span></button>
        </div>
      </div>
    </div>
    <p class="hint"><span>Enter 发送 / Shift + Enter 换行</span><span>回答结束后自动保存</span></p>
  </footer>
</template>

<style scoped>
.composer { padding:18px 26px 12px; border-top:0.5px solid #edf0f2; background:white; }
.input-box { border:0.5px solid #dde5e9; border-radius:12px; padding:12px 14px; max-width:820px; margin:auto; box-sizing:border-box; transition:border-color .2s; }.input-box:focus-within { border-color:#439b7e; box-shadow:0 0 0 2px #edf7f2; }
textarea { display:block; box-sizing:border-box; width:100%; border:none; background:transparent; outline:none; resize:none; font:inherit; font-size:14px; line-height:1.7; color:#2c4050; min-height:72px; max-height:160px; field-sizing:content; overflow-y:auto; scrollbar-width:thin; }textarea::placeholder { color:#9ba6b0; }
.input-toolbar,.send-actions { display:flex; align-items:center; gap:12px; }.input-toolbar { justify-content:space-between; margin-top:10px; }.effort { display:flex; align-items:center; gap:8px; color:#83909b; font-size:12px; }
select { color:#4c6157; border:1px solid #e5ece8; background:#f5f8f6; border-radius:6px; padding:5px 6px; font:inherit; cursor:pointer; }.count { font-size: 12px; color:#99a4ae; }
.send { display:flex; gap:12px; align-items:center; border:0; border-radius:8px; background:#208566; color:white; padding:9px 16px; font:inherit; font-size:13px; cursor:pointer; }.send:hover { background:#166b50; }.send:disabled { background:#dbe6e1; color:#93a69d; cursor:not-allowed; }.stop { background:#536979; }.stop:hover { background:#405765; }
.hint { display:flex; justify-content:space-between; flex-wrap:wrap; gap:6px; max-width:820px; margin:10px auto 0; color:#a0aab3; font-size: 12px; }
@media(max-width:700px) { .composer { padding:12px 14px; }.effort { gap:4px; }.send-actions { gap:7px; }.send { padding:9px 12px; } }
</style>
