<template>
  <div class="page chat-page">
    <header class="page-head">
      <div>
        <h2>群聊</h2>
      </div>
      <div class="head-actions">
        <span class="u-label">{{ groups.length }} groups</span>
        <el-button size="small" :loading="groupsLoading" @click="loadGroups()">刷新</el-button>
        <el-button size="small" type="primary" plain @click="openCreate">新建群</el-button>
      </div>
    </header>

    <section class="chat-shell">
      <aside class="panel conv-panel">
        <div class="panel-head">
          <strong>群组</strong>
          <span class="u-label">{{ filteredGroups.length }}</span>
        </div>
        <div class="conv-search">
          <el-input v-model="keyword" size="small" placeholder="搜索群名称" clearable />
        </div>
        <div v-loading="groupsLoading" class="conv-list">
          <button
            v-for="g in filteredGroups"
            :key="g.id"
            type="button"
            class="conv-item"
            :class="{ active: g.id === selectedId }"
            @click="select(g)"
          >
            <span class="conv-avatar">{{ firstChar(g.name) }}</span>
            <span class="conv-main">
              <span class="conv-title">
                <strong>{{ g.name }}</strong>
                <time class="conv-time mono">{{ shortTime(g.lastAt || g.createdAt) }}</time>
              </span>
              <span class="conv-sub">
                <span class="conv-preview">{{ preview(g) }}</span>
                <span class="conv-count mono">{{ g.memberCount }} 人</span>
              </span>
            </span>
          </button>
          <el-empty v-if="!groupsLoading && !filteredGroups.length" description="还没有群，先新建一个" :image-size="60" />
        </div>
      </aside>

      <section class="panel msg-panel">
        <template v-if="selectedGroup">
          <div class="msg-head">
            <div class="msg-head-main">
              <h3>{{ selectedGroup.name }}</h3>
              <p class="u-hint">
                {{ selectedGroup.memberCount }} 位成员 · 群主 {{ selectedGroup.ownerName || '—' }}
              </p>
            </div>
            <div class="msg-head-actions">
              <el-button v-if="!selectedGroup.joined" size="small" type="primary" @click="joinSelected">加入群聊</el-button>
              <el-button size="small" @click="openMembers">成员 {{ members.length || selectedGroup.memberCount }}</el-button>
              <span class="live-chip mono"><span class="dot"></span>LIVE</span>
            </div>
          </div>

          <div ref="thread" class="thread" @scroll="onScroll">
            <div class="thread-top">
              <el-button v-if="hasMore" link size="small" :loading="loadingMore" @click="loadEarlier">加载更早的消息</el-button>
              <span v-else class="u-label">已到群聊起点</span>
            </div>

            <div v-loading="messagesLoading && !messages.length" class="thread-body">
              <div v-for="m in messages" :key="m.id" class="msg-row" :class="{ mine: m.mine }">
                <span class="msg-avatar">{{ firstChar(m.senderName) }}</span>
                <div class="msg-body">
                  <div class="msg-meta">
                    <strong>{{ m.mine ? '我' : m.senderName }}</strong>
                    <time class="mono dim">{{ clock(m.sentAt) }}</time>
                  </div>
                  <div class="bubble">{{ m.content }}</div>
                </div>
              </div>
              <el-empty
                v-if="!messagesLoading && !messages.length"
                description="这个群还没有消息，说点什么吧"
                :image-size="72"
              />
            </div>
          </div>

          <div class="composer">
            <el-input
              v-model="draft"
              type="textarea"
              :rows="2"
              resize="none"
              maxlength="2000"
              show-word-limit
              :placeholder="selectedGroup.joined ? '输入消息，Enter 发送，Shift + Enter 换行' : '先加入群聊才能发言'"
              :disabled="!selectedGroup.joined"
              @keydown.enter="onEnter"
            />
            <div class="composer-foot">
              <span class="u-hint">Enter 发送 · Shift + Enter 换行</span>
              <el-button type="primary" :loading="sending" :disabled="!canSend" @click="submit">发送</el-button>
            </div>
          </div>
        </template>
        <div v-else class="msg-blank">
          <el-empty description="从左侧选择一个群组" :image-size="80" />
        </div>
      </section>
    </section>

    <el-drawer v-model="membersVisible" title="群成员" size="320px">
      <div class="member-list">
        <div v-for="m in members" :key="m.id" class="member-item">
          <span class="conv-avatar small">{{ firstChar(m.name) }}</span>
          <span class="member-main">
            <strong>{{ m.name }}</strong>
            <span class="mono dim">{{ m.account }}</span>
          </span>
          <span v-if="m.owner" class="chip chip-accent">群主</span>
        </div>
        <el-empty v-if="!members.length" description="暂无成员" :image-size="60" />
      </div>
    </el-drawer>

    <el-dialog v-model="createVisible" title="新建群聊" width="480px" :close-on-click-modal="!creating">
      <el-form label-width="72px" @submit.prevent>
        <el-form-item label="群名称">
          <el-input v-model="createForm.name" maxlength="100" placeholder="例如：项目周会" />
        </el-form-item>
        <el-form-item label="成员">
          <el-select
            v-model="createForm.memberIds"
            multiple
            filterable
            placeholder="选择要拉进群的人（可留空）"
            style="width: 100%"
          >
            <el-option v-for="u in candidates" :key="u.id" :label="`${u.name} · ${u.account}`" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="creating" @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" :disabled="!createForm.name.trim()" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import {
  listGroups,
  listCandidates,
  getGroup,
  listMessages,
  sendMessage,
  createGroup,
  joinGroup
} from '../api/groups'

const PAGE_SIZE = 50
const POLL_MS = 5000

export default {
  name: 'ChatView',
  data() {
    return {
      groups: [],
      groupsLoading: false,
      keyword: '',
      selectedId: null,
      members: [],
      membersVisible: false,
      messages: [],
      messagesLoading: false,
      loadingMore: false,
      hasMore: false,
      draft: '',
      sending: false,
      createVisible: false,
      creating: false,
      createForm: { name: '', memberIds: [] },
      candidates: [],
      pollTimer: null,
      stickBottom: true
    }
  },
  computed: {
    filteredGroups() {
      const kw = this.keyword.trim().toLowerCase()
      if (!kw) return this.groups
      return this.groups.filter(g => String(g.name || '').toLowerCase().includes(kw))
    },
    selectedGroup() {
      return this.groups.find(g => g.id === this.selectedId) || null
    },
    canSend() {
      return Boolean(this.selectedGroup && this.selectedGroup.joined && this.draft.trim()) && !this.sending
    }
  },
  mounted() {
    this.loadGroups()
    // 轻量轮询：群列表变化 + 当前群新消息，不引入 WebSocket 也能做到近似实时
    this.pollTimer = setInterval(this.poll, POLL_MS)
  },
  beforeUnmount() {
    if (this.pollTimer) clearInterval(this.pollTimer)
    this.pollTimer = null
  },
  methods: {
    async loadGroups(silent = false) {
      if (!silent) this.groupsLoading = true
      try {
        const res = await listGroups()
        const rows = res.data || []
        const prevSelected = this.selectedId
        this.groups = rows
        if (!rows.length) {
          this.selectedId = null
          this.messages = []
          return
        }
        const keep = prevSelected && rows.some(g => g.id === prevSelected)
        if (!keep) {
          this.select(rows[0])
        }
      } catch (error) {
        if (!silent) ElMessage.error(error.message || '群列表加载失败')
      } finally {
        this.groupsLoading = false
      }
    },

    select(group) {
      if (!group) return
      if (this.selectedId === group.id && this.messages.length) return
      this.selectedId = group.id
      this.messages = []
      this.hasMore = false
      this.stickBottom = true
      this.loadThread()
      this.loadMembers()
    },

    async loadThread() {
      if (!this.selectedId) return
      const groupId = this.selectedId
      this.messagesLoading = true
      try {
        const res = await listMessages(groupId, { limit: PAGE_SIZE })
        if (groupId !== this.selectedId) return
        const rows = res.data || []
        this.messages = rows
        this.hasMore = rows.length >= PAGE_SIZE
        this.scrollToBottom()
      } catch (error) {
        ElMessage.error(error.message || '消息加载失败')
      } finally {
        this.messagesLoading = false
      }
    },

    async loadEarlier() {
      if (!this.messages.length || this.loadingMore) return
      const groupId = this.selectedId
      const beforeId = this.messages[0].id
      const thread = this.$refs.thread
      const prevHeight = thread ? thread.scrollHeight : 0
      this.loadingMore = true
      try {
        const res = await listMessages(groupId, { beforeId, limit: PAGE_SIZE })
        if (groupId !== this.selectedId) return
        const rows = res.data || []
        this.hasMore = rows.length >= PAGE_SIZE
        const known = new Set(this.messages.map(m => m.id))
        const older = rows.filter(m => !known.has(m.id))
        this.messages = older.concat(this.messages)
        this.$nextTick(() => {
          if (thread) thread.scrollTop = thread.scrollHeight - prevHeight
        })
      } catch (error) {
        ElMessage.error(error.message || '更早的消息加载失败')
      } finally {
        this.loadingMore = false
      }
    },

    async loadMembers() {
      if (!this.selectedId) return
      try {
        const res = await getGroup(this.selectedId)
        const group = res.data && res.data.group
        this.members = (res.data && res.data.members) || []
        if (group) {
          this.groups = this.groups.map(g => (g.id === group.id ? { ...g, ...group } : g))
        }
      } catch (error) {
        // 不在群里时成员列表为空，不打扰用户
        this.members = []
      }
    },

    async poll() {
      if (!this.selectedId) return
      const groupId = this.selectedId
      try {
        const res = await listMessages(groupId, { limit: PAGE_SIZE })
        if (groupId !== this.selectedId) return
        const rows = res.data || []
        const latestLocal = this.messages.length ? this.messages[this.messages.length - 1].id : 0
        const latestRemote = rows.length ? rows[rows.length - 1].id : 0
        if (latestRemote > latestLocal || rows.length !== this.messages.length) {
          const byId = new Map(this.messages.map(m => [m.id, m]))
          rows.forEach(m => byId.set(m.id, m))
          this.messages = Array.from(byId.values()).sort((a, b) => a.id - b.id)
          if (this.stickBottom) this.scrollToBottom()
        }
      } catch (error) {
        // 轮询失败静默重试，避免弹窗打断
      }
      this.loadGroups(true)
    },

    onScroll() {
      const thread = this.$refs.thread
      if (!thread) return
      this.stickBottom = thread.scrollHeight - thread.scrollTop - thread.clientHeight < 90
    },

    scrollToBottom() {
      this.$nextTick(() => {
        const thread = this.$refs.thread
        if (thread) thread.scrollTop = thread.scrollHeight
        this.stickBottom = true
      })
    },

    onEnter(event) {
      if (event.shiftKey) return
      event.preventDefault()
      this.submit()
    },

    async submit() {
      if (!this.canSend) return
      const content = this.draft.trim()
      const groupId = this.selectedId
      this.sending = true
      try {
        const res = await sendMessage(groupId, {
          content,
          clientMessageId: `wa_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
        })
        this.draft = ''
        if (groupId === this.selectedId) {
          const saved = res.data
          if (saved && !this.messages.some(m => m.id === saved.id)) {
            this.messages = this.messages.concat(saved)
          }
          this.scrollToBottom()
        }
        this.loadGroups(true)
      } catch (error) {
        ElMessage.error(error.message || '发送失败')
      } finally {
        this.sending = false
      }
    },

    async joinSelected() {
      if (!this.selectedGroup) return
      try {
        const res = await joinGroup(this.selectedGroup.id)
        const detail = res.data || {}
        this.members = detail.members || []
        if (detail.group) {
          this.groups = this.groups.map(g => (g.id === detail.group.id ? { ...g, ...detail.group } : g))
        }
        ElMessage.success('已加入群聊')
        this.loadThread()
      } catch (error) {
        ElMessage.error(error.message || '加入失败')
      }
    },

    async openMembers() {
      this.membersVisible = true
      if (!this.members.length) this.loadMembers()
    },

    async openCreate() {
      this.createForm = { name: '', memberIds: [] }
      this.createVisible = true
      if (this.candidates.length) return
      try {
        const res = await listCandidates()
        this.candidates = res.data || []
      } catch (error) {
        ElMessage.error(error.message || '成员列表加载失败')
      }
    },

    async submitCreate() {
      if (!this.createForm.name.trim() || this.creating) return
      this.creating = true
      try {
        const res = await createGroup({
          name: this.createForm.name.trim(),
          memberIds: this.createForm.memberIds
        })
        const detail = res.data || {}
        this.createVisible = false
        ElMessage.success(res.msg || '群已创建')
        await this.loadGroups(true)
        if (detail.group) {
          this.selectedId = detail.group.id
          this.messages = []
          this.members = detail.members || []
          this.loadThread()
        }
      } catch (error) {
        ElMessage.error(error.message || '创建失败')
      } finally {
        this.creating = false
      }
    },

    firstChar(text) {
      const value = String(text || '?').trim()
      return value ? value.charAt(0).toUpperCase() : '?'
    },

    preview(group) {
      if (!group.lastMessage) return '暂无消息'
      const prefix = group.lastSender ? `${group.lastSender}：` : ''
      return prefix + group.lastMessage.replace(/\s+/g, ' ')
    },

    asDate(value) {
      if (!value) return null
      const date = new Date(value)
      return Number.isNaN(date.getTime()) ? null : date
    },

    clock(value) {
      const date = this.asDate(value)
      if (!date) return ''
      const pad = n => String(n).padStart(2, '0')
      return `${pad(date.getHours())}:${pad(date.getMinutes())}`
    },

    shortTime(value) {
      const date = this.asDate(value)
      if (!date) return ''
      const now = new Date()
      const pad = n => String(n).padStart(2, '0')
      if (date.toDateString() === now.toDateString()) return this.clock(value)
      return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    }
  }
}
</script>

<style scoped>
.chat-page { display: flex; flex-direction: column; gap: 18px; }
.head-actions { display: flex; align-items: center; gap: 10px; }

.chat-shell {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  height: calc(100vh - 190px);
  min-height: 520px;
}

.panel {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

/* ---------- 左：群列表 ---------- */
.conv-panel { display: flex; flex-direction: column; min-height: 0; }
.panel-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--line);
}
.panel-head strong { font-family: var(--display); font-size: 14px; letter-spacing: .04em; }
.conv-search { padding: 10px 12px; border-bottom: 1px solid var(--line); }
.conv-list { flex: 1; overflow-y: auto; padding: 6px; }

.conv-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  width: 100%;
  padding: 9px 10px;
  border: 1px solid transparent;
  border-radius: var(--r-md);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background .18s var(--ease), border-color .18s var(--ease);
}
.conv-item + .conv-item { margin-top: 2px; }
.conv-item:hover { background: var(--accent-soft); }
.conv-item.active { background: var(--accent-soft); border-color: var(--accent-line); }

.conv-avatar {
  display: grid; place-items: center;
  width: 34px; height: 34px;
  border-radius: var(--r-md);
  background: var(--bg-inset);
  border: 1px solid var(--line-2);
  font-family: var(--display);
  font-size: 14px;
  font-weight: 700;
  color: var(--accent);
}
.conv-avatar.small { width: 30px; height: 30px; font-size: 12px; }

.conv-main { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.conv-title { display: flex; align-items: baseline; gap: 8px; }
.conv-title strong {
  flex: 1; min-width: 0;
  font-size: 13px; font-weight: 600; color: var(--text);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.conv-time { flex-shrink: 0; font-size: 10px; color: var(--text-3); white-space: nowrap; }
.conv-sub { display: flex; align-items: baseline; gap: 8px; }
.conv-preview {
  flex: 1; min-width: 0;
  font-size: 11.5px; color: var(--text-3);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.conv-count { flex-shrink: 0; font-size: 10px; color: var(--text-3); white-space: nowrap; }

/* ---------- 右：消息 ---------- */
.msg-panel { display: flex; flex-direction: column; min-height: 0; }
.msg-head {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 13px 18px;
  border-bottom: 1px solid var(--line);
}
.msg-head-main { min-width: 0; }
.msg-head h3 {
  font-family: var(--display); font-size: 16px; font-weight: 700;
  letter-spacing: -.02em; margin: 0 0 2px;
}
.msg-head .u-hint { margin: 0; }
.msg-head-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.live-chip {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 10px; letter-spacing: .12em; color: var(--ok);
}
.live-chip .dot {
  width: 5px; height: 5px; border-radius: 50%; background: var(--ok);
  box-shadow: 0 0 0 3px rgba(10, 143, 120, .18);
}

.thread { flex: 1; overflow-y: auto; padding: 12px 18px 18px; min-height: 0; display: flex; flex-direction: column; }
.thread-top { display: flex; justify-content: center; padding: 2px 0 10px; }
.thread-body { display: flex; flex-direction: column; gap: 14px; margin-top: auto; }

.msg-row { display: flex; gap: 10px; align-items: flex-start; max-width: 76%; }
.msg-avatar {
  display: grid; place-items: center;
  width: 30px; height: 30px; flex-shrink: 0;
  border-radius: 50%;
  background: var(--bg-inset);
  border: 1px solid var(--line-2);
  font-size: 12px; font-weight: 700; color: var(--text-2);
}
.msg-body { min-width: 0; }
.msg-meta { display: flex; align-items: baseline; gap: 8px; margin-bottom: 4px; }
.msg-meta strong { font-size: 12px; font-weight: 600; color: var(--text-2); }
.msg-meta time { font-size: 10px; }

.bubble {
  padding: 9px 13px;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--text);
  font-size: 13.5px;
  line-height: 1.62;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: var(--shadow-xs);
}

.msg-row.mine { flex-direction: row-reverse; margin-left: auto; }
.msg-row.mine .msg-meta { justify-content: flex-end; }
.msg-row.mine .bubble {
  background: var(--accent-soft);
  border-color: var(--accent-line);
  color: var(--text);
}
.msg-row.mine .msg-avatar {
  background: var(--accent-soft);
  border-color: var(--accent-line);
  color: var(--accent);
}

.msg-blank { flex: 1; display: grid; place-items: center; }

/* ---------- 输入区 ---------- */
.composer {
  border-top: 1px solid var(--line);
  padding: 12px 16px 14px;
  background: var(--bg-sub);
}
.composer-foot {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; margin-top: 8px;
}
.composer-foot .u-hint { margin: 0; }

/* ---------- 成员抽屉 ---------- */
.member-list { display: flex; flex-direction: column; gap: 4px; }
.member-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 6px;
  border-bottom: 1px solid var(--line);
}
.member-main { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
.member-main strong { font-size: 13px; font-weight: 600; color: var(--text); }
.member-main .dim { font-size: 11px; }

@media (max-width: 1100px) {
  .chat-shell { grid-template-columns: 240px minmax(0, 1fr); }
}
@media (max-width: 900px) {
  .chat-shell { grid-template-columns: 1fr; height: auto; }
  .conv-panel { max-height: 260px; }
  .thread { max-height: 420px; }
}
</style>
