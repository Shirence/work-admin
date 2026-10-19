<template>
  <div class="auth mesh noise">
    <ParticleField class="bg-fx" />

    <div class="card reveal is-visible">
      <header class="card-head">
        <div class="logo">W</div>
        <div>
          <h1>工作管理中台</h1>
          <p class="u-label">Work Admin Console</p>
        </div>
      </header>

      <el-form ref="loginFormRef" :model="loginForm" size="large" class="form" @submit.prevent>
        <div class="field">
          <label class="u-label">账号</label>
          <el-input v-model="loginForm.username" placeholder="admin / manager / viewer" clearable @keyup.enter="handleLogin" />
        </div>
        <div class="field">
          <label class="u-label">密码</label>
          <el-input v-model="loginForm.password" type="password" placeholder="123456" show-password @keyup.enter="handleLogin" />
        </div>
        <button class="primary mono" type="button" :disabled="loading" @click="handleLogin">
          {{ loading ? 'SIGNING IN...' : 'SIGN IN' }}
        </button>
      </el-form>

      <footer class="card-foot">
        <span class="u-label">演示账号 admin / manager / viewer · 密码 123456</span>
        <span class="foot-links">
          <router-link class="link" to="/register">注册账号 →</router-link>
          <router-link class="link remote-link" to="/remote-login">远程账号登录 →</router-link>
        </span>
      </footer>
    </div>

    <p class="u-label copyright">Vue 3 · Element Plus · Spring Boot</p>
  </div>
</template>

<script>
import { login as loginApi } from '../api/auth'
import ParticleField from '../components/ParticleField.vue'

export default {
  name: "Login",
  components: { ParticleField },
  data() {
    return {
      loading: false,
      loginForm: { username: "", password: "" }
    }
  },
  methods: {
    async handleLogin() {
      const username = this.loginForm.username.trim()
      if (!username || !this.loginForm.password) {
        this.$message.error('请输入账号和密码')
        return
      }
      this.loading = true
      try {
        const res = await loginApi({ username, password: this.loginForm.password })
        this.$store.commit('setToken', res.data.token)
        this.$store.commit('setUserInfo', res.data.userInfo)
        this.$message.success(res.msg || '登录成功')
        this.$router.push('/dashboard')
      } catch (e) {
        this.$message.error(e.message || '登录失败')
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
  width: 400px;
  padding: 32px 32px 22px;
  background: rgba(255, 255, 255, .82);
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
  background: linear-gradient(150deg, rgba(10, 132, 255, .35), rgba(124, 92, 255, .18), transparent 60%);
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.card-head { display: flex; align-items: center; gap: 12px; margin-bottom: 26px; }
.logo {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  background: linear-gradient(150deg, var(--accent), var(--accent-2));
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 18px -6px rgba(10, 132, 255, .6);
}
.card-head h1 { font-size: 18px; font-weight: 650; letter-spacing: -.01em; margin-bottom: 3px; }

.form { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 7px; }
.field :deep(.el-input__wrapper) {
  border-radius: var(--r-md);
  box-shadow: 0 0 0 1px var(--line-2) inset;
  transition: box-shadow var(--dur-1) var(--ease-2);
}
.field :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px var(--line-3) inset; }
.field :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--accent) inset, 0 0 0 3px var(--accent-soft); }

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
.link { font-size: 12px; color: var(--accent); text-decoration: none; white-space: nowrap; }
.link:hover { text-decoration: underline; }
.foot-links { display: flex; flex-direction: column; align-items: flex-end; gap: 5px; }
.remote-link { color: var(--violet); }

.copyright { position: relative; z-index: 1; margin-top: 22px; }
</style>
