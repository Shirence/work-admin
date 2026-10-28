<script setup>
import { useAiSessions } from '../../composables/useAiSessions'
import AiSessionList from './AiSessionList.vue'
import AiConversation from './AiConversation.vue'
import AiComposer from './AiComposer.vue'
const { turns, input, effort, busy, canSend, activeModel, sessions, activeId, persistenceError, choose, remove, rename, exportFile, saveAgain, send, stop, retry, clear, keydown } = useAiSessions()
</script>

<template>
  <section class="ai-workspace" aria-label="AI 对话">
    <header class="ai-header">
      <div class="ai-identity">
        <span class="ai-mark" aria-hidden="true">✦</span>
        <div><h1>AI 对话</h1><p>{{ activeModel || '你的工作与学习助手' }}</p></div>
      </div>
      <div class="ai-actions"><el-button :disabled="busy || !activeId" @click="rename">重命名</el-button><el-button :disabled="busy || !activeId" @click="exportFile">导出</el-button><el-button :disabled="busy" @click="clear">新对话</el-button></div>
    </header>
    <el-alert v-if="persistenceError" type="error" :closable="false" :title="persistenceError"><el-button :disabled="busy" @click="saveAgain">重新保存</el-button></el-alert>
    <div class="ai-body"><AiSessionList :sessions="sessions" :active-id="activeId" :busy="busy || !!persistenceError" @choose="choose" @remove="remove" /><div class="ai-main">
    <AiConversation :turns="turns" :busy="busy" @retry="retry" @suggest="input = $event" />
    <AiComposer v-model="input" v-model:effort="effort" :busy="busy" :can-send="canSend"
      @send="send" @stop="stop" @keydown="keydown" />
    </div></div>
  </section>
</template>

<style scoped>
.ai-workspace { display:flex; flex-direction:column; height:var(--app-workspace-height,calc(100dvh - 100px)); min-height:480px; max-width:1160px; margin:0 auto; background:#fff; border:0.5px solid #e5e9ed; border-radius:14px; overflow:hidden; }
.ai-body { display:flex; flex:1; min-height:0; }.ai-main { display:flex; flex:1; min-width:0; flex-direction:column; }.ai-actions { display:flex; gap:6px; }.ai-actions .el-button { margin:0; }
.ai-header { display:flex; justify-content:space-between; align-items:center; gap:16px; padding:18px 26px; border-bottom:0.5px solid #edf0f2; }
.ai-identity { display:flex; align-items:center; gap:12px; min-width:0; }
.ai-mark { display:grid; place-items:center; flex-shrink:0; width:42px; height:42px; border-radius:12px; background:#e9f6f1; color:#168361; font-size:25px; }
h1 { font-size:18px; margin:0 0 5px; color:#22313d; }
p { margin:0; color:#7a8793; font-size:12px; overflow-wrap:anywhere; }
@media(max-width:700px) { .ai-body { flex-direction:column; }.ai-header { flex-wrap:wrap; }.ai-actions .el-button { padding:7px 10px; } .ai-body { display:flex; flex:1; min-height:0; }.ai-main { display:flex; flex:1; min-width:0; flex-direction:column; }.ai-actions { display:flex; gap:6px; }.ai-actions .el-button { margin:0; }
.ai-header { padding:15px; } .ai-workspace { border-radius:8px; } }
</style>
