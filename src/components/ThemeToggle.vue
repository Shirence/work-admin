<template>
  <button
    class="theme-toggle"
    type="button"
    :title="isDark ? '切换到浅色主题' : '切换到深色主题'"
    @click="toggle"
  >
    <span class="theme-glyph">{{ isDark ? '◐' : '◑' }}</span>
    <span class="theme-label mono">{{ isDark ? 'DARK' : 'LIGHT' }}</span>
  </button>
</template>

<script>
import { toggleTheme } from '../utils/theme'

export default {
  name: 'ThemeToggle',
  data() {
    return {
      isDark: document.documentElement.classList.contains('dark')
    }
  },
  mounted() {
    window.addEventListener('theme-change', this.syncTheme)
  },
  beforeUnmount() {
    window.removeEventListener('theme-change', this.syncTheme)
  },
  methods: {
    syncTheme(event) {
      this.isDark = event.detail.theme === 'dark'
    },
    toggle() {
      toggleTheme()
    }
  }
}
</script>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--line-2);
  border-radius: 2px;
  background: transparent;
  color: var(--text-3);
  cursor: pointer;
  transition: color var(--dur-1) var(--ease-2), border-color var(--dur-1) var(--ease-2), background var(--dur-1) var(--ease-2);
}
.theme-toggle:hover {
  color: var(--accent);
  border-color: var(--accent-line);
  background: var(--accent-soft);
}
.theme-glyph { font-size: 12px; line-height: 1; }
.theme-label { font-size: 9px; letter-spacing: .12em; }
</style>
