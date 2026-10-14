<template>
  <div class="login-page tech-grid">
    <div class="login-box hud">
      <div class="scanline"></div>

      <div class="box-head mono">
        <span class="prompt">&gt;</span>
        <span>AUTH_REQUIRED</span>
        <span class="cursor"></span>
      </div>

      <div class="brand">
        <div class="brand-mark">W</div>
        <h1>工作管理中台</h1>
        <p class="mono">WORK_ADMIN // CONSOLE</p>
      </div>

      <div class="form-wrap">
        <el-form ref="loginFormRef" :model="loginForm" size="large" @submit.prevent>
          <el-form-item>
            <el-input
              v-model="loginForm.username"
              placeholder="USERNAME / 账号"
              clearable
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="PASSWORD / 密码"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <button class="login-btn mono" type="button" :disabled="loading" @click="handleLogin">
            {{ loading ? 'AUTHENTICATING...' : 'EXECUTE LOGIN' }}
          </button>
        </el-form>
      </div>

      <div class="tips mono">
        <div><span class="k">DEMO</span>admin / manager / viewer</div>
        <div><span class="k">PASS</span>123456</div>
      </div>

      <div class="foot mono">
        还没有账号？<router-link to="/register">注册账号 / SIGN UP</router-link>
      </div>
    </div>

    <div class="copyright mono">VUE3 + ELEMENT-PLUS + SPRING-BOOT</div>
  </div>
</template>

<script>
// 第 11 课：登录改为调用 Spring Boot 后端 POST /api/login
import { login as loginApi } from '../api/auth'

export default {
  name: "Login",
  data() {
    return {
      loading: false,
      loginForm: {
        username: "",
        password: ""
      }
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
        // 后端权威数据：token + { username, role, permissions }
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
.login-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--bg-sub);
}
.login-box {
  width: 400px;
  background: var(--bg);
  border: 1px solid var(--line);
  box-shadow: 0 10px 40px rgba(16, 26, 36, .09);
  overflow: hidden;
}
.box-head {
  height: 34px;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 14px;
  background: var(--bg-sub);
  border-bottom: 1px solid var(--line);
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--text-2);
}
.prompt { color: var(--accent); font-weight: 700; }

.brand { text-align: center; padding: 26px 0 18px; }
.brand-mark {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  background: var(--accent);
  color: #fff;
  font-family: var(--mono);
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  clip-path: polygon(0 0, 100% 0, 100% 72%, 72% 100%, 0 100%);
}
.brand h1 { font-size: 20px; color: var(--text); margin-bottom: 6px; letter-spacing: 1px; }
.brand p { font-size: 10px; color: var(--text-3); letter-spacing: 2px; }

.form-wrap { padding: 0 26px; }
.login-btn {
  width: 100%;
  height: 42px;
  background: var(--accent);
  color: #fff;
  border: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 2px;
  transition: background .15s;
}
.login-btn:hover:not(:disabled) { background: #008cc0; }
.login-btn:disabled { opacity: .65; cursor: not-allowed; }

.tips {
  margin: 18px 26px 0;
  padding: 14px 0 0;
  border-top: 1px dashed var(--line);
  font-size: 11px;
  color: var(--text-3);
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.tips .k { display: inline-block; width: 46px; color: var(--accent); }

.foot {
  margin: 14px 26px 20px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
  font-size: 11px;
  color: var(--text-3);
  text-align: center;
}
.foot a { color: var(--accent); text-decoration: none; }
.foot a:hover { text-decoration: underline; }

.copyright { margin-top: 18px; font-size: 10px; color: var(--text-3); letter-spacing: 2px; }
</style>
