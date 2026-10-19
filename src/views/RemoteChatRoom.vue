<template>
  <div class="remote-app mesh noise">
    <header class="remote-top glass">
      <div class="brand">
        <div class="logo">S</div>
        <div class="brand-text">
          <strong>STU REMOTE CHAT</strong>
          <small class="u-label">远程 MySQL 群聊</small>
        </div>
      </div>

      <div class="remote-user">
        <span class="remote-badge mono">REMOTE</span>
        <div class="who">
          <strong>{{ displayName }}</strong>
          <small class="u-label">@{{ account }}</small>
        </div>
        <ThemeToggle />
        <div class="source-switch" role="group" aria-label="数据源">
          <button type="button" class="source-btn" @click="goLocalChat">本地 H2</button>
          <button type="button" class="source-btn active" disabled>远程 MySQL</button>
        </div>
        <button class="btn-ghost mono" type="button" @click="handleLogout">EXIT</button>
      </div>
    </header>

    <main class="remote-main">
      <aside class="panel group-panel">
        <div class="panel-head">
          <div>
            <strong>群聊列表</strong>
            <span class="u-label">{{ groups.length }} groups</span>
          </div>
          <el-button size="small" type="primary" plain @click="openCreate">
            <el-icon class="btn-icon"><Plus /></el-icon>新建
          </el-button>
        </div>

        <div class="search-row">
          <el-input v-model="groupKeyword" placeholder="搜索群名 / 群主" clearable>
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div v-loading="groupsLoading" class="group-list">
          <button
            v-for="g in filteredGroups"
            :key="g.id"
            type="button"
            class="group-item"
            :class="{ active: g.id === selectedGroupId }"
            @click="selectGroup(g)"
          >
            <span class="group-avatar">{{ (g.name || '#').charAt(0) }}</span>
            <span class="group-main">
              <span class="group-title">
                <strong>{{ g.name }}</strong>
                <span class="mono dim">#{{ g.id }}</span>
              </span>
              <span class="group-sub">
                {{ g.memberCount }} 人 · 群主 {{ g.ownerName || g.ownerAccount || '—' }}
              </span>
            </span>
            <span class="join-chip" :class="g.joined ? 'joined' : 'not-joined'">
              {{ g.joined ? '已加入' : '未加入' }}
            </span>
          </button>
          <el-empty v-if="!filteredGroups.length" description="没有匹配的群聊" :image-size="64" />
        </div>
      </aside>

      <section class="panel chat-panel">
        <template v-if="selectedGroup">
          <div class="chat-head">
            <div class="chat-title">
              <h3>{{ selectedGroup.name }}</h3>
              <p class="u-hint">
                #{{ selectedGroup.id }} · {{ selectedGroup.memberCount }} 人 · 群主
                {{ selectedGroup.ownerName || selectedGroup.ownerAccount || '—' }}
              </p>
            </div>
            <div class="chat-actions">
              <span v-if="selectedGroup.joined" class="joined-dot">
                <span class="dot"></span>已加入
              </span>
              <el-button v-else type="primary" :loading="joining" @click="joinSelected">加入群聊</el-button>
              <el-button v-if="selectedGroup.joined" :loading="messagesLoading" @click="loadMessages(true)">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </div>

          <div v-if="!selectedGroup.joined" class="join-state">
            <el-empty description="加入群聊后才能查看历史和发送消息" :image-size="88">
              <el-button type="primary" :loading="joining" @click="joinSelected">加入群聊</el-button>
            </el-empty>
            <p class="u-hint join-hint">首次加入可读取入群前最近 30 条消息，以及入群后的全部消息。</p>
          </div>

          <template v-else>
            <div ref="messageList" v-loading="messagesLoading" class="message-list">
              <article
                v-for="m in messages"
                :key="m.id"
                class="message-item"
                :class="{ mine: isMine(m) }"
              >
                <span class="msg-avatar">{{ (m.senderName || '?').charAt(0) }}</span>
                <div class="msg-body">
                  <div class="msg-meta">
                    <strong>{{ isMine(m) ? '我' : (m.senderName || '未知用户') }}</strong>
                    <span class="mono dim">#{{ m.senderId }}</span>
                    <time class="mono">{{ formatTime(m.sentAt) }}</time>
                  </div>
                  <p class="msg-content">{{ m.content }}</p>
                </div>
              </article>
              <el-empty v-if="!messages.length" description="还没有消息，说点什么吧" :image-size="72" />
            </div>

            <div class="composer">
              <el-input
                v-model="draft"
                type="textarea"
                :rows="2"
                maxlength="2000"
                show-word-limit
                resize="none"
                placeholder="输入消息，Enter 发送，Shift + Enter 换行"
                @keydown.enter.exact.prevent="send"
              />
              <el-button
                type="primary"
                class="send-btn"
                :loading="sending"
                :disabled="!draft.trim()"
                @click="send"
              >
                发送
              </el-button>
            </div>
          </template>
        </template>

        <div v-else class="empty-state">
          <el-empty description="选择左侧群聊开始" :image-size="96" />
        </div>
      </section>
    </main>

    <el-dialog v-model="createVisible" title="新建远程群聊" width="420px">
      <div class="dialog-body">
        <label class="u-label">群名称</label>
        <el-input v-model="newGroupName" maxlength="100" show-word-limit placeholder="例如：项目讨论组" />
        <p class="u-hint">群会直接写入远程 MySQL，并自动加入创建者。</p>
      </div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">创建并进入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import {
  remoteMe,
  remoteLogout,
  listRemoteGroups,
  createRemoteGroup,
  joinRemoteGroup,
  getRemoteChatMessages,
  sendRemoteChatMessage
} from '../api/remoteChat'
import ThemeToggle from '../components/ThemeToggle.vue'

export default {
  name: 'RemoteChatRoom',
  components: { ThemeToggle },
  data() {
    return {
      userInfo: {},
      groups: [],
      selectedGroupId: null,
      messages: [],
      afterId: 0,
      groupKeyword: '',
      groupsLoading: false,
      messagesLoading: false,
      joining: false,
      sending: false,
      draft: '',
      createVisible: false,
      newGroupName: '',
      creating: false,
      pollTimer: null
    }
  },
  computed: {
    account() {
      return this.userInfo.username || ''
    },
    displayName() {
      return this.userInfo.displayName || this.userInfo.username || '远程用户'
    },
    remoteUserId() {
      return this.userInfo.remoteUserId
    },
    selectedGroup() {
      return this.groups.find(g => g.id === this.selectedGroupId) || null
    },
    filteredGroups() {
      const keyword = this.groupKeyword.trim().toLowerCase()
      if (!keyword) return this.groups
      return this.groups.filter(g => {
        return [g.name, g.ownerName, g.ownerAccount]
          .filter(Boolean)
          .some(value => String(value).toLowerCase().includes(keyword))
      })
    }
  },
  created() {
    const raw = localStorage.getItem('remoteUserInfo')
    if (raw) {
      try {
        this.userInfo = JSON.parse(raw)
      } catch (e) {
        localStorage.removeItem('remoteUserInfo')
      }
    }
    if (!localStorage.getItem('remoteToken')) {
      this.$router.replace('/remote-login')
      return
    }
    this.bootstrap()
  },
  beforeUnmount() {
    this.stopPolling()
  },
  methods: {
    async bootstrap() {
      try {
        const me = await remoteMe()
        if (!me.data || me.data.authSource !== 'REMOTE') {
          this.clearSession()
          this.$router.replace('/remote-login')
          return
        }
        this.userInfo = me.data
        localStorage.setItem('remoteUserInfo', JSON.stringify(me.data))
        await this.loadGroups()
        this.startPolling()
      } catch (e) {
        this.$message.error(e.message || '远程登录态无效')
      }
    },

    async loadGroups() {
      this.groupsLoading = true
      try {
        const res = await listRemoteGroups({ limit: 200 })
        this.groups = res.data || []
        let target = this.groups.find(g => g.id === this.selectedGroupId)
        if (!target) target = this.groups.find(g => g.joined) || this.groups[0] || null
        if (target) {
          await this.selectGroup(target, true)
        } else {
          this.selectedGroupId = null
          this.messages = []
          this.afterId = 0
        }
      } catch (e) {
        this.$message.error(e.message || '群聊列表加载失败')
      } finally {
        this.groupsLoading = false
      }
    },

    async selectGroup(group, force = false) {
      if (!group) return
      if (!force && this.selectedGroupId === group.id) return
      this.selectedGroupId = group.id
      this.messages = []
      this.afterId = 0
      this.draft = ''
      if (group.joined) {
        await this.loadMessages(true)
      }
    },

    async loadMessages(reset, silent = false) {
      const group = this.selectedGroup
      if (!group || !group.joined) return
      if (reset) this.messagesLoading = true
      try {
        const afterId = reset ? 0 : this.afterId
        const res = await getRemoteChatMessages(group.id, { afterId, limit: 100 })
        const page = res.data || {}
        const items = page.items || []
        if (reset) {
          this.messages = items
        } else if (items.length) {
          const existing = new Set(this.messages.map(m => m.id))
          this.messages = this.messages.concat(items.filter(m => !existing.has(m.id)))
        }
        this.afterId = page.nextAfterId || (this.messages.length ? this.messages[this.messages.length - 1].id : 0)
        if (items.length) this.scrollToBottom(reset)
      } catch (e) {
        if (!silent) this.$message.error(e.message || '消息加载失败')
      } finally {
        if (reset) this.messagesLoading = false
      }
    },

    async joinSelected() {
      const group = this.selectedGroup
      if (!group) return
      this.joining = true
      try {
        await joinRemoteGroup(group.id)
        this.$message.success('已加入群聊')
        await this.loadGroups()
      } catch (e) {
        this.$message.error(e.message || '加入群聊失败')
      } finally {
        this.joining = false
      }
    },

    async send() {
      const group = this.selectedGroup
      const content = this.draft
      if (!group || !group.joined || this.sending) return
      if (!content.trim()) {
        this.$message.warning('消息不能为空')
        return
      }
      this.sending = true
      try {
        const clientMessageId = `wa-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
        const res = await sendRemoteChatMessage(group.id, { content, clientMessageId })
        const saved = res.data
        if (saved && !this.messages.some(m => m.id === saved.id)) {
          this.messages = this.messages.concat(saved)
          this.afterId = saved.id
        }
        this.draft = ''
        this.scrollToBottom(true)
      } catch (e) {
        this.$message.error(e.message || '发送失败')
      } finally {
        this.sending = false
      }
    },

    openCreate() {
      this.newGroupName = ''
      this.createVisible = true
    },

    async submitCreate() {
      const name = this.newGroupName.trim()
      if (!name) {
        this.$message.warning('请输入群名称')
        return
      }
      this.creating = true
      try {
        const res = await createRemoteGroup({ name })
        this.createVisible = false
        this.$message.success('群聊创建成功')
        this.selectedGroupId = res.data.id
        await this.loadGroups()
      } catch (e) {
        this.$message.error(e.message || '创建群聊失败')
      } finally {
        this.creating = false
      }
    },

    async handleLogout() {
      try {
        await remoteLogout()
      } catch (e) {
        // 退出接口失败也清本地登录态
      }
      this.clearSession()
      this.$router.replace('/remote-login')
    },

    goLocalChat() {
      if (localStorage.getItem('token')) {
        this.$router.push('/chat')
      } else {
        this.$router.push('/login')
      }
    },

    clearSession() {
      localStorage.removeItem('remoteToken')
      localStorage.removeItem('remoteUserInfo')
    },

    isMine(message) {
      return this.remoteUserId !== null && this.remoteUserId !== undefined
        && Number(message.senderId) === Number(this.remoteUserId)
    },

    formatTime(value) {
      if (!value) return '—'
      const parsed = dayjs(value)
      return parsed.isValid() ? parsed.format('MM-DD HH:mm:ss') : String(value)
    },

    startPolling() {
      this.stopPolling()
      this.pollTimer = setInterval(() => {
        const group = this.selectedGroup
        if (!group || !group.joined || this.sending || this.messagesLoading) return
        this.loadMessages(false, true)
      }, 3000)
    },

    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },

    scrollToBottom(force) {
      this.$nextTick(() => {
        const el = this.$refs.messageList
        if (!el) return
        const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 90
        if (force || nearBottom) {
          el.scrollTop = el.scrollHeight
        }
      })
    }
  }
}
</script>

<style scoped>
.remote-app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

.remote-top {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 64px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
  border-bottom: 1px solid var(--line);
}
.brand { display: flex; align-items: center; gap: 10px; }
.logo {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(150deg, var(--violet), var(--accent));
  color: var(--on-remote);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  box-shadow: 0 0 24px -6px color-mix(in srgb, var(--violet) 55%, transparent);
}
.brand-text { display: flex; flex-direction: column; line-height: 1.3; }
.brand-text strong { font-size: 12.5px; letter-spacing: .05em; }
.brand-text small { font-size: 10px; color: var(--text-3); }

.remote-user { display: flex; align-items: center; gap: 14px; }
.remote-badge {
  font-size: 9.5px;
  letter-spacing: .12em;
  color: var(--violet);
  background: color-mix(in srgb, var(--violet) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--violet) 25%, transparent);
  border-radius: 999px;
  padding: 3px 9px;
}
.who { display: flex; flex-direction: column; line-height: 1.25; }
.who strong { font-size: 12.5px; }
.btn-ghost {
  font-size: 10.5px;
  letter-spacing: .1em;
  color: var(--text-2);
  background: transparent;
  border: 1px solid var(--line-2);
  border-radius: var(--r-sm);
  padding: 6px 11px;
  cursor: pointer;
  transition: all var(--dur-1) var(--ease-2);
}
.btn-ghost:hover { color: var(--danger); border-color: rgba(229, 72, 77, .4); background: rgba(229, 72, 77, .06); }
.source-switch {
  display: flex;
  gap: 2px;
  padding: 2px;
  background: var(--bg-inset);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
}
.source-btn {
  border: 0;
  background: transparent;
  color: var(--text-3);
  font-size: 11px;
  padding: 5px 9px;
  border-radius: 5px;
  cursor: pointer;
  white-space: nowrap;
}
.source-btn:hover:not(:disabled) { color: var(--text); }
.source-btn.active {
  color: var(--violet);
  background: var(--surface);
  box-shadow: var(--shadow-xs);
  font-weight: 600;
}
.source-btn:disabled { cursor: default; }
.local-link { font-size: 11px; color: var(--text-3); text-decoration: none; }
.local-link:hover { color: var(--accent); }

.remote-main {
  flex: 1;
  min-height: 0;
  width: 100%;
  max-width: 1500px;
  margin: 0 auto;
  padding: 18px;
  display: grid;
  grid-template-columns: minmax(270px, 320px) minmax(0, 1fr);
  gap: 16px;
  height: calc(100vh - 64px);
}

.panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 15px;
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
}
.panel-head strong { font-size: 13px; }
.panel-head .u-label { display: block; margin-top: 2px; }
.btn-icon { margin-right: 3px; }

.search-row { padding: 10px 12px; border-bottom: 1px solid var(--line); }
.group-list { flex: 1; overflow-y: auto; padding: 6px; }
.group-item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  text-align: left;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: var(--r-md);
  background: transparent;
  color: inherit;
  cursor: pointer;
  transition: background var(--dur-1) var(--ease-2), border-color var(--dur-1) var(--ease-2);
}
.group-item:hover { background: var(--bg-inset); }
.group-item.active { background: color-mix(in srgb, var(--violet) 7%, transparent); border-color: color-mix(in srgb, var(--violet) 22%, transparent); }
.group-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 9px;
  background: var(--bg-inset);
  color: var(--text-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 650;
}
.group-item.active .group-avatar { background: var(--violet); color: var(--on-remote); }
.group-main { display: flex; flex-direction: column; gap: 2px; min-width: 0; flex: 1; }
.group-title { display: flex; align-items: center; gap: 6px; min-width: 0; }
.group-title strong { font-size: 12.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.group-sub { font-size: 10.5px; color: var(--text-3); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.join-chip {
  font-size: 9.5px;
  padding: 2px 7px;
  border-radius: 999px;
  white-space: nowrap;
  flex-shrink: 0;
}
.join-chip.joined { color: var(--ok); background: rgba(23, 166, 115, .08); border: 1px solid rgba(23, 166, 115, .2); }
.join-chip.not-joined { color: var(--text-3); background: var(--bg-inset); border: 1px solid var(--line-2); }

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
}
.chat-title h3 { font-size: 15px; font-weight: 650; margin-bottom: 3px; }
.chat-actions { display: flex; align-items: center; gap: 10px; }
.joined-dot { display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--ok); }
.dot { width: 7px; height: 7px; border-radius: 50%; background: var(--ok); box-shadow: 0 0 0 3px rgba(23, 166, 115, .14); }

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.message-item { display: flex; gap: 10px; max-width: 78%; }
.message-item.mine { align-self: flex-end; flex-direction: row-reverse; }
.msg-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 9px;
  background: var(--bg-inset);
  border: 1px solid var(--line-2);
  color: var(--text-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 650;
}
.message-item.mine .msg-avatar { background: color-mix(in srgb, var(--violet) 12%, transparent); border-color: color-mix(in srgb, var(--violet) 22%, transparent); color: var(--violet); }
.msg-body { min-width: 0; }
.msg-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.msg-meta strong { font-size: 12px; }
.msg-meta time { font-size: 9.5px; color: var(--text-3); margin-left: auto; }
.message-item.mine .msg-meta { flex-direction: row-reverse; }
.message-item.mine .msg-meta time { margin-left: 0; margin-right: auto; }
.msg-content {
  margin: 0;
  padding: 9px 12px;
  background: var(--bg-sub);
  border: 1px solid var(--line);
  border-radius: 2px 10px 10px 10px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--text);
  white-space: pre-wrap;
  word-break: break-word;
}
.message-item.mine .msg-content {
  background: color-mix(in srgb, var(--violet) 9%, transparent);
  border-color: color-mix(in srgb, var(--violet) 20%, transparent);
  border-radius: 10px 2px 10px 10px;
}

.composer {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 12px 14px;
  border-top: 1px solid var(--line);
  flex-shrink: 0;
}
.composer :deep(.el-textarea__inner) { border-radius: var(--r-md); }
.send-btn { height: 54px; width: 84px; }

.join-state { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.join-hint { margin-top: -14px; }
.empty-state { flex: 1; display: flex; align-items: center; justify-content: center; }
.dialog-body { display: flex; flex-direction: column; gap: 8px; }
.dialog-body .u-hint { margin-top: 2px; }
.dim { color: var(--text-3); }

@media (max-width: 900px) {
  .remote-main { grid-template-columns: 1fr; height: auto; min-height: calc(100vh - 64px); }
  .group-panel { max-height: 300px; }
  .chat-panel { min-height: 560px; }
  .remote-user .local-link { display: none; }
  .message-item { max-width: 92%; }
}
</style>
