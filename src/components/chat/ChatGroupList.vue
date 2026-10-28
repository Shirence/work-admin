<script>
import { avatarColor, timeLabel } from './chatPresentation'
export default {
  name: 'ChatGroupList',
  props: {
    groups: { type: Array, required: true }, messages: { type: Array, required: true },
    chosen: { type: [Number, String], default: null }, busy: Boolean, hasMore: Boolean,
    ready: Boolean, status: { type: String, default: '' }
  },
  emits: ['choose', 'create', 'refresh', 'load-more'],
  data: () => ({ keyword: '' }),
  computed: {
    filtered() {
      const keyword = this.keyword.trim().toLowerCase()
      return this.groups.filter(group => group.name.toLowerCase().includes(keyword))
    },
    latest() {
      return this.messages.reduce((result, message) => {
        const previous = result[message.groupId]
        if (!previous || (message.sentAt || '') >= (previous.sentAt || '')) result[message.groupId] = message
        return result
      }, {})
    }
  },
  methods: { avatarColor, timeLabel }
}
</script>

<template>
  <aside class="group-sidebar" aria-label="群聊列表">
    <div class="sidebar-heading"><h2>群聊</h2><span>{{ groups.length }} 个群组</span></div>
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索群聊" clearable aria-label="搜索群聊"><template #prefix><el-icon><Search /></el-icon></template></el-input>
      <button class="square-button" type="button" title="创建群组" aria-label="创建群组" :disabled="busy" @click="$emit('create')"><el-icon><Plus /></el-icon></button>
    </div>
    <div class="list-caption"><span>全部群聊</span><button type="button" class="refresh-button" :disabled="busy" title="刷新群组" aria-label="刷新群组" @click="$emit('refresh')"><el-icon :class="{ spinning: busy }"><Refresh /></el-icon></button></div>
    <div v-loading="busy && !groups.length" class="group-scroll">
      <button v-for="group in filtered" :key="group.id" type="button" class="group-row" :class="{ active: chosen === group.id }" :aria-current="chosen === group.id ? 'true' : undefined" @click="$emit('choose', group.id)">
        <span class="group-avatar" :style="{ backgroundColor: avatarColor(group.id) }" aria-hidden="true"><el-icon v-for="tile in 4" :key="tile"><UserFilled /></el-icon></span>
        <span class="group-copy">
          <span class="group-line"><span class="group-name">{{ group.name }}</span><time v-if="latest[group.id]">{{ timeLabel(latest[group.id].sentAt, true) }}</time></span>
          <span class="group-preview">{{ latest[group.id] ? `${latest[group.id].senderName || '成员'}：${latest[group.id].content}` : group.joined ? '点击查看聊天记录' : '点击加入群聊' }}</span>
        </span>
        <span v-if="!group.joined" class="join-mark">加入</span>
      </button>
      <div v-if="!filtered.length && !busy" class="list-empty"><el-icon><ChatDotRound /></el-icon><p>{{ keyword ? '没有找到相关群聊' : '还没有群聊' }}</p><small>{{ keyword ? '试试其他关键词' : '点击上方 + 创建一个群组' }}</small></div>
      <button v-if="hasMore" class="load-more" type="button" :disabled="busy" @click="$emit('load-more')">{{ busy ? '加载中…' : '加载更多群聊' }}</button>
    </div>
    <div class="connection"><i :class="{ online: ready }"></i><span>{{ status }}</span></div>
  </aside>
</template>

<style scoped>
.group-sidebar { display: flex; flex-direction: column; min-height: 0; background: #ededed; border-right: 1px solid #dedede; }
.sidebar-heading { display: flex; align-items: baseline; justify-content: space-between; padding: 23px 20px 17px; }
.sidebar-heading h2 { color: #222; font-size: 19px; font-weight: 600; }
.sidebar-heading > span { color: #a0a0a0; font-size: 12px; }
.search-bar { display: flex; align-items: center; gap: 9px; padding: 0 16px 15px; }
.search-bar :deep(.el-input__wrapper) { background: #dfdfdf; box-shadow: none; border-radius: 5px; font-size: 12px; }
button { font: inherit; cursor: pointer; }
button:disabled { cursor: default; opacity: .5; }
button:focus-visible { outline: 2px solid #07c160; outline-offset: -2px; }
.square-button { display: grid; place-items: center; width: 31px; height: 31px; flex-shrink: 0; border: 0; border-radius: 4px; background: #dfdfdf; color: #555; font-size: 18px; }
.square-button:hover { background: #d5d5d5; }
.list-caption { display: flex; align-items: center; justify-content: space-between; padding: 0 19px 9px; color: #999; font-size: 12px; }
.refresh-button { display: grid; place-items: center; padding: 3px; border: 0; background: none; color: #8b8b8b; font-size: 14px; }
.group-scroll { flex: 1; overflow-y: auto; min-height: 0; }
.group-row { position: relative; display: flex; align-items: center; gap: 12px; width: 100%; padding: 16px; border: 0; background: transparent; text-align: left; }
.group-row:hover { background: #e2e2e2; }
.group-row.active { background: #d6d6d6; }
.group-avatar { display: grid; grid-template-columns: 1fr 1fr; gap: 2px; flex-shrink: 0; width: 42px; height: 42px; padding: 5px; border-radius: 5px; color: #fff; }
.group-avatar .el-icon { width: 100%; height: 100%; padding: 2px; background: #ffffff26; border-radius: 2px; font-size: 12px; }
.group-copy { min-width: 0; flex: 1; }
.group-line { display: flex; align-items: center; gap: 8px; }
.group-name { flex: 1; color: #252525; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.group-line time { flex-shrink: 0; color: #999; font-size: 12px; }
.group-preview { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 6px; color: #999; font-size: 12px; line-height: 1.4; }
.join-mark { color: #75917e; font-size: 12px; }
.list-empty { padding: 55px 12px; text-align: center; color: #aaa; font-size: 12px; }
.list-empty .el-icon { font-size: 30px; color: #bbb; }
.list-empty p { margin: 12px 0 7px; }
.list-empty small { font-size: 12px; }
.load-more { display: block; width: 100%; padding: 14px; border: 0; color: #888; background: transparent; font-size: 12px; }
.connection { display: flex; align-items: center; gap: 7px; padding: 13px 18px; color: #929292; font-size: 12px; border-top: 1px solid #e2e2e2; }
.connection i { width: 6px; height: 6px; border-radius: 50%; background: #dfae62; }
.connection i.online { background: #07c160; }
.spinning { animation: spin 1.2s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) { .spinning { animation: none; } }
</style>
