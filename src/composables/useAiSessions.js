import { computed, onMounted, ref } from 'vue'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConversations, getConversation, createConversation, saveConversation, deleteConversation, exportConversation } from '../api/conversation'
import { useAiConversation } from './useAiConversation'
export function useAiSessions() {
  const store = useStore(), sessions = ref([]), activeId = ref(null), loading = ref(false), persistenceError = ref(''), title = ref('新对话')
  const chat = useAiConversation({ onSettled: persist })
  const busy = computed(() => chat.busy.value || loading.value)
  async function refresh() { sessions.value = await getConversations() }
  async function persist(turns = chat.turns.value) {
    if (!activeId.value) return
    try { await saveConversation(activeId.value, { title: title.value, turns }); persistenceError.value = ''; await refresh() }
    catch (error) { persistenceError.value = error.userMessage || '对话保存失败，离开前请点击重新保存或导出内容' }
  }
  async function choose(id) {
    if (busy.value || persistenceError.value) return
    loading.value = true
    try { const result = await getConversation(id); activeId.value = result.id; title.value = result.title; chat.restore(result.turns); chat.input.value = '' }
    catch {} finally { loading.value = false }
  }
  async function send() {
    if (busy.value || !chat.canSend.value) return
    loading.value = true
    try {
      if (!activeId.value) {
        const result = await createConversation(chat.input.value.trim().slice(0,100)); activeId.value = result.id; title.value = result.title
      }
      await chat.send()
    } catch {} finally { loading.value = false }
  }
  function clear() {
    if (busy.value || persistenceError.value) return
    chat.clear(); activeId.value = null; title.value = '新对话'; chat.input.value = ''
  }
  async function remove(session) {
    if (busy.value) return
    loading.value = true
    try {
      await ElMessageBox.confirm(`确定删除对话“${session.title}”？`, '删除对话', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
      await deleteConversation(session.id)
      if (activeId.value === session.id) { activeId.value = null; chat.clear(); title.value = '新对话'; persistenceError.value = '' }
      await refresh()
    } catch {} finally { loading.value = false }
  }
  async function rename() {
    if (busy.value || !activeId.value) return
    loading.value = true
    try {
      const { value } = await ElMessageBox.prompt('输入对话标题', '重命名对话', { inputValue: title.value, inputValidator: value => !!value?.trim() && value.trim().length <= 100 || '标题为 1–100 字符' })
      title.value = value.trim(); await persist()
    } catch {} finally { loading.value = false }
  }
  async function exportFile() {
    if (busy.value || !activeId.value) return
    loading.value = true
    try {
      // Export the visible copy even if the last network save failed.
      const content = persistenceError.value ? `# ${title.value}\n\n${chat.turns.value.map(t => `## 用户\n\n${t.question}\n\n## 助手\n\n${t.answer}`).join('\n\n')}` : await exportConversation(activeId.value)
      const url = URL.createObjectURL(new Blob([content], { type: 'text/markdown;charset=utf-8' })), anchor = document.createElement('a')
      anchor.href = url; anchor.download = `对话-${activeId.value}.md`; anchor.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch { ElMessage.error('导出失败，请重试') } finally { loading.value = false }
  }
  function keydown(event) { if (event.key === 'Enter' && !event.shiftKey && !event.isComposing && event.keyCode !== 229) { event.preventDefault(); send() } }
  async function saveAgain() {if(busy.value)return; loading.value=true; try {await persist()} finally {loading.value=false}}
  onMounted(async () => {loading.value = true; try {await refresh()} catch {} finally {loading.value = false}})
  return { ...chat, sessions, activeId, title, persistenceError, busy, send, clear, choose, remove, rename, exportFile, keydown, saveAgain, canSend: computed(() => !busy.value && chat.canSend.value), userId: computed(() => store.state.userInfo?.id) }
}
