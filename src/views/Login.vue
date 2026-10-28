<template>
  <div class="login-page">
    <!-- ============ 左侧品牌区 ============ -->
    <div class="login-brand">
      <div class="brand-glow glow-1"></div>
      <div class="brand-glow glow-2"></div>
      <div class="brand-grid"></div>

      <div class="brand-inner">
        <div class="brand-logo">
          <span class="mark">
            <el-icon><OfficeBuilding /></el-icon>
          </span>
          <span>工作中台</span>
        </div>

        <h1 class="brand-title">
          一站式<em>工作中台</em>
        </h1>
        <p class="brand-desc">
          统一账号与权限，连接用户管理、群组协作和 AI 对话。
        </p>

        <ul class="brand-points">
          <li>统一账号，一处登录全站通行</li>
          <li>用户、角色与菜单，权限清晰可控</li>
          <li>群组实时沟通，AI 辅助工作与学习</li>
        </ul>
      </div>
    </div>

    <!-- ============ 右侧表单区 ============ -->
    <div class="login-panel">
      <div class="login-box">
        <h2 class="login-title">欢迎回来</h2>
        <p class="login-subtitle">请登录你的账号以继续</p>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          @submit.prevent
        >
          <el-form-item prop="username">
            <el-input
              v-model.trim="loginForm.username"
              placeholder="请输入账号"
              prefix-icon="User" aria-label="登录账号" autocomplete="username" maxlength="100"
              @keyup.enter="login"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock" aria-label="登录密码" autocomplete="current-password" maxlength="100"
              show-password
              @keyup.enter="login"
            />
          </el-form-item>

          <div class="login-options">

            <button type="button" class="link forgot-button" @click="onForgot">忘记密码？</button>
          </div>

          <el-form-item class="submit-item">
            <el-button
              class="login-btn"
              :loading="loading"
              @click="login"
            >
              {{ loading ? '登录中' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <p class="register-entry">还没有账号？<router-link class="link" to="/register">注册普通用户</router-link></p>
        <p class="login-footer">© 2026 工作中台 / 内部系统</p>
        <button class="theme-toggle" type="button" @click="toggleTheme">{{ dark ? '浅色' : '深色' }}</button>
      </div>
    </div>
  </div>
</template>
<script>
import { loginApi } from '../api/auth'

export default {
  name: 'AdminLogin',
  data() {
    return {
      loading: false,
      dark: typeof document !== 'undefined' && document.documentElement.classList.contains('dark'),
      loginForm: { username: '', password: '' },
      rules: {
        username: [
          { required: true, whitespace: true, message: '请输入账号', trigger: 'blur' },
          { max: 100, message: '账号最多 100 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, whitespace: true, message: '请输入密码', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    toggleTheme() {
      this.dark = !this.dark
      document.documentElement.classList.toggle('dark', this.dark)
      try { localStorage.setItem('work-admin-theme', this.dark ? 'dark' : 'light') } catch { /* 忽略存储失败 */ }
    },
    async login() {
      if (this.loading) return
      this.loading = true
      try {
        const valid = await this.$refs.loginFormRef.validate().catch(() => false)
        if (!valid) return
        const result = await loginApi({
          account: this.loginForm.username.trim(),
          passwd: this.loginForm.password
        })
        this.$store.commit('setSession', result)
        this.$message.success('登录成功')
        await this.$router.replace('/dashboard')
      } catch (error) {
        this.$message.error(error.userMessage || '登录失败，请重试')
      } finally {
        this.loading = false
      }
    },
    onForgot() {
      this.$message.info('请联系管理员重置密码')
    }
  }
}
</script>

<style scoped>
.forgot-button { padding:0; border:0; background:transparent; font:inherit; cursor:pointer; margin-left:auto; }
.register-entry { margin-top: 16px; text-align: center; font-size: 14px; color: #737b8c; }
/* ==================== 页面骨架 ==================== */
.login-page {
  display: flex;
  min-height: 100vh;
  background: #f5f7fb;
  overflow: hidden;
}

/* ==================== 左侧品牌区 ==================== */
.login-brand {
  position: relative;
  flex: 1 1 52%;
  display: flex;
  align-items: center;
  padding: 0 clamp(48px, 7vw, 104px);
  overflow: hidden;
  color: #fff;
  background: linear-gradient(145deg, #1d2c6e 0%, #172355 45%, #0c1230 100%);
}

.brand-inner {
  position: relative;
  z-index: 3;
  max-width: 520px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 56px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.brand-logo .mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  font-size: 18px;
  background: linear-gradient(135deg, #5b8bff, #8b5cf6);
  box-shadow: 0 8px 22px rgba(91, 139, 255, 0.45);
}

.brand-title {
  margin: 0 0 18px;
  font-size: 34px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.5px;
}

.brand-title em {
  font-style: normal;
  background: linear-gradient(90deg, #8fb4ff, #c4a7ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.brand-desc {
  margin: 0 0 40px;
  font-size: 15px;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.6);
}

.brand-points {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 16px;
}

.brand-points li {
  display: flex;
  align-items: center;
  gap: 11px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.78);
}

.brand-points li::before {
  content: "\2713";
  display: grid;
  place-items: center;
  flex: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  font-size: 12px;
  color: #a8c1ff;
  background: rgba(120, 160, 255, 0.16);
  border: 1px solid rgba(140, 175, 255, 0.42);
}

/* 背景光斑 */
.brand-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
  pointer-events: none;
}

.glow-1 {
  top: -130px;
  right: -90px;
  width: 470px;
  height: 470px;
  background: radial-gradient(circle, #3b6bff 0%, transparent 70%);
  animation: floatA 13s ease-in-out infinite;
}

.glow-2 {
  bottom: -150px;
  left: -110px;
  width: 430px;
  height: 430px;
  background: radial-gradient(circle, #7c4dff 0%, transparent 70%);
  animation: floatB 16s ease-in-out infinite;
}

@keyframes floatA {
  50% { transform: translate(-32px, 42px); }
}

@keyframes floatB {
  50% { transform: translate(42px, -32px); }
}

/* 网格线 */
.brand-grid {
  position: absolute;
  inset: 0;
  z-index: 2;
  opacity: 0.55;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.055) 1px, transparent 1px);
  background-size: 52px 52px;
  -webkit-mask-image: radial-gradient(ellipse at 62% 40%, #000 0%, transparent 76%);
  mask-image: radial-gradient(ellipse at 62% 40%, #000 0%, transparent 76%);
}

/* ==================== 右侧表单区 ==================== */
.login-panel {
  flex: 1 1 48%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background: #fff;
}

.login-box {
  width: 100%;
  max-width: 380px;
}

.login-title {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
  color: #1a2233;
}

.login-subtitle {
  margin: 0 0 34px;
  font-size: 14px;
  color: #8a93a6;
}

/* ---------- 输入框 ---------- */
:deep(.el-form-item ){
  margin-bottom: 18px;
}

:deep(.el-input__inner ){
  height: 48px;
  line-height: 48px;
  padding-left: 42px;
  border-radius: 12px;
  border-color: #e3e8f2;
  background: #f6f8fc;
  font-size: 14px;
  color: #1a2233;
  transition: all 0.22s ease;
}

:deep(.el-input__inner::placeholder ){
  color: #a9b2c4;
}

:deep(.el-input__inner:hover ){
  border-color: #cdd7ea;
}

:deep(.el-input__inner:focus ){
  border-color: #5b8bff;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(91, 139, 255, 0.14);
}

:deep(.el-input__prefix ){
  left: 14px;
  display: flex;
  align-items: center;
  color: #a3adc2;
  font-size: 16px;
}

:deep(.el-input__suffix ){
  display: flex;
  align-items: center;
}

:deep(.el-input__icon ){
  line-height: 48px;
}

/* ---------- 记住我 / 忘记密码 ---------- */
.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 26px;
}

:deep(.el-checkbox__label ){
  padding-left: 8px;
  font-size: 13px;
  color: #5f6b82;
}

:deep(.el-checkbox__inner ){
  width: 16px;
  height: 16px;
  border-radius: 5px;
  border-color: #d3dae8;
}

:deep(.el-checkbox__inner::after ){
  left: 5px;
  top: 2px;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner ){
  border-color: transparent;
  background: linear-gradient(135deg, #5b8bff, #7c5cff);
}

:deep(.el-checkbox__input.is-checked + .el-checkbox__label ){
  color: #5f6b82;
}

.link {
  font-size: 13px;
  color: #5b8bff;
  cursor: pointer;
  transition: color 0.2s;
}

.link:hover {
  color: #3f6ff0;
}

/* ---------- 登录按钮 ---------- */
.submit-item {
  margin-bottom: 0;
}

.login-btn {
  width: 100%;
  height: 48px;
  padding: 0;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  color: #fff;
  background: linear-gradient(135deg, #4d7dff 0%, #7c5cff 100%);
  box-shadow: 0 10px 24px -8px rgba(80, 110, 255, 0.65);
  transition: all 0.22s ease;
}

.login-btn:hover,
.login-btn:focus {
  color: #fff;
  border: none;
  background: linear-gradient(135deg, #4373ff 0%, #7252ff 100%);
  box-shadow: 0 14px 28px -8px rgba(80, 110, 255, 0.78);
  transform: translateY(-1px);
}

.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 8px 18px -8px rgba(80, 110, 255, 0.7);
}

.login-footer {
  margin: 34px 0 0;
  text-align: center;
  font-size: 12px;
  color: #b3bbcc;
}

/* ==================== 响应式 ==================== */
@media (max-width: 900px) {
  .login-brand {
    display: none;
  }

  .login-panel {
    background: linear-gradient(165deg, #eef2fb 0%, #ffffff 60%);
  }

  .login-title {
    font-size: 24px;
  }
}
.theme-toggle {
  position: fixed;
  top: 20px;
  right: 24px;
  z-index: 10;
  padding: 8px 14px;
  border: 1px solid #dce5f0;
  border-radius: 8px;
  color: #4e6480;
  background: #ffffffcc;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}
html.dark .theme-toggle { color: #cbd9e6; background: #1b2937cc; border-color: #334759; }
</style>
