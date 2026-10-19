<template>
  <div class="auth mesh noise">
    <ParticleField class="bg-fx" />

    <div class="card reveal is-visible">
      <header class="card-head">
        <div class="logo">S</div>
        <div>
          <h1>远程群聊</h1>
          <p class="u-label">STU Remote Chat</p>
        </div>
      </header>

      <div class="remote-note">
        <span class="dot"></span>
        <span>使用远程 MySQL <code>demo.user</code> 表中的账号登录，与本地 H2 账号体系相互独立。</span>
      </div>

      <el-form :model="form" size="large" class="form" @submit.prevent>
        <div class="field">
          <label class="u-label">远程账号</label>
          <el-input v-model="form.account" placeholder="stu 数据库 user.account" clearable @keyup.enter="handleLogin" />
        </div>
        <div class="field">
          <label class="u-label">密码</label>
          <el-input v-model="form.password" type="password" placeholder="远程账号密码" show-password @keyup.enter="handleLogin" />
        </div>

        <p v-if="errorText" class="error-text">{{ errorText }}</p>

        <button class="primary mono" type="button" :disabled="loading" @click="handleLogin">
          {{ loading ? 'CONNECTING...' : 'REMOTE SIGN IN' }}
        </button>
      </el-form>

      <footer class="card-foot">
        <span class="u-label">远程 MySQL · 明文群聊库</span>
        <span class="foot-links">
          <router-link class="link" to="/remote-register">注册账号 →</router-link>
          <router-link class="link muted" to="/login">← 返回本地登录</router-link>
        </span>
      </footer>
    </div>

    <p class="u-label copyright">独立登录态 · 只能访问远程群聊</p>
  </div>
</template>

<script>
import { remoteLogin } from '../api/remoteChat'
import ParticleField from '../components/ParticleField.vue'

export default {
  name: 'RemoteLogin',
  components: { ParticleField },
  data() {
    return {
      loading: false,
      errorText: '',
      form: {
        account: '',
        password: ''
      }
    }
  },
  methods: {
    async handleLogin() {
      const account = this.form.account.trim()
      if (!account || !this.form.password) {
        this.errorText = '请输入远程账号和密码'
        return
      }
      this.loading = true
      this.errorText = ''
      try {
        const res = await remoteLogin({ account, password: this.form.password })
        localStorage.setItem('remoteToken', res.data.token)
        localStorage.setItem('remoteUserInfo', JSON.stringify(res.data.userInfo))
        this.$message.success(res.msg || '远程账号登录成功')
        this.$router.push('/remote-chat')
      } catch (e) {
        this.errorText = e.message || '远程登录失败'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.auth {
  position: relative;
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.bg-fx { z-index: 0; }

.card {
  position: relative;
  z-index: 1;
  width: 430px;
  padding: 32px 32px 22px;
  background: rgba(255, 255, 255, .84);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border: 1px solid rgba(255, 255, 255, .9);
  border-radius: 18px;
  box-shadow: var(--shadow-lg);
}
.card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 18px;
  padding: 1px;
  background: linear-gradient(150deg, rgba(124, 92, 255, .45), rgba(10, 132, 255, .18), transparent 60%);
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.card-head { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; }
.logo {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  background: linear-gradient(150deg, var(--violet), var(--accent));
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 18px -6px rgba(124, 92, 255, .55);
}
.card-head h1 { font-size: 18px; font-weight: 650; letter-spacing: -.01em; margin-bottom: 3px; }

.remote-note {
  display: flex;
  gap: 9px;
  align-items: flex-start;
  padding: 10px 12px;
  margin-bottom: 18px;
  border: 1px solid var(--line);
  background: var(--bg-sub);
  border-radius: var(--r-md);
  color: var(--text-2);
  font-size: 11.5px;
  line-height: 1.6;
}
.remote-note code { font-family: var(--mono); font-size: 11px; color: var(--violet); }
.dot {
  width: 7px;
  height: 7px;
  margin-top: 5px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--violet);
  box-shadow: 0 0 0 3px rgba(124, 92, 255, .14);
}

.form { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 7px; }
.field :deep(.el-input__wrapper) {
  border-radius: var(--r-md);
  box-shadow: 0 0 0 1px var(--line-2) inset;
  transition: box-shadow var(--dur-1) var(--ease-2);
}
.field :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px var(--line-3) inset; }
.field :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--violet) inset, 0 0 0 3px rgba(124, 92, 255, .10); }

.error-text { color: var(--danger); font-size: 11.5px; margin-top: -2px; }

.primary {
  height: 44px;
  margin-top: 6px;
  border: none;
  border-radius: var(--r-md);
  background: var(--text);
  color: #fff;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: .1em;
  cursor: pointer;
  transition: transform var(--dur-1) var(--ease), box-shadow var(--dur-2) var(--ease), background var(--dur-1);
  box-shadow: 0 1px 2px rgba(13, 21, 32, .12);
}
.primary:hover:not(:disabled) { background: #16202c; box-shadow: var(--shadow-md); transform: translateY(-1px); }
.primary:active:not(:disabled) { transform: translateY(0) scale(.995); }
.primary:disabled { opacity: .55; cursor: not-allowed; }

.card-foot {
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.foot-links { display: flex; flex-direction: column; align-items: flex-end; gap: 5px; }
.link { font-size: 12px; color: var(--violet); text-decoration: none; white-space: nowrap; }
.link:hover { text-decoration: underline; }
.link.muted { color: var(--text-3); }
.copyright { position: relative; z-index: 1; margin-top: 22px; }
</style>
