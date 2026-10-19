<template>
  <div class="auth mesh noise">
    <ParticleField class="bg-fx" />
    <ThemeToggle class="auth-theme" />

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
import ThemeToggle from '../components/ThemeToggle.vue'

export default {
  name: "Login",
  components: { ParticleField, ThemeToggle },
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
.auth-theme { position: absolute; top: 24px; right: 24px; z-index: 3; }
.bg-fx { z-index: 0; }

.card {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 32px 32px 22px;
  background: var(--auth-card);
  backdrop-filter: saturate(180%) blur(24px);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  border: 1px solid var(--auth-border);
  border-radius: 6px;
  box-shadow: var(--shadow-lg);
}
.card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 6px;
  padding: 1px;
  background: linear-gradient(150deg, rgba(94, 242, 214, .55), rgba(88, 166, 255, .20), transparent 45%, rgba(255, 61, 129, .30));
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.card-head { display: flex; align-items: center; gap: 12px; margin-bottom: 26px; }
.logo {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  background: linear-gradient(135deg, var(--accent), var(--ice));
  color: var(--on-accent);
  font-family: var(--display);
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  clip-path: polygon(25% 0, 75% 0, 100% 50%, 75% 100%, 25% 100%, 0 50%);
  box-shadow: 0 0 28px -6px rgba(94, 242, 214, .65);
}
.card-head h1 { font-family: var(--display); font-size: 20px; font-weight: 700; letter-spacing: -.04em; margin-bottom: 3px; }

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
  border-radius: 3px;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
  color: var(--on-accent);
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: .14em;
  cursor: pointer;
  transition: transform var(--dur-1) var(--ease), box-shadow var(--dur-2) var(--ease), background var(--dur-1);
  box-shadow: 0 0 28px -8px rgba(94, 242, 214, .7);
}
.primary:hover:not(:disabled) { background: linear-gradient(135deg, #8bffe7, #d8fff8); box-shadow: 0 0 36px -6px rgba(94, 242, 214, .85); transform: translateY(-1px); }
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
