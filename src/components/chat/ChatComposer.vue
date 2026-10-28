<script>
export default {
  name: 'ChatComposer',
  props: { modelValue: { type: String, required: true }, canSend: Boolean, selected: Boolean, hint: { type: String, default: '' } },
  emits: ['update:modelValue', 'send', 'keydown'],
  data: () => ({ emojiVisible: false, composing: false, compositionEndedAt: 0, emojis: ['😊', '😄', '👍', '👏', '🎉', '❤️', '🙏', '🤝', '✅', '💡', '☕', '🌹'] }),
  watch: { selected() { this.emojiVisible = false } },
  methods: {
    onKeydown(event) {
      if (event.key === 'Enter' && !event.shiftKey && (this.composing || Date.now() - this.compositionEndedAt < 500)) {
        event.preventDefault()
        return
      }
      this.$emit('keydown', event)
    },
    emoji(value) {
      if (this.modelValue.length + value.length > 2000) return
      this.$emit('update:modelValue', this.modelValue + value)
      this.emojiVisible = false
      this.$nextTick(() => this.$refs.input.focus())
    }
  }
}
</script>

<template>
  <footer class="composer" :class="{ inactive: !selected }">
    <div class="composer-toolbar">
      <div class="emoji-control">
        <button class="emoji-toggle" type="button" aria-label="选择表情" :aria-expanded="emojiVisible" :disabled="!selected" @click="emojiVisible = !emojiVisible"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" /><circle cx="8.7" cy="9.5" r=".7" /><circle cx="15.3" cy="9.5" r=".7" /><path d="M7.8 14.2c1.2 3 7.2 3 8.4 0" /></svg></button>
        <div v-if="emojiVisible" class="emoji-picker" aria-label="表情列表"><button v-for="item in emojis" :key="item" type="button" :aria-label="`插入表情 ${item}`" @click="emoji(item)">{{ item }}</button></div>
      </div>
      <span class="composer-tip">{{ hint || 'Enter 发送，Shift + Enter 换行' }}</span>
    </div>
    <textarea ref="input" :value="modelValue" :disabled="!selected" maxlength="2000" :placeholder="selected ? '输入消息…' : '选择群聊后开始发送消息'" aria-label="输入消息" @compositionstart="composing = true" @compositionend="composing = false; compositionEndedAt = Date.now()" @input="$emit('update:modelValue', $event.target.value)" @keydown="onKeydown" />
    <div class="composer-footer"><span class="word-count">{{ modelValue.length }} / 2000</span><button class="send-button" type="button" :disabled="!canSend || !modelValue.trim()" @click="$emit('send')">发送</button></div>
  </footer>
</template>

<style scoped>
.composer { flex-shrink: 0; height: auto; min-height: 150px; max-height: 250px; padding: 12px 23px 14px; border-top: 0.5px solid #e5e5e5; background: #fff; display: flex; flex-direction: column; }
.composer.inactive { background: #fafafa; }
.composer-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 15px; height: 25px; margin-bottom: 6px; }
.emoji-control { position: relative; }
.emoji-toggle { padding: 0; border: 0; background: none; color: #666; width: 24px; height: 24px; cursor: pointer; }
.emoji-toggle:disabled { color: #c4c4c4; cursor: default; }
.emoji-toggle svg { width: 22px; height: 22px; fill: none; stroke: currentColor; stroke-width: 1.5; }
.emoji-toggle svg circle:not(:first-child) { fill: currentColor; stroke: none; }
.composer-tip { color: #b0b0b0; font-size: 12px; text-align: right; }
textarea { flex: 1; min-height: 56px; max-height: 150px; width: 100%; padding: 4px 0; resize: none; outline: none; border: 0; color: #333; background: transparent; font: inherit; font-size: 14px; line-height: 1.7; field-sizing: content; overflow-y: auto; scrollbar-width: thin; }
textarea::placeholder { color: #c5c5c5; }
textarea:disabled { cursor: default; }
.composer-footer { display: flex; align-items: center; justify-content: flex-end; gap: 16px; margin-top: 6px; }
.word-count { color: #c0c0c0; font-size: 12px; }
.send-button { min-width: 83px; padding: 7px 20px; border: 0; border-radius: 4px; color: #fff; background: #07b75a; font-size: 13px; cursor: pointer; }
.send-button:hover { background: #06a750; }
.send-button:disabled { color: #aaa; background: #ededed; cursor: default; }
.emoji-picker { position: absolute; bottom: 34px; left: -4px; z-index: 5; display: grid; grid-template-columns: repeat(6, 1fr); gap: 3px; width: 236px; padding: 12px; border: 1px solid #e3e3e3; border-radius: 8px; background: #fff; box-shadow: 0 5px 22px #00000012; }
.emoji-picker button { height: 32px; padding: 0; border: 0; border-radius: 4px; background: none; font-size: 22px; cursor: pointer; }
.emoji-picker button:hover { background: #f1f1f1; }
button:focus-visible { outline: 2px solid #07c160; outline-offset: 2px; }
@media (max-width: 900px) { .composer { padding-left: 18px; padding-right: 18px; } }
</style>
