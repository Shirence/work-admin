<script>
import { registerApi } from '../api/auth'

export default {
  name: 'AccountRegister',
  data: () => ({
    form: { name: '', account: '', passwd: '' },
    busy: false,
    dark: typeof document !== 'undefined' && document.documentElement.classList.contains('dark')
  }),
  methods: {
    toggleTheme() {
      this.dark = !this.dark
      document.documentElement.classList.toggle('dark', this.dark)
      try { localStorage.setItem('work-admin-theme', this.dark ? 'dark' : 'light') } catch { /* 忽略存储失败 */ }
    },
    async register() {
      if (this.busy) return
      if (!this.form.account.trim() || !this.form.passwd.trim()) {
        return this.$message.warning('请填写账号和密码')
      }
      this.busy = true
      try {
        const result = await registerApi({
          name: this.form.name.trim(),
          account: this.form.account.trim(),
          passwd: this.form.passwd
        })
        this.$store.commit('setSession', result)
        this.$message.success('注册成功')
        await this.$router.replace('/dashboard')
      } catch (error) {
        this.$message.error(error.userMessage || '注册失败')
      } finally {
        this.busy = false
      }
    }
  }
}
</script>

<template>
  <main class="register-page">
    <section class="register-card" aria-labelledby="register-title">
      <h2 id="register-title">注册普通用户</h2>
      <p class="register-subtitle">创建账号，开始使用工作中台</p>
      <el-form :model="form" label-position="top" :disabled="busy" @submit.prevent="register">
        <el-form-item label="姓名（选填）">
          <el-input v-model="form.name" maxlength="100" placeholder="请输入姓名" autocomplete="name" />
        </el-form-item>
        <el-form-item label="登录账号" required>
          <el-input v-model="form.account" maxlength="100" placeholder="请输入登录账号" autocomplete="username" />
        </el-form-item>
        <el-form-item label="登录密码" required>
          <el-input
            v-model="form.passwd"
            maxlength="100"
            type="password"
            show-password
            placeholder="请输入登录密码"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-button class="register-button" type="primary" native-type="submit" :loading="busy">
          {{ busy ? '注册中…' : '注册并登录' }}
        </el-button>
      </el-form>
      <p class="register-footer">已有账号？<router-link to="/login">返回登录</router-link></p>
    </section>
    <button class="theme-toggle" type="button" @click="toggleTheme">{{ dark ? '浅色' : '深色' }}</button>
  </main>
</template>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
  padding: 60px 20px;
  background: #f5f7fb;
}
.register-card {
  width: 100%;
  max-width: 400px;
  padding: 32px;
  border: 1px solid #e6eaf2;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(29, 44, 110, 0.06);
}
.register-card h2 { color: #172355; }
.register-subtitle { margin: 12px 0 28px; color: #737b8c; font-size: 14px; }
.register-button { width: 100%; margin-top: 8px; }
.register-footer { margin-top: 24px; text-align: center; color: #737b8c; font-size: 14px; }
.register-footer a { color: #409eff; text-decoration: none; }
.theme-toggle {
  position: fixed; top: 20px; right: 24px; z-index: 10; padding: 8px 14px;
  border: 1px solid #dce5f0; border-radius: 8px; color: #4e6480;
  background: #ffffffcc; font: inherit; font-size: 12px; cursor: pointer;
}
html.dark .theme-toggle { color: #cbd9e6; background: #1b2937cc; border-color: #334759; }
</style>
