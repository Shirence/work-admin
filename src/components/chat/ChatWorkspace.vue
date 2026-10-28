<script>
import { getGroups, createGroup, joinGroup, getHistory } from '../../api/chat'
import { messageId, messageKey, mergeMessages } from '../../utils/chat'
import { expireSession } from '../../api/request'
import ChatGroupList from './ChatGroupList.vue'
import ChatTimeline from './ChatTimeline.vue'
import ChatComposer from './ChatComposer.vue'
export default {
  name: 'ChatWorkspace',
  components: { ChatGroupList, ChatTimeline, ChatComposer },
  data: () => ({ groups: [], cursor: 0, hasMoreGroups: true, groupBusy: false,
    createVisible: false, mobileChat: false, groupName: '', chosen: null, selected: null, input: '', messages: [],
    messageKeyword: '',
    ready: false, subscribed: false, historyLoading: false, status: '尚未连接', notice: '',
    labels: {sending:'发送中',pending:'已接收，正在保存',saved:'已保存',failed:'未确认或未保存成功，可重试'} }),
  computed: {
    activeGroup() { return this.groups.find(group => group.id === this.selected) || null },
    composerHint() {
      if (!this.selected) return '选择一个群聊，开始聊天'
      if (!this.ready) return '连接恢复后即可发送，草稿会保留'
      if (this.historyLoading) return '正在同步聊天记录，请稍候'
      if (!this.subscribed) return '正在准备群聊，暂时无法发送'
      return ''
    },
    userId() { return this.$store.state.userInfo?.id },
    visibleMessages() { return this.messages.filter(m=>m.groupId===this.selected) },
    searching() { return !!this.messageKeyword.trim() },
    displayMessages() {
      const keyword = this.messageKeyword.trim().toLowerCase()
      if (!keyword) return this.visibleMessages
      return this.visibleMessages.filter(message =>
        String(message.content || '').toLowerCase().includes(keyword) ||
        String(message.senderName || '').toLowerCase().includes(keyword))
    },
    canSend() { return this.ready && this.subscribed && !this.historyLoading && !!this.selected }
  },
  created() {
    this.ws=null; this.generation=0; this.selection=0; this.subscription=''
    this.retryCount=0; this.stopped=false; this.timers=new Map(); this.outbox=[]
    this.cacheKey=`work-admin.outbox.${this.userId}`
    try {
      const cached=JSON.parse(sessionStorage.getItem(this.cacheKey) || '[]')
      this.outbox=Array.isArray(cached) ? cached.filter(m=>m.senderId===this.userId && typeof m.content==='string').slice(-100) : []
      this.messages=this.outbox.map(m=>({...m}))
    } catch { this.outbox=[] }
    this.loadGroups(true); this.connect()
  },
  mounted() { window.addEventListener('pagehide',this.shutdown) },
  beforeUnmount() { this.shutdown(); window.removeEventListener('pagehide',this.shutdown) },
  watch: { '$store.state.token'() { this.shutdown() } },
  methods: {
    chooseGroup(id) { this.chosen = id; this.mobileChat = true; this.choose(id) },
    keyOf:messageKey,
    persist() {
      try { sessionStorage.setItem(this.cacheKey,JSON.stringify(this.outbox)) }
      catch { this.notice='本地缓存不可用，刷新后未确认消息可能需要手动核对' }
    },
    async loadGroups(reset=true) {
      if(this.groupBusy) return
      this.groupBusy=true
      try {
        const page=await getGroups(reset ? 0 : this.cursor)
        const map=new Map((reset ? [] : this.groups).map(g=>[g.id,g]))
        page.forEach(g=>map.set(g.id,g)); this.groups=[...map.values()]
        if(page.length) this.cursor=page[page.length-1].id; else if(reset) this.cursor=0
        this.hasMoreGroups=page.length===50
      } catch {} finally { this.groupBusy=false }
    },
    async create() {
      const name=this.groupName.trim()
      if(!name || this.groupBusy) return
      this.groupBusy=true
      try {
        const group=await createGroup(name)
        this.groups.push({...group,joined:true,memberCount:1}); this.groupName=''
        this.createVisible=false; this.mobileChat=true
        this.chosen=group.id; await this.choose(group.id)
      } catch {} finally { this.groupBusy=false }
    },
    async choose(id) {
      const selection=++this.selection
      this.selected=null; this.subscribed=false; this.historyLoading=false; this.notice=''; this.messageKeyword=''
      try {
        const group=this.groups.find(g=>g.id===id)
        if(!group?.joined) {
          await joinGroup(id)
          if(selection!==this.selection || this.stopped) return
          if(group) { group.joined=true; group.memberCount+=1 }
        }
        if(selection!==this.selection || this.stopped) return
        this.selected=id
        if(this.ready) this.subscribe()
      } catch { if(selection===this.selection) this.notice='加入失败，请重新选择或刷新群列表' }
    },
    write(frame) {
      if(this.ws?.readyState!==WebSocket.OPEN) return false
      this.ws.send(JSON.stringify(frame)); return true
    },
    connect() {
      if(this.stopped || !this.$store.state.token) return
      const generation=++this.generation
      const url=new URL('/ws/chat',window.location.href)
      url.protocol=location.protocol==='https:' ? 'wss:' : 'ws:'
      const socket=new WebSocket(url); this.ws=socket
      this.ready=false; this.subscribed=false; this.status='连接中…'
      this.lastData=Date.now()
      const authTimer=setTimeout(()=>socket.close(),12000)
      socket.onopen=()=> {
        if(generation!==this.generation) return
        this.write({type:'auth',token:this.$store.state.token})
      }
      socket.onmessage=event=> {
        if(generation!==this.generation || this.stopped) return
        this.lastData=Date.now()
        try {
          const frame=JSON.parse(event.data)
          if(frame.type==='ready') {
            clearTimeout(authTimer); this.ready=true; this.retryCount=0; this.status='连接正常'
            clearInterval(this.heartbeat)
            this.heartbeat=setInterval(()=> {
              if(Date.now()-this.lastData>55000) socket.close()
              else this.write({type:'ping'})
            }, 5000)
            if(this.selected) this.subscribe()
          } else if(frame.type==='subscribed') {
            if(frame.requestId===this.subscription && frame.groupId===this.selected) {
              this.subscribed=true; this.syncHistory(frame.groupId,frame.requestId,generation)
            }
          } else if(['message','ack','saved','save_failed'].includes(frame.type)) {
            this.receive(frame.message)
          } else if(frame.type==='error') this.frameError(frame)
        } catch { this.notice='收到无效消息，请检查后端协议' }
      }
      socket.onerror=()=> { if(generation===this.generation) this.status='连接异常，等待重连' }
      socket.onclose=async event=> {
        clearTimeout(authTimer)
        if(generation!==this.generation || this.stopped) return
        clearInterval(this.heartbeat); this.ready=false; this.subscribed=false
        this.status='连接已断开'
        // 1008 可能是登录或权限失效；先向 HTTP 权限接口核实，避免无限重连。
        if(event.code===1008) {
          try { await this.$store.dispatch('refreshAccess') } catch {}
          if(generation!==this.generation || this.stopped) return
          if(!this.$store.state.token || !this.$store.getters.can('chat:access')) {
            this.notice='登录或群聊权限已失效'; this.stopped=true; return
          }
        }
        const delay=Math.min(1000*2**this.retryCount++,15000)
        this.status=`连接已断开，${delay/1000} 秒后重连`
        this.reconnect=setTimeout(()=>this.connect(),delay)
      }
    },
    subscribe() {
      if(!this.ready || !this.selected) return
      this.subscribed=false; this.historyLoading=true
      this.subscription=messageId()
      this.write({type:'subscribe',groupId:this.selected,requestId:this.subscription})
    },
    async syncHistory(groupId,requestId,generation) {
      this.historyLoading=true
      const current=()=>!this.stopped && this.selected===groupId && this.subscription===requestId && this.generation===generation
      try {
        let cursor=0, more=true
        while(more && current()) {
          const page=await getHistory(groupId,cursor)
          if(!current()) return
          page.items.forEach(m=>this.receive(m))
          if(page.hasMore && page.nextAfterId<=cursor) throw new Error('历史游标没有前进')
          cursor=page.nextAfterId; more=page.hasMore
        }
        if(!current()) return
        // 先核对历史与订阅推送，再重发仍没有确认的草稿；明确失败的草稿等手动重试。
        this.outbox.filter(m=>m.groupId===groupId && m.deliveryStatus==='sending').forEach(m=>this.transmit(m))
        this.notice=''
      } catch { if(current()) { this.subscribed=false; this.notice='历史补齐失败，请点击重新补齐当前群历史' } }
      finally { if(current()) this.historyLoading=false }
    },
    receive(message) {
      if(!message || !message.clientMessageId) return
      this.messages=mergeMessages(this.messages,message)
      const key=messageKey(message)
      clearTimeout(this.timers.get(key)); this.timers.delete(key)
      if(message.senderId===this.userId) {
        this.outbox=this.outbox.filter(m=>messageKey(m)!==key)
        const actual=this.messages.find(m=>messageKey(m)===key)
        if(actual?.deliveryStatus==='failed') this.outbox.push({...actual})
        this.persist()
      }
    },
    frameError(frame) {
      this.notice=frame.message || '消息操作失败'
      if(frame.status===401) { expireSession(this.$store.state.token); this.shutdown(); return }
      if(frame.requestId===this.subscription) { this.subscribed=false; this.historyLoading=false }
      const draft=this.outbox.find(m=>m.clientMessageId===frame.requestId)
      if(draft) this.receive({...draft,deliveryStatus:'failed'})
      if(frame.status===403) {
        this.$store.dispatch('refreshAccess').then(()=> {
          if(!this.$store.getters.can('chat:access')) this.shutdown()
        }).catch(()=>{})
      }
    },
    transmit(message) {
      if(!this.ready) return
      const key=messageKey(message)
      if(!this.write({type:'send',groupId:message.groupId,content:message.content,
        clientMessageId:message.clientMessageId,requestId:message.clientMessageId})) return
      clearTimeout(this.timers.get(key))
      this.timers.set(key,setTimeout(()=> {
        const current=this.messages.find(m=>messageKey(m)===key)
        if(current?.deliveryStatus==='sending') this.receive({...current,deliveryStatus:'failed'})
      },12000))
    },
    keydown(event) {
      if(event.key==='Enter' && !event.shiftKey && !event.isComposing && event.keyCode!==229) { event.preventDefault(); this.send() }
    },
    send() {
      const content=this.input.trim()
      if(!this.canSend || !content || content.length>2000) return
      if(this.outbox.length>=100) return this.$message.warning('请先处理未确认的消息')
      const message={id:null,groupId:this.selected,senderId:this.userId,
        senderName:this.$store.state.userInfo?.name || this.$store.state.userInfo?.account,
        content,clientMessageId:messageId(),sentAt:new Date().toISOString(),deliveryStatus:'sending'}
      this.messages=mergeMessages(this.messages,message)
      this.outbox.push(message); this.persist(); this.input=''; this.transmit(message)
    },
    retry(message) {
      if(!this.canSend) return
      const draft={...message,deliveryStatus:'sending'}
      const key=messageKey(draft)
      this.messages=this.messages.map(m=>messageKey(m)===key ? draft : m)
      this.outbox=this.outbox.filter(m=>messageKey(m)!==key); this.outbox.push(draft)
      this.persist(); this.transmit(draft)
    },
    shutdown() {
      this.stopped=true; ++this.generation; this.ready=false; this.subscribed=false
      clearTimeout(this.reconnect); clearInterval(this.heartbeat)
      this.timers.forEach(timer=>clearTimeout(timer)); this.timers.clear()
      this.ws?.close(); this.ws=null
    }
  }
}
</script>

<template>
  <section class="chat-page">
    <div class="chat-shell" :class="{ 'mobile-chat': mobileChat }">
      <ChatGroupList class="group-panel" :groups="groups" :messages="messages" :chosen="chosen" :busy="groupBusy" :has-more="hasMoreGroups" :ready="ready" :status="status" @choose="chooseGroup" @create="createVisible = true" @refresh="loadGroups(true)" @load-more="loadGroups(false)" />
      <main class="conversation">
        <header class="conversation-header">
          <button class="back-button" type="button" aria-label="返回群聊列表" @click="mobileChat = false"><el-icon><ArrowLeft /></el-icon></button>
          <div class="conversation-title"><h1>{{ activeGroup?.name || '群组聊天' }}<span v-if="activeGroup?.memberCount != null">（{{ activeGroup.memberCount }}）</span></h1><p>{{ activeGroup ? '群组聊天' : '选择一个群聊开始交流' }}</p></div>
          <el-input v-if="selected" v-model="messageKeyword" class="message-search" size="small" clearable placeholder="搜索当前群消息" aria-label="搜索当前群消息" />
          <div v-if="selected && searching" class="search-count">{{ displayMessages.length }} 条</div>
          <div v-if="selected" class="conversation-actions">
            <button class="header-icon" type="button" :disabled="!ready || historyLoading" title="同步聊天记录" aria-label="同步聊天记录" @click="subscribe"><el-icon><Refresh /></el-icon></button>
            <el-popover placement="bottom-end" :width="240" trigger="click">
              <template #reference><button class="header-icon" type="button" title="群聊信息" aria-label="群聊信息"><el-icon><MoreFilled /></el-icon></button></template>
              <div class="group-info"><strong>{{ activeGroup?.name }}</strong><p v-if="activeGroup?.memberCount != null">群成员：{{ activeGroup.memberCount }} 人</p><p>已加载 {{ visibleMessages.length }} 条消息</p><p>{{ status }}</p></div>
            </el-popover>
          </div>
        </header>
        <div v-if="notice" class="chat-notice" role="status"><el-icon><Warning /></el-icon><span>{{ notice }}</span><button v-if="selected && ready && !historyLoading" type="button" @click="subscribe">重新同步</button></div>
        <ChatTimeline :messages="displayMessages" :user-id="userId" :group-id="selected" :loading="historyLoading" :can-send="canSend" :searching="searching" :labels="labels" @retry="retry" />
        <ChatComposer v-model="input" :selected="!!selected" :can-send="canSend" :hint="composerHint" @send="send" @keydown="keydown" />
      </main>
    </div>
    <el-dialog v-model="createVisible" title="创建群聊" width="min(420px, 92vw)" :show-close="!groupBusy" :close-on-click-modal="!groupBusy" :close-on-press-escape="!groupBusy">
      <el-form label-position="top" @submit.prevent="create"><el-form-item label="群聊名称"><el-input v-model="groupName" maxlength="100" show-word-limit placeholder="给群聊起个名字" :disabled="groupBusy" /></el-form-item></el-form>
      <template #footer><el-button :disabled="groupBusy" @click="createVisible = false">取消</el-button><el-button type="primary" :loading="groupBusy" :disabled="!groupName.trim()" @click="create">创建群聊</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.chat-page { height: var(--app-workspace-height, calc(100dvh - 100px)); min-height: 480px; container-name: chat; container-type: inline-size; --el-color-primary: #07b75a; }
.chat-shell { display: grid; grid-template-columns: 268px minmax(0, 1fr); height: 100%; overflow: hidden; border: 0.5px solid #dcdcdc; border-radius: 7px; background: #f5f5f5; box-shadow: 0 4px 20px #00000004; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.conversation { display: flex; flex-direction: column; min-width: 0; min-height: 0; }
.conversation-header { display: flex; align-items: center; gap: 12px; min-height: 83px; padding: 17px 26px; border-bottom: 0.5px solid #e5e5e5; background: #f5f5f5; }
.conversation-title { flex: 1; min-width: 0; }
.conversation-title h1 { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #222; font-size: 18px; font-weight: 500; line-height: 1.5; }
.conversation-title h1 span { color: #777; font-size: 15px; font-weight: 400; }
.conversation-title p { margin-top: 4px; color: #a6a6a6; font-size: 12px; }
.conversation-actions { display: flex; align-items: center; gap: 12px; }
.message-search { width: 210px; flex-shrink: 0; }
.search-count { flex-shrink: 0; color: #999; font-size: 12px; }
.header-icon, .back-button { display: grid; place-items: center; width: 28px; height: 28px; padding: 0; border: 0; color: #777; background: none; font-size: 18px; cursor: pointer; border-radius: 4px; }
.header-icon:hover, .back-button:hover { background: #e8e8e8; }
.header-icon:disabled { color: #c5c5c5; cursor: default; }
.back-button { display: none; }
.header-icon:focus-visible, .back-button:focus-visible { outline: 2px solid #07c160; }
.chat-notice { display: flex; align-items: center; gap: 8px; flex-shrink: 0; padding: 10px 24px; background: #fff7e8; color: #a77f3d; font-size: 12px; line-height: 1.6; }
.chat-notice button { flex-shrink: 0; margin-left: auto; padding: 0; border: 0; color: #8c6a32; background: none; font: inherit; text-decoration: underline; cursor: pointer; }
.group-info strong { display: block; margin-bottom: 13px; color: #333; font-size: 14px; }
.group-info p { margin: 7px 0; color: #888; font-size: 12px; }
@container chat (max-width: 640px) {
  .chat-shell { grid-template-columns: minmax(0, 1fr); }
  .chat-shell:not(.mobile-chat) .conversation { display: none; }
  .chat-shell.mobile-chat .group-panel { display: none; }
  .back-button { display: grid; }
  .conversation-header { padding: 14px 16px; min-height: 74px; }
  .conversation-header { flex-wrap: wrap; }
  .message-search { width: 100%; order: 3; }
  .search-count { order: 3; }
}
</style>
