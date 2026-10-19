const STORAGE_KEY = 'workadmin-theme'

function systemTheme() {
  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark'
  }
  return 'light'
}

export function getTheme() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  return systemTheme()
}

export function applyTheme(theme, persist = true) {
  const next = theme === 'dark' ? 'dark' : 'light'
  document.documentElement.classList.toggle('dark', next === 'dark')
  if (persist) localStorage.setItem(STORAGE_KEY, next)
  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta) meta.setAttribute('content', next === 'dark' ? '#05080d' : '#f3f7f8')
  window.dispatchEvent(new CustomEvent('theme-change', { detail: { theme: next } }))
}

export function toggleTheme() {
  applyTheme(document.documentElement.classList.contains('dark') ? 'light' : 'dark')
}

export function initTheme() {
  applyTheme(getTheme(), false)
}
