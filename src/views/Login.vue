<template>
  <div class="login-page">
    <div class="login-box">
      <div class="brand">
        <div class="brand-mark">W</div>
        <h1>工作管理中台</h1>
        <p>WORK ADMIN CONSOLE</p>
      </div>

      <el-form ref="loginFormRef" :model="loginForm" size="large" @submit.prevent>
        <el-form-item>
          <el-input
            v-model="loginForm.username"
            placeholder="请输入账号"
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>

      <div class="tips">
        <span>演示账号：admin / manager / viewer</span>
        <span>密码：123456</span>
      </div>
    </div>
    <div class="copyright">work-admin · Vue 3 + Element Plus + Spring Boot</div>
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
  background: radial-gradient(circle at 18% 18%, #2b5876 0%, #1f2d3d 65%);
}
.login-box {
  width: 400px;
  padding: 38px 36px 24px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, .3);
}
.brand { text-align: center; margin-bottom: 26px; }
.brand-mark {
  width: 52px;
  height: 52px;
  margin: 0 auto 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, #409eff, #6ab0ff);
  color: #fff;
  font-size: 26px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(64, 158, 255, .45);
}
.brand h1 { font-size: 21px; color: #1f2d3d; margin-bottom: 4px; }
.brand p { font-size: 11px; color: #a0aab8; letter-spacing: 1.5px; }
.login-btn { width: 100%; letter-spacing: 4px; margin-top: 4px; }
.tips {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}
.copyright { margin-top: 22px; color: rgba(255, 255, 255, .35); font-size: 12px; }
</style>
