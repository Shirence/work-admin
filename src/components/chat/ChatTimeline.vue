<script>
import { messageKey } from '../../utils/chat'
import { avatarColor, initials, timelineEntries } from './chatPresentation'
export default {
  name: 'ChatTimeline',
  props: {
    messages: { type: Array, required: true }, userId: { type: [Number, String], default: null },
    groupId: { type: [Number, String], default: null }, loading: Boolean, canSend: Boolean,
    searching: Boolean,
    labels: { type: Object, required: true }
  },
  emits: ['retry'],
  data: () => ({ nearBottom: true }),
  computed: { entries() { return timelineEntries(this.messages) } },
  watch: {
    messages(next, previous) {
      const last = next[next.length - 1], old = previous?.[previous.length - 1]
      const ownNewMessage = !!last && last.senderId === this.userId && (!old || messageKey(last) !== messageKey(old))
      if (this.nearBottom || ownNewMessage) this.$nextTick(this.scrollBottom)
    },
    groupId() { this.nearBottom = true; this.$nextTick(this.scrollBottom) }
  },
  mounted() { this.scrollBottom() },
  methods: {
    avatarColor, initials,
    scrollBottom() { const element = this.$refs.scroll; if (element) element.scrollTop = element.scrollHeight },
    trackScroll() { const element = this.$refs.scroll; this.nearBottom = element.scrollHeight - element.scrollTop - element.clientHeight < 90 }
  }
}
</script>

<template>
  <div ref="scroll" class="message-scroll" aria-label="聊天记录" @scroll="trackScroll">
    <div v-if="!groupId" class="welcome-state"><span class="welcome-icon"><el-icon><ChatDotRound /></el-icon></span><h3>选择一个群聊</h3><p>从左侧打开群聊，开始交流</p></div>
    <template v-else>
      <p v-if="loading" class="sync-label"><el-icon class="spinning"><Loading /></el-icon>正在同步聊天记录…</p>
      <div v-if="!messages.length && !loading" class="empty-chat"><el-icon><ChatLineRound /></el-icon><p>{{ searching ? '没有匹配的消息' : '暂无聊天记录' }}</p><span>{{ searching ? '换个关键词试试，清空后恢复全部消息' : '发送第一条消息，开启这段对话吧' }}</span></div>
      <template v-for="entry in entries" :key="entry.key">
        <p v-if="entry.showTime && entry.time" class="time-separator">{{ entry.time }}</p>
        <article class="message-row" :class="{ own: entry.message.senderId === userId }">
          <div class="sender-avatar" :style="{ backgroundColor: avatarColor(entry.message.senderId) }" :title="entry.message.senderName || String(entry.message.senderId)">{{ initials(entry.message.senderName || entry.message.senderId) }}</div>
          <div class="message-body">
            <p v-if="entry.message.senderId !== userId" class="sender-name">{{ entry.message.senderName || `成员 ${entry.message.senderId}` }}</p>
            <div class="message-bubble">{{ entry.message.content }}</div>
            <div v-if="entry.message.senderId === userId" class="delivery" :class="{ failed: entry.message.deliveryStatus === 'failed' }">
              <el-icon v-if="entry.message.deliveryStatus === 'sending' || entry.message.deliveryStatus === 'pending'" class="spinning"><Loading /></el-icon>
              <el-icon v-else-if="entry.message.deliveryStatus === 'saved'"><Check /></el-icon>
              <el-icon v-else-if="entry.message.deliveryStatus === 'failed'"><WarningFilled /></el-icon>
              <span>{{ labels[entry.message.deliveryStatus] || entry.message.deliveryStatus }}</span>
              <button v-if="entry.message.deliveryStatus === 'failed'" type="button" :disabled="!canSend" title="使用原消息编号重试" @click="$emit('retry', entry.message)">重试</button>
            </div>
          </div>
        </article>
      </template>
    </template>
  </div>
</template>

<style scoped>
.message-scroll { flex: 1; min-height: 0; overflow-y: auto; padding: 20px 28px 28px; background: #f5f5f5; scroll-behavior: auto; }
.message-row { display: flex; align-items: flex-start; gap: 11px; margin-bottom: 22px; }
.message-row.own { flex-direction: row-reverse; }
.sender-avatar { flex-shrink: 0; display: grid; place-items: center; width: 36px; height: 36px; color: #fff; font-size: 15px; border-radius: 5px; }
.message-body { min-width: 0; max-width: min(75%, 580px); }
.own .message-body { display: flex; align-items: flex-end; flex-direction: column; }
.sender-name { margin: -2px 0 6px; color: #969696; font-size: 12px; line-height: 1.3; }
.message-bubble { position: relative; width: fit-content; max-width: 100%; padding: 10px 13px; border: 1px solid #eaeaea; border-radius: 4px; color: #222; background: #fff; font-size: 14px; line-height: 1.7; white-space: pre-wrap; overflow-wrap: anywhere; text-align: left; user-select: text; }
.message-bubble::before { content: ''; position: absolute; top: 13px; left: -5px; width: 8px; height: 8px; background: inherit; border-left: 1px solid #eaeaea; border-bottom: 1px solid #eaeaea; transform: rotate(45deg); }
.own .message-bubble { background: #95ec69; border-color: #89df60; }
.own .message-bubble::before { left: auto; right: -5px; border: 0; border-top: 1px solid #89df60; border-right: 1px solid #89df60; }
.time-separator { margin: 9px 0 24px; text-align: center; color: #adadad; font-size: 12px; }
.delivery { display: flex; align-items: center; justify-content: flex-end; gap: 4px; margin-top: 7px; color: #a4aaa1; font-size: 12px; }
.delivery.failed { color: #df6363; }
.delivery button { border: 0; padding: 0 0 0 4px; background: none; color: #637d9d; font-size: 12px; cursor: pointer; }
.delivery button:disabled { opacity: .5; cursor: default; }
.sync-label { display: flex; align-items: center; justify-content: center; gap: 6px; padding: 6px 0 20px; color: #999; font-size: 12px; }
.welcome-state, .empty-chat { min-height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; }
.welcome-icon { display: grid; place-items: center; width: 80px; height: 80px; border-radius: 24px; background: #e8eee9; color: #83a38d; font-size: 42px; }
.welcome-state h3 { margin-top: 23px; color: #777; font-size: 17px; font-weight: 400; letter-spacing: 2px; }
.welcome-state p, .empty-chat p { margin-top: 13px; color: #a8a8a8; font-size: 12px; }
.empty-chat > .el-icon { color: #c8ceca; font-size: 38px; }
.empty-chat span { margin-top: 8px; color: #bbb; font-size: 12px; }
.spinning { animation: spin 1.2s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) { .spinning { animation: none; } }
@media (max-width: 900px) { .message-scroll { padding: 18px; } .message-body { max-width: 82%; } }
</style>
