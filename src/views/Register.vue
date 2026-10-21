<template>
  <div class="auth mesh noise">
    <ParticleField class="bg-fx" />
    <ThemeToggle class="auth-theme" />

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
          <label class="u-label">账号</label>
          <el-input v-model="form.username" placeholder="4-20 位字母 / 数字 / 下划线" clearable />
        </div>
        <div class="field">
          <label class="u-label">姓名</label>
          <el-input v-model="form.name" placeholder="可留空，默认与账号相同" clearable />
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
        <span class="u-label">注册即写入 stu 的 user 表，默认 member 角色</span>
        <router-link class="link" to="/login">← 返回登录</router-link>
      </footer>
    </div>

    <p class="u-label copyright">Vue 3 · Element Plus · Spring Boot</p>
  </div>
</template>

<script>
import { register as registerApi } from '../api/auth'
import ParticleField from '../components/ParticleField.vue'
import ThemeToggle from '../components/ThemeToggle.vue'

export default {
  name: "Register",
  components: { ParticleField, ThemeToggle },
  data() {
    const checkUsername = (rule, value, callback) => {
      if (!/^[A-Za-z0-9_]{4,20}$/.test(value || '')) {
        callback(new Error('账号需为 4-20 位字母、数字或下划线'))
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
      form: { username: '', name: '', password: '', confirmPassword: '' },
      rules: {
        username: [
          { required: true, message: '请输入账号', trigger: 'blur' },
          { validator: checkUsername, trigger: 'blur' }
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
          name: this.form.name.trim(),
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
.auth-theme { position: absolute; top: 24px; right: 24px; z-index: 3; }
.bg-fx { z-index: 0; }

.card {
  position: relative;
  z-index: 1;
  width: 440px;
  padding: 30px 32px 22px;
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

.card-head { display: flex; align-items: center; gap: 12px; margin-bottom: 22px; }
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
