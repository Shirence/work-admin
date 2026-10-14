<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 style="text-align:center;margin-bottom:20px">工作中台登录</h2>
      <el-form ref="loginFormRef" :model="loginForm" label-width="80px" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="loginForm.username" placeholder="admin / manager / viewer" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="123456" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width:100%">登录</el-button>
        </el-form-item>
      </el-form>
      <p class="tips">演示账号：admin / manager / viewer，密码均为 123456</p>
    </el-card>
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
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
}
.login-card {
  width: 420px;
}
.tips {
  font-size: 12px;
  color: #909399;
  text-align: center;
}
</style>
