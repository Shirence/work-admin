<template>
  <div class="auth mesh noise">
    <ParticleField class="bg-fx" />

    <div class="card reveal is-visible">
      <header class="card-head">
        <div class="logo">W</div>
        <div>
          <h1>创建账号</h1>
          <p class="u-label">Create your account</p>
        </div>
      </header>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" class="form" @submit.prevent>
        <div class="field">
          <label class="u-label">用户名</label>
          <el-input v-model="form.username" placeholder="4-20 位字母 / 数字 / 下划线" clearable />
        </div>
        <div class="field">
          <label class="u-label">邮箱</label>
          <el-input v-model="form.email" placeholder="you@example.com" clearable />
        </div>
        <div class="row">
          <div class="field">
            <label class="u-label">密码</label>
            <el-input v-model="form.password" type="password" placeholder="6-32 位，含字母和数字" show-password />
          </div>
          <div class="field">
            <label class="u-label">确认密码</label>
            <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入" show-password @keyup.enter="handleRegister" />
          </div>
        </div>
        <button class="primary mono" type="button" :disabled="loading" @click="handleRegister">
          {{ loading ? 'CREATING...' : 'CREATE ACCOUNT' }}
        </button>
      </el-form>

      <footer class="card-foot">
        <span class="u-label">注册后默认拥有只读权限</span>
        <router-link class="link" to="/login">← 返回登录</router-link>
      </footer>
    </div>

    <p class="u-label copyright">Vue 3 · Element Plus · Spring Boot</p>
  </div>
</template>

<script>
import { register as registerApi } from '../api/auth'
import ParticleField from '../components/ParticleField.vue'

export default {
  name: "Register",
  components: { ParticleField },
  data() {
    const checkUsername = (rule, value, callback) => {
      if (!/^[A-Za-z0-9_]{4,20}$/.test(value || '')) {
        callback(new Error('用户名需为 4-20 位字母、数字或下划线'))
      } else {
        callback()
      }
    }
    const checkPassword = (rule, value, callback) => {
      if (!/^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{6,32}$/.test(value || '')) {
        callback(new Error('密码需 6-32 位，且同时包含字母和数字'))
      } else {
        callback()
      }
    }
    const checkConfirm = (rule, value, callback) => {
      if (value !== this.form.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      form: { username: '', email: '', password: '', confirmPassword: '' },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { validator: checkUsername, trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { validator: checkPassword, trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          { validator: checkConfirm, trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    handleRegister() {
      this.$refs.formRef.validate(valid => {
        if (valid) this.submit()
      })
    },
    async submit() {
      this.loading = true
      try {
        const res = await registerApi({
          username: this.form.username.trim(),
          email: this.form.email.trim(),
          password: this.form.password,
          confirmPassword: this.form.confirmPassword
        })
        this.$message.success(res.msg || '注册成功，请登录')
        this.$router.push('/login')
      } catch (e) {
        this.$message.error(e.message || '注册失败')
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

.card-head { display: flex; align-items: center; gap: 12px; margin-bottom: 22px; }
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

.form { display: flex; flex-direction: column; gap: 14px; }
.field { display: flex; flex-direction: column; gap: 6px; flex: 1; min-width: 0; }
.row { display: flex; gap: 12px; }
.field :deep(.el-input__wrapper) {
  border-radius: var(--r-md);
  box-shadow: 0 0 0 1px var(--line-2) inset;
  transition: box-shadow var(--dur-1) var(--ease-2);
}
.field :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px var(--line-3) inset; }
.field :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--accent) inset, 0 0 0 3px var(--accent-soft); }
.form :deep(.el-form-item) { margin-bottom: 0; }

.primary {
  height: 44px;
  margin-top: 8px;
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
.link { font-size: 12px; color: var(--accent); text-decoration: none; white-space: nowrap; }
.link:hover { text-decoration: underline; }

.copyright { position: relative; z-index: 1; margin-top: 20px; }
</style>
