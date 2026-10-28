import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useStore } from 'vuex'
import { streamMessage } from '../api/message'
import { buildHistory } from '../utils/sse'

export function useAiConversation({ onSettled } = {}) {
  const store = useStore()
  const turns = ref([])
  const input = shallowRef('')
  const effort = shallowRef('medium')
  const busy = shallowRef(false)
  let controller = null
  let serial = 0
  const canSend = computed(() => !busy.value && !!input.value.trim() && input.value.trim().length <= 2000)
  const activeModel = computed(() => [...turns.value].reverse().find(turn => turn.model)?.model || '')
  function stop() { controller?.abort() }

  async function generate(turn) {
    if (busy.value || turn !== turns.value[turns.value.length - 1]) return
    busy.value = true
    Object.assign(turn, { answer: '', model: '', status: 'pending', note: '正在连接…', errorStatus: null })
    const token = store.state.token
    const pending = new AbortController()
    controller = pending
    try {
      await streamMessage({ message: turn.question, history: turn.history, effort: turn.effort }, {
        signal: pending.signal,
        onEvent(event, data) {
          if (pending.signal.aborted) return
          if (event === 'start') {
            turn.model = typeof data.model === 'string' ? data.model : ''
            turn.note = '正在思考…'
          } else if (event === 'delta') {
            turn.answer += data.text
            turn.note = '正在生成…'
          }
        }
      })
      pending.signal.throwIfAborted()
      if (!turn.answer.trim()) throw new Error('AI 未返回文本，请重新生成')
      turn.status = 'completed'
      turn.note = '回复完成'
    } catch (error) {
      turn.status = pending.signal.aborted ? 'stopped' : 'failed'
      turn.note = pending.signal.aborted ? '已停止生成' : error.message || '生成失败，请重试'
      turn.errorStatus = error.status || null
    } finally {
      if (onSettled && token === store.state.token) await onSettled(turns.value)
      if (controller === pending) { controller = null; busy.value = false }
    }
  }

  function send() {
    if (!canSend.value) return
    const question = input.value.trim()
    turns.value.push({ id: ++serial, question, answer: '', model: '', status: 'pending', note: '',
      effort: effort.value, history: buildHistory(turns.value, question) })
    input.value = ''
    return generate(turns.value[turns.value.length - 1])
  }
  function retry() {
    const turn = turns.value[turns.value.length - 1]
    if (turn && ['failed', 'stopped'].includes(turn.status)) return generate(turn)
  }
  function clear() { if (!busy.value) turns.value = [] }
  function restore(saved) {
    if (busy.value) return
    turns.value = saved.map((turn, index) => ({ ...turn, id: index + 1, history: buildHistory(saved.slice(0, index), turn.question) }))
    serial = turns.value.length
  }
  function keydown(event) {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing && event.keyCode !== 229) {
      event.preventDefault(); send()
    }
  }
  watch(() => store.state.token, () => { stop(); turns.value = []; input.value = '' }, { flush: 'sync' })
  onMounted(() => window.addEventListener('pagehide', stop))
  onBeforeUnmount(() => { stop(); window.removeEventListener('pagehide', stop) })
  return { turns, input, effort, busy, canSend, activeModel, send, stop, retry, clear, restore, keydown }
}
