<template>
  <div class="auth mesh noise">
    <ParticleField class="bg-fx" />

    <div class="card reveal is-visible">
      <header class="card-head">
        <div class="logo">S</div>
        <div>
          <h1>{{ mode === 'login' ? '远程群聊' : '注册远程账号' }}</h1>
          <p class="u-label">{{ mode === 'login' ? 'STU Remote Chat' : 'STU Remote Register' }}</p>
        </div>
      </header>

      <div class="mode-switch">
        <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
        <button type="button" :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
      </div>

      <div class="remote-note">
        <span class="dot"></span>
        <span v-if="mode === 'login'">
          使用远程 MySQL <code>demo.user</code> 表中的账号登录，与本地 H2 账号体系相互独立。
        </span>
        <span v-else>
          注册会直接写入远程 <code>user</code> 表并绑定 <code>member</code> 角色；注册成功自动登录。
        </span>
      </div>

      <el-form :model="form" size="large" class="form" @submit.prevent>
        <div v-if="mode === 'register'" class="field">
          <label class="u-label">昵称（可选）</label>
          <el-input v-model="form.name" placeholder="显示名称，留空则使用账号" clearable @keyup.enter="handleSubmit" />
        </div>
        <div class="field">
          <label class="u-label">远程账号</label>
          <el-input v-model="form.account" placeholder="stu 数据库 user.account" clearable @keyup.enter="handleSubmit" />
        </div>
        <div class="field">
          <label class="u-label">密码</label>
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="mode === 'register' ? '6-32 位密码' : '远程账号密码'"
            show-password
            @keyup.enter="handleSubmit"
          />
        </div>
        <div v-if="mode === 'register'" class="field">
          <label class="u-label">确认密码</label>
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" show-password @keyup.enter="handleSubmit" />
        </div>

        <p v-if="errorText" class="error-text">{{ errorText }}</p>

        <button class="primary mono" type="button" :disabled="loading" @click="handleSubmit">
          {{ loading ? 'CONNECTING...' : (mode === 'login' ? 'REMOTE SIGN IN' : 'CREATE ACCOUNT') }}
        </button>
      </el-form>

      <footer class="card-foot">
        <button type="button" class="link-btn" @click="switchMode(mode === 'login' ? 'register' : 'login')">
          {{ mode === 'login' ? '没有远程账号？注册一个 →' : '已有远程账号？去登录 →' }}
        </button>
        <router-link class="link" to="/login">← 返回本地登录</router-link>
      </footer>
    </div>

    <p class="u-label copyright">独立登录态 · 只能访问远程群聊</p>
  </div>
</template>

<script>
import { remoteLogin, remoteRegister } from '../api/remoteChat'
import ParticleField from '../components/ParticleField.vue'

export default {
  name: 'RemoteLogin',
  components: { ParticleField },
  data() {
    return {
      mode: 'login',
      loading: false,
      errorText: '',
      form: {
        name: '',
        account: '',
        password: '',
        confirmPassword: ''
      }
    }
  },
  methods: {
    switchMode(mode) {
      if (this.mode === mode) return
      this.mode = mode
      this.errorText = ''
      this.form.confirmPassword = ''
    },

    validate() {
      const account = this.form.account.trim()
      if (!account) {
        this.errorText = '请输入远程账号'
        return false
      }
      if (!this.form.password) {
        this.errorText = '请输入密码'
        return false
      }
      if (this.mode === 'register') {
        if (account.length < 2) {
          this.errorText = '远程账号至少 2 个字符'
          return false
        }
        if (this.form.password.length < 6 || this.form.password.length > 32) {
          this.errorText = '密码长度必须为 6-32 个字符'
          return false
        }
        if (this.form.password !== this.form.confirmPassword) {
          this.errorText = '两次输入的密码不一致'
          return false
        }
      }
      return true
    },

    async handleSubmit() {
      if (this.loading || !this.validate()) return
      this.loading = true
      this.errorText = ''
      try {
        const account = this.form.account.trim()
        const res = this.mode === 'register'
          ? await remoteRegister({
              name: this.form.name.trim() || undefined,
              account,
              password: this.form.password,
              confirmPassword: this.form.confirmPassword
            })
          : await remoteLogin({ account, password: this.form.password })

        localStorage.setItem('remoteToken', res.data.token)
        localStorage.setItem('remoteUserInfo', JSON.stringify(res.data.userInfo))
        this.$message.success(res.msg || (this.mode === 'register' ? '远程账号注册成功' : '远程账号登录成功'))
        this.$router.push('/remote-chat')
      } catch (e) {
        this.errorText = e.message || (this.mode === 'register' ? '远程注册失败' : '远程登录失败')
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
  width: 440px;
  padding: 30px 32px 22px;
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

.card-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
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

.mode-switch {
  display: flex;
  gap: 4px;
  padding: 3px;
  margin-bottom: 14px;
  background: var(--bg-inset);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
}
.mode-switch button {
  flex: 1;
  border: 0;
  background: transparent;
  color: var(--text-3);
  font-size: 12px;
  padding: 7px 10px;
  border-radius: 7px;
  cursor: pointer;
  transition: all var(--dur-1) var(--ease-2);
}
.mode-switch button:hover { color: var(--text); }
.mode-switch button.active {
  color: var(--violet);
  background: var(--surface);
  box-shadow: var(--shadow-xs);
  font-weight: 600;
}

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

.form { display: flex; flex-direction: column; gap: 14px; }
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
  margin-top: 4px;
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
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.link { font-size: 12px; color: var(--violet); text-decoration: none; white-space: nowrap; }
.link:hover { text-decoration: underline; }
.link-btn {
  border: 0;
  background: transparent;
  padding: 0;
  font-size: 12px;
  color: var(--accent);
  cursor: pointer;
}
.link-btn:hover { text-decoration: underline; }
.copyright { position: relative; z-index: 1; margin-top: 22px; }
</style>
