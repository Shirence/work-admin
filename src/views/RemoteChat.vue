<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h2>远程数据观察台</h2>
        <p class="u-hint">只读连接远程 MySQL，查看 stu 群聊库中的群组、成员与消息</p>
      </div>
      <div class="head-actions">
        <span class="readonly-badge mono">READ ONLY</span>
        <el-button :loading="loading" @click="refreshAll">
          <el-icon class="btn-icon"><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </header>

    <section v-if="error" class="alert">
      <el-icon class="alert-icon"><WarningFilled /></el-icon>
      <div>
        <strong>远程库暂不可用</strong>
        <p>{{ error }}</p>
        <p class="u-hint">H2 本地业务不受影响；请检查 server/application-local.yml 中的远程连接配置。</p>
      </div>
    </section>

    <section v-reveal class="metrics">
      <div class="metric">
        <span class="u-label">Groups</span>
        <strong>{{ displayCount(overview && overview.groupCount) }}</strong>
        <small>群组</small>
      </div>
      <div class="metric">
        <span class="u-label">Members</span>
        <strong>{{ displayCount(overview && overview.memberCount) }}</strong>
        <small>成员关系</small>
      </div>
      <div class="metric">
        <span class="u-label">Messages</span>
        <strong>{{ displayCount(overview && overview.messageCount) }}</strong>
        <small>消息</small>
      </div>
      <div class="metric">
        <span class="u-label">Users</span>
        <strong>{{ displayCount(overview && overview.userCount) }}</strong>
        <small>用户</small>
      </div>
    </section>

    <section v-reveal class="connection surface">
      <div class="conn-left">
        <span class="live-dot"></span>
        <div class="conn-text">
          <strong>{{ (overview && overview.database) || 'demo' }}</strong>
          <span class="mono">
            {{ (overview && overview.hostname) || '—' }} · MySQL {{ (overview && overview.version) || '—' }}
          </span>
        </div>
      </div>
      <div class="conn-right mono">
        <span>{{ overview ? overview.latencyMs + ' ms' : '—' }}</span>
        <span>{{ formatTime(overview && overview.fetchedAt) }}</span>
      </div>
    </section>

    <section class="workspace">
      <aside class="panel groups-panel">
        <div class="panel-head">
          <div class="tabs">
            <button type="button" class="tab" :class="{ active: tab === 'groups' }" @click="switchTab('groups')">
              群组
            </button>
            <button type="button" class="tab" :class="{ active: tab === 'users' }" @click="switchTab('users')">
              用户
            </button>
          </div>
          <span class="u-label">{{ tab === 'groups' ? groups.length : users.length }}</span>
        </div>

        <div class="search-row">
          <el-input
            v-model="keyword"
            :placeholder="tab === 'groups' ? '搜索群名 / 群主' : '搜索昵称 / 账号'"
            clearable
            @keyup.enter="loadList"
            @clear="loadList"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div v-loading="listLoading" class="list">
          <template v-if="tab === 'groups'">
            <button
              type="button"
              class="group-item all-item"
              :class="{ active: allGroupsView }"
              @click="selectAllGroups"
            >
              <span class="group-avatar">全</span>
              <span class="group-main">
                <span class="group-title"><strong>全部群聊</strong></span>
                <span class="group-sub">跨群查看最近消息</span>
              </span>
            </button>
            <button
              v-for="g in groups"
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
                  {{ g.memberCount }} 人 · {{ g.messageCount }} 条 · 群主 {{ g.ownerName || g.ownerAccount || '—' }}
                </span>
                <span class="group-time mono">{{ formatTime(g.lastMessageAt || g.createdAt) }}</span>
              </span>
            </button>
            <el-empty v-if="!groups.length" description="没有匹配的群组" :image-size="64" />
          </template>

          <template v-else>
            <div v-for="u in users" :key="u.id" class="user-item">
              <span class="group-avatar">{{ (u.name || u.account || '?').charAt(0) }}</span>
              <span class="group-main">
                <span class="group-title">
                  <strong>{{ u.name || u.account }}</strong>
                  <span class="role-chip">{{ u.roleName || '未分配' }}</span>
                </span>
                <span class="group-sub">{{ u.account }} · ID {{ u.id }}</span>
              </span>
            </div>
            <el-empty v-if="!users.length" description="没有匹配的用户" :image-size="64" />
          </template>
        </div>
      </aside>

      <section class="panel messages-panel">
        <div class="panel-head messages-head">
          <div class="messages-title">
            <h3>{{ selectedGroup ? selectedGroup.name : '跨群最近消息' }}</h3>
            <p v-if="selectedGroup" class="u-hint">
              {{ selectedGroup.memberCount }} 位成员 · {{ selectedGroup.messageCount }} 条消息 · 群主
              {{ selectedGroup.ownerName || selectedGroup.ownerAccount || '—' }}
            </p>
            <p v-else class="u-hint">未选择群组时显示所有群的最近消息</p>
          </div>

          <div class="message-tools">
            <el-input
              v-model="messageKeyword"
              placeholder="搜索消息 / 发送者"
              clearable
              class="message-search"
              @keyup.enter="loadMessages"
              @clear="loadMessages"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button :loading="messageLoading" @click="loadMessages">
              <el-icon><Search /></el-icon>
            </el-button>
          </div>
        </div>

        <div v-loading="messageLoading" class="message-list">
          <template v-if="messages.length">
            <article v-for="m in messages" :key="m.id" class="message-item">
              <span class="msg-avatar">{{ (m.senderName || m.senderAccount || '?').charAt(0) }}</span>
              <div class="msg-body">
                <div class="msg-meta">
                  <strong>{{ m.senderName || '未知用户' }}</strong>
                  <span class="mono dim">@{{ m.senderAccount || m.senderId }}</span>
                  <span v-if="messageKeyword || !selectedGroup" class="group-chip">#{{ m.groupName }}</span>
                  <time class="mono">{{ formatTime(m.sentAt) }}</time>
                </div>
                <p class="msg-content">{{ m.content }}</p>
                <div class="msg-foot mono">msg #{{ m.id }} · {{ m.clientMessageId }}</div>
              </div>
            </article>
          </template>
          <el-empty v-else description="暂无消息" :image-size="72" />
        </div>

        <div v-if="selectedGroup && hasMore && !messageKeyword" class="load-more">
          <el-button link type="primary" :loading="messageLoading" @click="loadMore">加载更多</el-button>
        </div>
      </section>
    </section>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import {
  getRemoteOverview,
  getRemoteGroups,
  getRemoteGroupMessages,
  getRemoteMessages,
  getRemoteUsers
} from '../api/remote'

export default {
  name: 'RemoteChat',
  data() {
    return {
      loading: false,
      listLoading: false,
      messageLoading: false,
      overview: null,
      groups: [],
      users: [],
      messages: [],
      selectedGroupId: null,
      allGroupsView: false,
      nextAfterId: 0,
      hasMore: false,
      keyword: '',
      messageKeyword: '',
      tab: 'groups',
      error: ''
    }
  },
  computed: {
    selectedGroup() {
      return this.groups.find(g => g.id === this.selectedGroupId) || null
    }
  },
  created() {
    this.refreshAll()
  },
  methods: {
    async refreshAll() {
      this.loading = true
      this.error = ''
      try {
        const [overview, groups, users] = await Promise.all([
          getRemoteOverview(),
          getRemoteGroups({ limit: 200 }),
          getRemoteUsers({ limit: 200 })
        ])
        this.overview = overview.data || null
        this.groups = groups.data || []
        this.users = users.data || []
        if (!this.allGroupsView && (!this.selectedGroupId || !this.selectedGroup)) {
          this.selectedGroupId = this.groups.length ? this.groups[0].id : null
        }
        this.nextAfterId = 0
        await this.loadMessages()
      } catch (e) {
        this.error = e.message || '无法连接远程 MySQL'
        this.$message.error(this.error)
      } finally {
        this.loading = false
      }
    },

    async loadList() {
      this.listLoading = true
      try {
        if (this.tab === 'groups') {
          const res = await getRemoteGroups({ keyword: this.keyword, limit: 200 })
          this.groups = res.data || []
          if (!this.allGroupsView && (!this.selectedGroupId || !this.selectedGroup)) {
            this.selectedGroupId = this.groups.length ? this.groups[0].id : null
          }
          await this.loadMessages()
        } else {
          const res = await getRemoteUsers({ keyword: this.keyword, limit: 200 })
          this.users = res.data || []
        }
      } catch (e) {
        this.$message.error(e.message || '加载失败')
      } finally {
        this.listLoading = false
      }
    },

    switchTab(tab) {
      if (this.tab === tab) return
      this.tab = tab
      this.keyword = ''
      this.loadList()
    },

    async selectGroup(group) {
      this.allGroupsView = false
      this.selectedGroupId = group.id
      this.messageKeyword = ''
      this.nextAfterId = 0
      await this.loadMessages()
    },

    async selectAllGroups() {
      this.allGroupsView = true
      this.selectedGroupId = null
      this.messageKeyword = ''
      await this.loadMessages()
    },

    async loadMessages() {
      this.messageLoading = true
      try {
        const keyword = this.messageKeyword.trim()
        if (keyword) {
          const res = await getRemoteMessages({
            keyword,
            groupId: this.selectedGroupId || undefined,
            limit: 200
          })
          // 后端按 id 倒序返回，这里翻转成对话顺序
          this.messages = (res.data || []).slice().reverse()
          this.hasMore = false
          return
        }

        if (this.selectedGroupId) {
          const res = await getRemoteGroupMessages(this.selectedGroupId, { afterId: 0, limit: 200 })
          const page = res.data || {}
          this.messages = page.items || []
          this.nextAfterId = page.nextAfterId || 0
          this.hasMore = Boolean(page.hasMore)
        } else {
          const res = await getRemoteMessages({ limit: 100 })
          this.messages = (res.data || []).slice().reverse()
          this.hasMore = false
        }
      } catch (e) {
        this.messages = []
        this.$message.error(e.message || '消息加载失败')
      } finally {
        this.messageLoading = false
      }
    },

    async loadMore() {
      if (!this.selectedGroupId || this.messageLoading) return
      this.messageLoading = true
      try {
        const res = await getRemoteGroupMessages(this.selectedGroupId, {
          afterId: this.nextAfterId,
          limit: 200
        })
        const page = res.data || {}
        this.messages = this.messages.concat(page.items || [])
        this.nextAfterId = page.nextAfterId || this.nextAfterId
        this.hasMore = Boolean(page.hasMore)
      } catch (e) {
        this.$message.error(e.message || '加载更多失败')
      } finally {
        this.messageLoading = false
      }
    },

    displayCount(value) {
      return value === null || value === undefined ? '—' : value
    },

    formatTime(value) {
      if (!value) return '—'
      const parsed = dayjs(value)
      return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : String(value)
    }
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 18px; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.page-head h2 { font-size: 22px; font-weight: 650; letter-spacing: -.02em; margin-bottom: 4px; }
.head-actions { display: flex; align-items: center; gap: 10px; }
.btn-icon { margin-right: 4px; }
.readonly-badge {
  font-size: 10px;
  letter-spacing: .12em;
  color: var(--warn);
  background: rgba(217, 145, 0, .08);
  border: 1px solid rgba(217, 145, 0, .28);
  border-radius: 999px;
  padding: 4px 10px;
}

.alert {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid rgba(229, 72, 77, .25);
  background: rgba(229, 72, 77, .05);
  border-radius: var(--r-md);
  color: var(--text-2);
}
.alert strong { color: var(--danger); font-size: 13px; }
.alert p { margin-top: 4px; font-size: 12px; }
.alert-icon { color: var(--danger); font-size: 18px; margin-top: 1px; }

.metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.metric {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 18px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
}
.metric::after {
  content: '';
  position: absolute;
  right: -12px;
  top: -16px;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--accent-soft);
  opacity: .7;
}
.metric strong { font-size: 28px; line-height: 1.1; letter-spacing: -.03em; }
.metric small { font-size: 11px; color: var(--text-3); }

.connection { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 12px 18px; }
.conn-left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.conn-text { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.conn-text strong { font-size: 13px; }
.conn-text span { font-size: 11px; color: var(--text-3); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conn-right { display: flex; gap: 18px; font-size: 11px; color: var(--text-3); }
.live-dot {
  width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
  background: var(--ok);
  box-shadow: 0 0 0 4px rgba(23, 166, 115, .14);
}

.workspace {
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  gap: 16px;
  height: calc(100vh - 330px);
  min-height: 520px;
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
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
}
.tabs { display: flex; gap: 4px; }
.tab {
  border: 0;
  background: transparent;
  color: var(--text-3);
  font-size: 13px;
  padding: 5px 10px;
  border-radius: var(--r-sm);
  cursor: pointer;
  transition: all var(--dur-1) var(--ease-2);
}
.tab:hover { background: var(--bg-inset); color: var(--text); }
.tab.active { background: var(--accent-soft); color: var(--accent); font-weight: 600; }

.search-row { padding: 12px 14px; border-bottom: 1px solid var(--line); }
.list { flex: 1; overflow-y: auto; padding: 6px; }

.group-item,
.user-item {
  display: flex;
  gap: 10px;
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
.user-item { cursor: default; }
.group-item:hover { background: var(--bg-inset); }
.group-item.active { background: var(--accent-soft); border-color: var(--accent-line); }
.group-avatar {
  width: 34px; height: 34px; flex-shrink: 0;
  border-radius: 10px;
  background: var(--bg-inset);
  color: var(--text-2);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 650;
}
.group-item.active .group-avatar { background: var(--accent); color: var(--on-accent); }
.group-main { display: flex; flex-direction: column; gap: 2px; min-width: 0; flex: 1; }
.group-title { display: flex; align-items: center; gap: 7px; min-width: 0; }
.group-title strong { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.group-sub { font-size: 11px; color: var(--text-3); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.group-time { font-size: 10px; color: var(--text-3); }
.role-chip {
  font-size: 10px;
  color: var(--text-2);
  background: var(--bg-inset);
  border: 1px solid var(--line-2);
  border-radius: 999px;
  padding: 1px 7px;
  white-space: nowrap;
}

.messages-head { align-items: flex-start; }
.messages-title { min-width: 0; }
.messages-title h3 { font-size: 15px; font-weight: 650; margin-bottom: 3px; }
.message-tools { display: flex; gap: 8px; align-items: center; }
.message-search { width: 240px; }
.message-list { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 14px; }

.message-item { display: flex; gap: 11px; }
.msg-avatar {
  width: 32px; height: 32px; flex-shrink: 0;
  border-radius: 9px;
  background: linear-gradient(150deg, var(--accent-soft), var(--bg-inset));
  border: 1px solid var(--line-2);
  color: var(--text-2);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 650;
}
.msg-body { min-width: 0; flex: 1; }
.msg-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 4px; }
.msg-meta strong { font-size: 12.5px; }
.msg-meta time { margin-left: auto; font-size: 10px; color: var(--text-3); }
.group-chip {
  font-size: 10px;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid var(--accent-line);
  border-radius: 999px;
  padding: 1px 7px;
}
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
.msg-foot { margin-top: 4px; font-size: 9.5px; color: var(--text-3); letter-spacing: .02em; }
.load-more { padding: 10px; text-align: center; border-top: 1px solid var(--line); }
.dim { color: var(--text-3); }

@media (max-width: 1100px) {
  .metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .workspace { grid-template-columns: minmax(240px, 300px) minmax(0, 1fr); }
  .message-search { width: 180px; }
}
@media (max-width: 820px) {
  .workspace { grid-template-columns: 1fr; height: auto; }
  .groups-panel { max-height: 340px; }
  .messages-panel { min-height: 520px; }
  .messages-head { flex-direction: column; }
  .message-tools { width: 100%; }
  .message-search { flex: 1; width: auto; }
}
</style>
