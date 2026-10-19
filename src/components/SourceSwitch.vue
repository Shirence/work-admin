<template>
  <div class="source-switch" role="group" aria-label="数据源">
    <button
      type="button"
      class="source-btn"
      :class="{ active: mode === 'local' }"
      :disabled="mode === 'local'"
      @click="select('local')"
    >
      本地 H2
    </button>
    <button
      type="button"
      class="source-btn"
      :class="{ active: mode === 'remote' }"
      :disabled="mode === 'remote'"
      @click="select('remote')"
    >
      远程 MySQL
    </button>
  </div>
</template>

<script>
export default {
  name: 'SourceSwitch',
  props: {
    mode: {
      type: String,
      default: 'local'
    }
  },
  emits: ['change'],
  methods: {
    select(mode) {
      if (mode !== this.mode) this.$emit('change', mode)
    }
  }
}
</script>

<style scoped>
.source-switch {
  display: flex;
  gap: 2px;
  padding: 2px;
  background: var(--bg-inset);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
}
.source-btn {
  border: 0;
  background: transparent;
  color: var(--text-3);
  font-family: var(--mono);
  font-size: 10px;
  letter-spacing: .04em;
  padding: 5px 9px;
  border-radius: 5px;
  cursor: pointer;
  white-space: nowrap;
  transition: color var(--dur-1) var(--ease-2), background var(--dur-1) var(--ease-2);
}
.source-btn:hover:not(:disabled) { color: var(--text); }
.source-btn.active {
  color: var(--accent);
  background: var(--surface);
  box-shadow: var(--shadow-xs);
  font-weight: 600;
}
.source-btn:disabled { cursor: default; }
</style>
