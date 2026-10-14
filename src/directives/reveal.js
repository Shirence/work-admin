/**
 * v-reveal —— 元素进入视口时淡入上移（呼吸感）
 *   v-reveal                  直接使用
 *   v-reveal="120"            延迟 120ms，用来做错落
 *   v-reveal="{ delay: 120 }" 同上
 */
let observer = null

function getObserver() {
  if (observer) return observer
  if (typeof IntersectionObserver === 'undefined') return null
  observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.08, rootMargin: '0px 0px -32px 0px' })
  return observer
}

export default {
  mounted(el, binding) {
    el.classList.add('reveal')
    const v = binding.value
    const delay = typeof v === 'number' ? v : (v && v.delay) || 0
    if (delay) el.style.transitionDelay = delay + 'ms'

    const ob = getObserver()
    if (!ob) {
      el.classList.add('is-visible')
      return
    }
    ob.observe(el)
  },
  unmounted(el) {
    if (observer) observer.unobserve(el)
  }
}
