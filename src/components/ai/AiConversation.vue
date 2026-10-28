<script setup>
import { nextTick, ref, shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownContent from '../common/MarkdownContent.vue'
const props = defineProps({ turns: { type: Array, required: true }, busy: Boolean })
const emit = defineEmits(['retry', 'suggest'])
const scroller = ref(null)
const follow = shallowRef(true)
const suggestions = ['帮我整理一份今天的工作计划', '解释一下 WebSocket 和 HTTP 的区别', '帮我把这段文字润色得更清晰']
async function copy(turn) {
  if (!turn.answer) return
  try {
    await navigator.clipboard.writeText(turn.answer)
    ElMessage.success('已复制原始 Markdown')
  } catch {
    ElMessage.error('复制失败，请手动选择回答内容')
  }
}
function trackScroll() {
  const el = scroller.value
  if (el) follow.value = el.scrollHeight - el.scrollTop - el.clientHeight < 100
}
watch(() => [props.turns.length, props.turns.at(-1)?.answer, props.turns.at(-1)?.note], async (value, previous) => {
  const added = value[0] !== previous?.[0]
  await nextTick()
  if (scroller.value && (follow.value || added)) scroller.value.scrollTop = scroller.value.scrollHeight
}, { flush: 'post' })
</script>

<template>
  <div ref="scroller" class="conversation" aria-label="AI 聊天记录" @scroll="trackScroll">
    <div v-if="!turns.length" class="welcome">
      <span class="welcome-icon" aria-hidden="true">✦</span>
      <h2>有什么可以帮你？</h2>
      <p>一起梳理思路、解答问题，或完成下一项工作。</p>
      <div class="suggestions">
        <button v-for="suggestion in suggestions" :key="suggestion" @click="emit('suggest', suggestion)">{{ suggestion }} <span aria-hidden="true">↗</span></button>
      </div>
    </div>
    <div v-else class="turns">
      <article v-for="(turn, index) in turns" :key="turn.id" class="turn">
        <div class="question"><span class="question-label">你</span><div>{{ turn.question }}</div></div>
        <div class="response">
          <span class="assistant-avatar" aria-hidden="true">✦</span>
          <div class="answer-body">
            <div class="assistant-label">{{ turn.model || 'AI 助手' }}</div>
            <MarkdownContent v-if="turn.answer" :content="turn.answer" />
            <div v-else-if="turn.status === 'pending'" class="thinking"><span class="pulse" />{{ turn.note || '正在连接…' }}</div>
            <div v-else class="no-answer">{{ turn.status === 'stopped' ? '本次生成已停止' : '本次未生成回答' }}</div>
            <div v-if="turn.status === 'failed'" class="error" role="alert">
              <strong>生成未完成</strong><p>{{ turn.note }}</p>
              <p v-if="turn.errorStatus === 503" class="error-help">AI 服务暂不可用，请联系管理员检查服务配置后重试。</p>
            </div>
            <div class="answer-footer">
              <span role="status">{{ turn.status === 'failed' ? '可重试此问题' : turn.note }}</span>
              <el-button size="small" :disabled="busy || !turn.answer" @click="copy(turn)">复制回答</el-button>
              <el-button v-if="index === turns.length - 1 && ['failed', 'stopped'].includes(turn.status)"
                size="small" :disabled="busy" @click="emit('retry')">重新生成</el-button>
            </div>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.conversation { flex:1; min-height:0; overflow:auto; background:#fafbfc; overscroll-behavior:contain; }
.welcome { min-height:100%; box-sizing:border-box; display:flex; flex-direction:column; justify-content:center; align-items:center; text-align:center; padding:32px 24px; }
.welcome-icon { color:#198465; font-size:48px; margin-bottom:16px; }
h2 { margin:0 0 12px; font-size:26px; font-weight:600; color:#253744; }
.welcome p { margin:0; color:#7d8a96; font-size:14px; line-height:1.7; }
.suggestions { display:flex; gap:12px; flex-wrap:wrap; justify-content:center; margin-top:30px; max-width:760px; }
.suggestions button { width:220px; padding:16px; background:white; border:1px solid #e3e9ed; border-radius:10px; text-align:left; color:#516271; cursor:pointer; font:inherit; font-size:13px; line-height:1.7; }
.suggestions button:hover { border-color:#6dac99; background:#f4faf7; }.suggestions span { float:right; color:#168361; }
.turns { max-width:820px; margin:0 auto; padding:28px 30px; }.turn + .turn { margin-top:32px; }
.question { display:flex; flex-direction:column; align-items:flex-end; gap:8px; margin:0 0 24px 48px; }.question-label { font-size:12px; color:#7a8793; }
.question > div { white-space:pre-wrap; overflow-wrap:anywhere; background:#e9f4ef; color:#263f34; border-radius:14px 2px 14px 14px; padding:12px 18px; font-size:14px; line-height:1.8; max-width:100%; box-sizing:border-box; }
.response { display:flex; align-items:flex-start; gap:12px; }.assistant-avatar { display:grid; place-items:center; width:32px; height:32px; flex-shrink:0; color:#158060; background:#e6f2ed; border-radius:9px; font-size:22px; }
.answer-body { flex:1; min-width:0; }.assistant-label { margin:7px 0 12px; font-size:12px; font-weight:600; color:#526372; overflow-wrap:anywhere; }
.thinking,.no-answer { color:#85919c; font-size:14px; line-height:1.8; }
.pulse { display:inline-block; width:7px; height:7px; background:#218b68; border-radius:50%; margin-right:8px; animation:pulse 1.3s ease-in-out infinite; }
.answer-footer { display:flex; gap:14px; align-items:center; min-height:26px; margin-top:12px; color:#8c98a2; font-size:12px; }
.error { padding:12px 16px; margin-top:12px; border:1px solid #f2d9d3; border-radius:8px; background:#fff7f4; color:#ad4b38; font-size:13px; line-height:1.7; }.error strong { font-weight:600; }.error p { margin:4px 0 0; }.error-help { color:#927269; }
@keyframes pulse { 50% { opacity:.35; } }@media(prefers-reduced-motion:reduce) { .pulse { animation:none; } }
@media(max-width:700px) { .turns { padding:22px 16px; }.question { margin-left:20px; }.suggestions { gap:8px; }.suggestions button { width:100%; }.welcome { padding:28px 20px; }h2 { font-size:23px; } }
</style>
