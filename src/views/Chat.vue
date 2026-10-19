<template>
  <div class="page chat-page">
    <header class="page-head">
      <div>
        <h2>消息</h2>
        <p class="u-hint">端到端加密 · 每设备独立密钥 · 服务端只存密文</p>
      </div>
      <div class="head-actions">
        <template v-if="deviceState === 'active'">
          <span class="device-chip mono">DEVICE {{ localDevice.deviceId }}</span>
          <el-button size="small" @click="openPrivate">
            <el-icon class="btn-icon"><User /></el-icon>新私聊
          </el-button>
          <el-button size="small" @click="openGroup" :disabled="Boolean(groupPermission && !groupPermission.canCreateGroup)">
            <el-icon class="btn-icon"><Plus /></el-icon>新建群
          </el-button>
          <el-button size="small" @click="openDevices">
            <el-icon class="btn-icon"><ChatDotRound /></el-icon>设备
          </el-button>
        </template>
      </div>
    </header>

    <section
      v-if="deviceState === 'loading' || deviceState === 'initializing'"
      class="state-panel panel"
      v-loading="true"
    >
      <p class="state-text">{{ deviceState === 'initializing' ? '正在生成身份密钥和预共享密钥…' : '正在检查本机设备…' }}</p>
    </section>

    <section v-else-if="deviceState === 'need-init'" class="state-panel panel">
      <div class="state-badge">E2EE</div>
      <h3>初始化本机加密设备</h3>
      <p class="state-desc">
        身份密钥、签名预密钥和一次性预密钥只保存在浏览器本地；服务端只保存公钥和密文。
      </p>
      <el-button type="primary" :loading="initializing" @click="initDevice">生成密钥并注册设备</el-button>
      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
    </section>

    <section v-else-if="deviceState === 'pending'" class="state-panel panel">
      <div class="state-badge pending">PENDING</div>
      <h3>等待已有设备授权</h3>
      <p class="state-desc">
        本设备已登记，但还需要同一账号下已激活的设备确认。
        请在另一台已登录设备上打开「设备」并批准。
      </p>
      <div class="key-box mono">{{ localDevice && localDevice.clientDeviceKey }}</div>
      <el-button type="primary" :loading="checking" @click="checkDeviceStatus()">检查授权状态</el-button>
      <p class="u-hint">新设备只能解密授权之后收到的消息；更早的消息需要历史密钥授权。</p>
    </section>

    <section v-else-if="deviceState === 'active'" class="chat-shell">
      <aside class="panel conv-panel">
        <div class="panel-head">
          <strong>会话</strong>
          <span class="u-label">{{ conversations.length }}</span>
        </div>
        <div class="conv-list">
          <button
            v-for="c in conversations"
            :key="c.id"
            type="button"
            class="conv-item"
            :class="{ active: c.id === selectedId }"
            @click="selectConversation(c)"
          >
            <span class="conv-avatar">{{ convTitle(c).charAt(0) }}</span>
            <span class="conv-main">
              <span class="conv-title">
                <strong>{{ convTitle(c) }}</strong>
                <span class="mono dim">{{ c.type === 'GROUP' ? 'GROUP' : 'DM' }}</span>
              </span>
              <span class="conv-sub">{{ convSub(c) }}</span>
            </span>
            <span v-if="c.unreadCount" class="unread">{{ c.unreadCount > 99 ? '99+' : c.unreadCount }}</span>
            <time class="conv-time mono">{{ formatTime(c.lastMessageAt || c.updatedAt) }}</time>
          </button>
          <el-empty v-if="!conversations.length" description="还没有会话，发起一个吧" :image-size="64" />
        </div>
      </aside>

      <section class="panel msg-panel">
        <template v-if="selectedConversation">
          <div class="msg-head">
            <div>
              <h3>{{ convTitle(selectedConversation) }}</h3>
              <p class="u-hint">
                {{ selectedConversation.members.length }} 位成员 ·
                {{ selectedConversation.type === 'GROUP' ? '群聊' : '私聊' }} ·
                v{{ selectedConversation.version }}
              </p>
            </div>
            <div class="msg-head-actions">
              <span class="e2ee-chip mono">E2EE</span>
              <el-button size="small" :loading="messagesLoading" @click="loadMessages(true)">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </div>

          <div ref="messageList" v-loading="messagesLoading" class="message-list">
            <article v-for="m in messages" :key="m.id" class="message-item" :class="{ mine: m.mine }">
              <span class="msg-avatar">{{ m.mine ? '我' : (m.senderUsername || '?').charAt(0) }}</span>
              <div class="msg-body">
                <div class="msg-meta">
                  <strong v-if="!m.mine">{{ m.senderUsername }}</strong>
                  <span class="mono dim">#{{ m.serverSeq }}</span>
                  <time class="mono">{{ formatTime(m.createdAt) }}</time>
                </div>
                <p class="msg-content" :class="{ locked: !m.decrypted }">{{ m.text }}</p>
                <div class="msg-foot mono">{{ m.senderClientDeviceKey }}</div>
              </div>
            </article>
            <el-empty v-if="!messages.length" description="还没有消息" :image-size="72" />
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
              @keydown.enter.exact.prevent="sendMessage"
            />
            <el-button
              type="primary"
              class="send-btn"
              :loading="sending"
              :disabled="!draft.trim()"
              @click="sendMessage"
            >
              发送
            </el-button>
          </div>
        </template>
        <div v-else class="empty-state">
          <el-empty description="选择一个会话开始" :image-size="96" />
        </div>
      </section>
    </section>

    <el-dialog v-model="privateVisible" title="发起私聊" width="420px">
      <div class="dialog-body">
        <span class="u-label">对方 username</span>
        <el-input v-model="privateTarget" placeholder="例如 manager" @keyup.enter="submitPrivate" />
      </div>
      <template #footer>
        <el-button @click="privateVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingPrivate" @click="submitPrivate">开始私聊</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="groupVisible" title="创建群聊" width="480px">
      <div class="dialog-body">
        <span class="u-label">群名称</span>
        <el-input v-model="groupName" maxlength="80" show-word-limit placeholder="例如：项目讨论组" />
        <span class="u-label">成员 username</span>
        <el-input
          v-model="groupMembersText"
          type="textarea"
          :rows="3"
          placeholder="多个成员用逗号、空格或换行分隔"
        />
        <p v-if="groupPermission && !groupPermission.canCreateGroup" class="error-text">
          {{ groupPermission.reason }}
        </p>
      </div>
      <template #footer>
        <el-button @click="groupVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingGroup" @click="submitGroup">创建群聊</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="devicesVisible" title="设备管理" width="680px">
      <div v-loading="deviceLoading" class="device-dialog">
        <div class="device-toolbar">
          <span class="u-hint">同一账号每台设备独立密钥；新设备需要已有 ACTIVE 设备批准。</span>
          <el-button size="small" :loading="deviceLoading" @click="loadDevices">刷新</el-button>
        </div>

        <div class="device-list">
          <div v-for="d in devices" :key="d.clientDeviceKey" class="device-row">
            <div class="device-main">
              <strong>
                {{ d.deviceName || 'Web 设备' }}
                <span v-if="isCurrentDevice(d)" class="current-chip">当前</span>
              </strong>
              <span class="mono dim">{{ d.clientDeviceKey }} · ID {{ d.deviceId }}</span>
            </div>
            <span class="status-chip" :class="'status-' + String(d.status).toLowerCase()">{{ d.status }}</span>
            <el-button
              v-if="!isCurrentDevice(d) && d.status === 'ACTIVE'"
              link
              type="danger"
              @click="revokeDevice(d)"
            >
              撤销
            </el-button>
          </div>
        </div>

        <div v-if="pendingLinks.length" class="pending-block">
          <span class="u-label">待授权设备</span>
          <div v-for="l in pendingLinks" :key="l.id" class="device-row">
            <div class="device-main">
              <strong>{{ l.newDeviceName || 'Web 设备' }}</strong>
              <span class="mono dim">{{ l.newDeviceKey }} · ID {{ l.newDeviceId }}</span>
            </div>
            <el-button size="small" type="primary" @click="approvePending(l)">批准</el-button>
            <el-button size="small" @click="rejectPending(l)">拒绝</el-button>
          </div>
        </div>

        <p class="u-hint device-note">
          批准后，新设备可以解密之后发送给它的消息；批准前的旧消息需要额外历史密钥授权。
        </p>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import dayjs from 'dayjs'
import {
  getChatDevices,
  getPendingDeviceLinks,
  approveChatDevice,
  rejectChatDevice,
  revokeChatDevice,
  getDeviceBundles,
  getGroupPermission,
  listChatConversations,
  createPrivateConversation,
  createGroupConversation,
  markConversationRead,
  syncChatMessages,
  sendChatMessage
} from '../api/chat'
import {
  ensureDevice,
  getStoredDevice,
  refreshDeviceStatus,
  signalStoreFor,
  encryptForDeviceBundles,
  decryptEnvelope
} from '../utils/chatCrypto'

const PLAIN_PREFIX = 'wa-plain'

export default {
  name: 'Chat',
  data() {
    return {
      username: '',
      deviceState: 'loading',
      localDevice: null,
      signalStore: null,
      initializing: false,
      checking: false,
      errorMessage: '',

      conversations: [],
      selectedId: null,
      messages: [],
      lastSeq: 0,
      draft: '',
      messagesLoading: false,
      sending: false,
      groupPermission: null,

      privateVisible: false,
      privateTarget: '',
      creatingPrivate: false,

      groupVisible: false,
      groupName: '',
      groupMembersText: '',
      creatingGroup: false,

      devicesVisible: false,
      devices: [],
      pendingLinks: [],
      deviceLoading: false,

      pollTimer: null,
      statusTimer: null,
      conversationTimer: null
    }
  },
  computed: {
    selectedConversation() {
      return this.conversations.find(c => c.id === this.selectedId) || null
    },
    currentDeviceKey() {
      return this.localDevice ? this.localDevice.clientDeviceKey : ''
    }
  },
  created() {
    this.username = this.$store.state.userInfo?.username || ''
    if (!this.username) {
      this.$router.replace('/login')
      return
    }
    this.bootstrap()
  },
  beforeUnmount() {
    this.stopPolling()
    this.stopStatusPolling()
  },
  methods: {
    async bootstrap() {
      const stored = getStoredDevice(this.username)
      if (stored) {
        this.localDevice = stored
        this.signalStore = markRaw(signalStoreFor(stored.clientDeviceKey))
        if (stored.status === 'ACTIVE') {
          await this.enterChat()
          return
        }
        if (stored.status === 'PENDING') {
          this.deviceState = 'pending'
          this.startStatusPolling()
          return
        }
      }
      this.deviceState = 'need-init'
    },

    async initDevice() {
      this.initializing = true
      this.deviceState = 'initializing'
      this.errorMessage = ''
      try {
        const result = await ensureDevice(this.username)
        this.localDevice = result.device
        this.signalStore = markRaw(result.store)
        if (result.device.status === 'PENDING' || result.pendingApproval) {
          this.deviceState = 'pending'
          this.startStatusPolling()
        } else {
          await this.enterChat()
        }
      } catch (e) {
        this.deviceState = 'need-init'
        this.errorMessage = e.message || '设备初始化失败'
      } finally {
        this.initializing = false
      }
    },

    async checkDeviceStatus(silent = false) {
      if (!this.localDevice) {
        this.deviceState = 'need-init'
        return
      }
      this.checking = true
      try {
        const result = await refreshDeviceStatus(this.username, this.localDevice)
        if (result.status === 'ACTIVE') {
          this.localDevice = result.device
          this.stopStatusPolling()
          if (!silent) this.$message.success('设备已激活')
          await this.enterChat()
        } else if (result.status === 'PENDING') {
          this.localDevice = result.device
          this.deviceState = 'pending'
          if (!silent) this.$message.info('设备仍在等待授权')
        } else {
          this.localDevice = null
          this.deviceState = 'need-init'
          if (!silent) this.$message.warning('本机设备已不存在，请重新注册')
        }
      } catch (e) {
        if (!silent) this.$message.error(e.message || '检查设备状态失败')
      } finally {
        this.checking = false
      }
    },

    async enterChat() {
      this.deviceState = 'active'
      await Promise.all([this.loadGroupPermission(), this.loadConversations()])
      this.startPolling()
    },

    async loadGroupPermission() {
      try {
        const res = await getGroupPermission()
        this.groupPermission = res.data
      } catch (e) {
        this.groupPermission = null
      }
    },

    async loadConversations() {
      try {
        const res = await listChatConversations({ deviceKey: this.currentDeviceKey })
        this.conversations = res.data || []
        if (!this.conversations.length) {
          this.selectedId = null
          this.messages = []
          this.lastSeq = 0
          return
        }
        let target = this.conversations.find(c => c.id === this.selectedId)
        if (!target) target = this.conversations[0]
        if (target.id !== this.selectedId) {
          await this.selectConversation(target)
        }
      } catch (e) {
        this.$message.error(e.message || '会话列表加载失败')
      }
    },

    async silentRefreshConversations() {
      try {
        const res = await listChatConversations({ deviceKey: this.currentDeviceKey })
        this.conversations = res.data || []
        if (this.selectedId && !this.conversations.some(c => c.id === this.selectedId)) {
          const next = this.conversations[0]
          if (next) await this.selectConversation(next)
          else {
            this.selectedId = null
            this.messages = []
            this.lastSeq = 0
          }
        }
      } catch (e) {
        // 轮询失败不打扰用户
      }
    },

    async selectConversation(conv) {
      if (!conv) return
      this.selectedId = conv.id
      this.messages = []
      this.lastSeq = 0
      this.draft = ''
      await this.loadMessages(true)
    },

    async loadMessages(reset, silent = false) {
      const conv = this.selectedConversation
      if (!conv || !this.signalStore) return
      if (reset) this.messagesLoading = true
      try {
        const afterSeq = reset ? 0 : this.lastSeq
        const res = await syncChatMessages({
          conversationId: conv.id,
          deviceKey: this.currentDeviceKey,
          afterSeq,
          limit: 200
        })
        const incoming = res.data || []
        const view = []
        for (const message of incoming) {
          view.push(await this.decryptMessage(message))
        }
        if (reset) {
          this.messages = view
        } else if (view.length) {
          const existing = new Set(this.messages.map(m => m.id))
          this.messages = this.messages.concat(view.filter(m => !existing.has(m.id)))
        }
        this.messages.sort((a, b) => Number(a.serverSeq) - Number(b.serverSeq))
        this.lastSeq = this.messages.length
          ? this.messages[this.messages.length - 1].serverSeq
          : (reset ? 0 : this.lastSeq)
        if (incoming.length) {
          this.markRead(this.lastSeq)
          this.scrollToBottom(reset)
        }
      } catch (e) {
        if (!silent) this.$message.error(e.message || '消息加载失败')
      } finally {
        if (reset) this.messagesLoading = false
      }
    },

    async decryptMessage(message) {
      const mine = message.senderUsername === this.username
      const cached = this.readPlaintext(message)
      if (cached !== null) {
        return { ...message, text: cached, decrypted: true, mine }
      }
      if (!message.envelope) {
        if (message.senderClientDeviceKey === this.currentDeviceKey) {
          return {
            ...message,
            text: '（本机发送，明文未缓存）',
            decrypted: false,
            mine
          }
        }
        return {
          ...message,
          text: '无法解密：这条消息没有发往当前设备（可能发送于设备授权之前）',
          decrypted: false,
          mine
        }
      }
      try {
        const text = await decryptEnvelope(this.signalStore, message)
        this.cachePlaintext(message, text)
        return { ...message, text, decrypted: true, mine }
      } catch (e) {
        return {
          ...message,
          text: '无法解密：可能是设备授权前的旧消息，或密钥状态已变化',
          decrypted: false,
          mine
        }
      }
    },

    async sendMessage() {
      const conv = this.selectedConversation
      if (!conv || this.sending) return
      const text = this.draft
      if (!text.trim()) {
        this.$message.warning('消息不能为空')
        return
      }
      this.sending = true
      try {
        const bundles = await Promise.all(
          conv.members.map(member => getDeviceBundles(member.username))
        )
        const targetMap = new Map()
        for (const res of bundles) {
          for (const bundle of (res.data || [])) {
            if (bundle.clientDeviceKey !== this.currentDeviceKey) {
              targetMap.set(bundle.clientDeviceKey, bundle)
            }
          }
        }
        const envelopes = await encryptForDeviceBundles(
          this.signalStore,
          text,
          Array.from(targetMap.values())
        )
        const clientMessageId = `c-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
        const res = await sendChatMessage({
          conversationId: conv.id,
          clientMessageId,
          senderDeviceKey: this.currentDeviceKey,
          contentType: 'TEXT',
          expectedVersion: conv.version,
          envelopes
        })
        const saved = res.data.message
        this.cachePlaintext({ ...saved, clientMessageId }, text)
        if (!this.messages.some(m => m.id === saved.id)) {
          this.messages = this.messages.concat({
            ...saved,
            text,
            decrypted: true,
            mine: true
          })
          this.lastSeq = saved.serverSeq
        }
        conv.lastMessageSeq = saved.serverSeq
        conv.lastMessageAt = saved.createdAt
        conv.unreadCount = 0
        conv.lastReadSeq = saved.serverSeq
        this.draft = ''
        this.scrollToBottom(true)
        await this.markRead(saved.serverSeq)
        await this.silentRefreshConversations()
      } catch (e) {
        this.$message.error(e.message || '发送失败')
      } finally {
        this.sending = false
      }
    },

    async markRead(seq) {
      const conv = this.selectedConversation
      if (!conv || !seq) return
      try {
        await markConversationRead(conv.id, {
          deviceKey: this.currentDeviceKey,
          lastReadSeq: seq
        })
        conv.lastReadSeq = seq
        conv.unreadCount = 0
      } catch (e) {
        // 已读失败不阻塞消息展示
      }
    },

    plaintextIdKey(message) {
      return `${PLAIN_PREFIX}:${this.username}:${this.currentDeviceKey}:id:${message.id}`
    },

    plaintextClientKey(clientMessageId) {
      return `${PLAIN_PREFIX}:${this.username}:${this.currentDeviceKey}:client:${clientMessageId}`
    },

    readPlaintext(message) {
      const byId = message.id ? localStorage.getItem(this.plaintextIdKey(message)) : null
      if (byId !== null) return byId
      const byClient = message.clientMessageId
        ? localStorage.getItem(this.plaintextClientKey(message.clientMessageId))
        : null
      return byClient
    },

    cachePlaintext(message, text) {
      if (message.id) localStorage.setItem(this.plaintextIdKey(message), text)
      if (message.clientMessageId) {
        localStorage.setItem(this.plaintextClientKey(message.clientMessageId), text)
      }
    },

    openPrivate() {
      this.privateTarget = ''
      this.privateVisible = true
    },

    async submitPrivate() {
      const target = this.privateTarget.trim()
      if (!target) {
        this.$message.warning('请输入对方 username')
        return
      }
      this.creatingPrivate = true
      try {
        const res = await createPrivateConversation({ targetUsername: target })
        this.privateVisible = false
        await this.loadConversations()
        await this.selectConversationById(res.data.id)
      } catch (e) {
        this.$message.error(e.message || '发起私聊失败')
      } finally {
        this.creatingPrivate = false
      }
    },

    openGroup() {
      if (this.groupPermission && !this.groupPermission.canCreateGroup) {
        this.$message.warning(this.groupPermission.reason || '当前账号不能创建群聊')
        return
      }
      this.groupName = ''
      this.groupMembersText = ''
      this.groupVisible = true
    },

    async submitGroup() {
      const name = this.groupName.trim()
      const members = this.groupMembersText
        .split(/[\s,，;；]+/)
        .map(v => v.trim())
        .filter(Boolean)
      if (!name) {
        this.$message.warning('请输入群名称')
        return
      }
      if (!members.length) {
        this.$message.warning('请至少输入一位成员 username')
        return
      }
      this.creatingGroup = true
      try {
        const res = await createGroupConversation({ name, memberUsernames: members })
        this.groupVisible = false
        await this.loadConversations()
        await this.selectConversationById(res.data.id)
      } catch (e) {
        this.$message.error(e.message || '创建群聊失败')
      } finally {
        this.creatingGroup = false
      }
    },

    async selectConversationById(id) {
      const conv = this.conversations.find(c => c.id === id)
      if (conv) await this.selectConversation(conv)
    },

    openDevices() {
      this.devicesVisible = true
      this.loadDevices()
    },

    async loadDevices() {
      this.deviceLoading = true
      try {
        const [devices, pending] = await Promise.all([
          getChatDevices(),
          getPendingDeviceLinks(this.currentDeviceKey)
        ])
        this.devices = devices.data || []
        this.pendingLinks = pending.data || []
      } catch (e) {
        this.$message.error(e.message || '设备列表加载失败')
      } finally {
        this.deviceLoading = false
      }
    },

    isCurrentDevice(device) {
      return device.clientDeviceKey === this.currentDeviceKey
    },

    async approvePending(link) {
      try {
        await approveChatDevice({
          approverDeviceKey: this.currentDeviceKey,
          targetDeviceKey: link.newDeviceKey,
          grants: []
        })
        this.$message.success('设备已批准；它只能解密批准后发送给它的消息')
        await this.loadDevices()
      } catch (e) {
        this.$message.error(e.message || '批准设备失败')
      }
    },

    async rejectPending(link) {
      try {
        await rejectChatDevice({
          approverDeviceKey: this.currentDeviceKey,
          targetDeviceKey: link.newDeviceKey
        })
        this.$message.success('已拒绝该设备')
        await this.loadDevices()
      } catch (e) {
        this.$message.error(e.message || '拒绝设备失败')
      }
    },

    async revokeDevice(device) {
      try {
        await this.$confirm(
          `撤销设备「${device.deviceName || device.clientDeviceKey}」后，它不能再收发消息。`,
          '撤销设备',
          { confirmButtonText: '撤销', cancelButtonText: '取消', type: 'warning' }
        )
      } catch (e) {
        return
      }
      try {
        await revokeChatDevice({
          actorDeviceKey: this.currentDeviceKey,
          targetDeviceKey: device.clientDeviceKey
        })
        this.$message.success('设备已撤销')
        await this.loadDevices()
      } catch (e) {
        this.$message.error(e.message || '撤销设备失败')
      }
    },

    convTitle(conv) {
      if (conv.type === 'GROUP') return conv.name || `群聊 #${conv.id}`
      const other = (conv.members || []).find(m => m.username !== this.username)
      return other ? other.username : `私聊 #${conv.id}`
    },

    convSub(conv) {
      if (conv.type === 'GROUP') {
        return `${conv.members.length} 人 · ${conv.myRole === 'OWNER' ? '群主' : '成员'}`
      }
      return '端到端加密私聊'
    },

    formatTime(value) {
      if (!value) return '—'
      const parsed = dayjs(value)
      return parsed.isValid() ? parsed.format('MM-DD HH:mm') : String(value)
    },

    scrollToBottom(force) {
      this.$nextTick(() => {
        const el = this.$refs.messageList
        if (!el) return
        const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 90
        if (force || nearBottom) el.scrollTop = el.scrollHeight
      })
    },

    startPolling() {
      this.stopPolling()
      this.pollTimer = setInterval(() => {
        if (this.deviceState === 'active' && this.selectedConversation && !this.sending && !this.messagesLoading) {
          this.loadMessages(false, true)
        }
      }, 3000)
      this.conversationTimer = setInterval(() => {
        if (this.deviceState === 'active') this.silentRefreshConversations()
      }, 15000)
    },

    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
      if (this.conversationTimer) {
        clearInterval(this.conversationTimer)
        this.conversationTimer = null
      }
    },

    startStatusPolling() {
      this.stopStatusPolling()
      this.statusTimer = setInterval(() => {
        if (this.deviceState === 'pending') this.checkDeviceStatus(true)
      }, 5000)
    },

    stopStatusPolling() {
      if (this.statusTimer) {
        clearInterval(this.statusTimer)
        this.statusTimer = null
      }
    }
  }
}
</script>

<style scoped>
.chat-page { height: calc(100vh - 120px); min-height: 560px; display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-shrink: 0; }
.page-head h2 { font-size: 22px; font-weight: 650; letter-spacing: -.02em; margin-bottom: 4px; }
.head-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.btn-icon { margin-right: 3px; }
.device-chip {
  font-size: 9.5px;
  letter-spacing: .1em;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid var(--accent-line);
  border-radius: 999px;
  padding: 4px 9px;
}

.panel {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-xs);
}
.state-panel {
  flex: 1;
  min-height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 40px;
  text-align: center;
}
.state-panel h3 { font-size: 19px; font-weight: 650; }
.state-desc { max-width: 520px; font-size: 12.5px; line-height: 1.8; color: var(--text-2); }
.state-badge {
  font-family: var(--mono);
  font-size: 10px;
  letter-spacing: .16em;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid var(--accent-line);
  border-radius: 999px;
  padding: 7px 14px;
}
.state-badge.pending { color: var(--warn); background: rgba(217, 145, 0, .08); border-color: rgba(217, 145, 0, .3); }
.state-text { color: var(--text-2); font-size: 13px; }
.key-box {
  max-width: 560px;
  overflow-wrap: anywhere;
  padding: 10px 14px;
  background: var(--bg-inset);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  font-size: 11px;
  color: var(--text-2);
}
.error-text { color: var(--danger); font-size: 12px; }

.chat-shell {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(240px, 300px) minmax(0, 1fr);
  gap: 14px;
}
.conv-panel, .msg-panel { display: flex; flex-direction: column; min-height: 0; overflow: hidden; }
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
}
.panel-head strong { font-size: 13px; }
.conv-list { flex: 1; overflow-y: auto; padding: 6px; }
.conv-item {
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
.conv-item:hover { background: var(--bg-inset); }
.conv-item.active { background: var(--accent-soft); border-color: var(--accent-line); }
.conv-avatar {
  width: 34px; height: 34px; flex-shrink: 0;
  border-radius: 10px;
  background: var(--bg-inset);
  color: var(--text-2);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 650;
}
.conv-item.active .conv-avatar { background: var(--accent); color: var(--on-accent); }
.conv-main { display: flex; flex-direction: column; gap: 2px; min-width: 0; flex: 1; }
.conv-title { display: flex; align-items: center; gap: 6px; min-width: 0; }
.conv-title strong { font-size: 12.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-sub { font-size: 10.5px; color: var(--text-3); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-time { font-size: 9.5px; color: var(--text-3); margin-left: auto; align-self: flex-start; }
.unread {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--danger);
  color: var(--on-accent);
  font-size: 9.5px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.msg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
}
.msg-head h3 { font-size: 15px; font-weight: 650; margin-bottom: 3px; }
.msg-head-actions { display: flex; align-items: center; gap: 8px; }
.e2ee-chip {
  font-size: 9px;
  letter-spacing: .12em;
  color: var(--ok);
  background: rgba(23, 166, 115, .08);
  border: 1px solid rgba(23, 166, 115, .22);
  border-radius: 999px;
  padding: 3px 8px;
}
.message-list { flex: 1; overflow-y: auto; padding: 18px; display: flex; flex-direction: column; gap: 14px; }
.message-item { display: flex; gap: 10px; max-width: 78%; }
.message-item.mine { align-self: flex-end; flex-direction: row-reverse; }
.msg-avatar {
  width: 32px; height: 32px; flex-shrink: 0;
  border-radius: 9px;
  background: var(--bg-inset);
  border: 1px solid var(--line-2);
  color: var(--text-2);
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 650;
}
.message-item.mine .msg-avatar { background: var(--accent-soft); border-color: var(--accent-line); color: var(--accent); }
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
  background: var(--accent-soft);
  border-color: var(--accent-line);
  border-radius: 10px 2px 10px 10px;
}
.msg-content.locked { color: var(--text-3); font-style: italic; }
.msg-foot { margin-top: 4px; font-size: 9px; color: var(--text-3); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
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
.empty-state { flex: 1; display: flex; align-items: center; justify-content: center; }

.dialog-body { display: flex; flex-direction: column; gap: 8px; }
.device-dialog { display: flex; flex-direction: column; gap: 12px; min-height: 120px; }
.device-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.device-list { display: flex; flex-direction: column; gap: 2px; }
.device-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px solid var(--line);
}
.device-main { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
.device-main strong { font-size: 12.5px; }
.device-main span { font-size: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.current-chip {
  font-size: 9px;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid var(--accent-line);
  border-radius: 999px;
  padding: 1px 6px;
}
.status-chip {
  font-size: 9.5px;
  letter-spacing: .06em;
  padding: 3px 8px;
  border-radius: 999px;
  border: 1px solid var(--line-2);
  color: var(--text-3);
  background: var(--bg-inset);
}
.status-chip.status-active { color: var(--ok); background: rgba(23, 166, 115, .08); border-color: rgba(23, 166, 115, .2); }
.status-chip.status-pending { color: var(--warn); background: rgba(217, 145, 0, .08); border-color: rgba(217, 145, 0, .25); }
.status-chip.status-revoked { color: var(--danger); background: rgba(229, 72, 77, .07); border-color: rgba(229, 72, 77, .22); }
.pending-block { display: flex; flex-direction: column; gap: 4px; padding-top: 4px; }
.device-note { margin-top: 2px; }
.dim { color: var(--text-3); }

@media (max-width: 1000px) {
  .chat-shell { grid-template-columns: minmax(220px, 270px) minmax(0, 1fr); }
  .message-item { max-width: 90%; }
}
@media (max-width: 820px) {
  .chat-page { height: auto; }
  .chat-shell { grid-template-columns: 1fr; }
  .conv-panel { max-height: 280px; }
  .msg-panel { min-height: 520px; }
  .head-actions { flex-wrap: wrap; justify-content: flex-end; }
}
</style>
