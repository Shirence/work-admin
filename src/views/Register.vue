<template>
  <div class="login-page tech-grid">
    <div class="login-box hud">
      <div class="scanline"></div>

      <div class="box-head mono">
        <span class="prompt">&gt;</span>
        <span>NEW_ACCOUNT_SETUP</span>
        <span class="cursor"></span>
      </div>

      <div class="brand">
        <div class="brand-mark">W</div>
        <h1>注册账号</h1>
        <p class="mono">WORK_ADMIN // SIGN UP</p>
      </div>

      <div class="form-wrap">
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="USERNAME / 用户名" clearable />
          </el-form-item>
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="EMAIL / 邮箱" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="PASSWORD / 密码" show-password />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="CONFIRM / 确认密码"
              show-password
              @keyup.enter="handleRegister"
            />
          </el-form-item>
          <button class="login-btn mono" type="button" :disabled="loading" @click="handleRegister">
            {{ loading ? 'SUBMITTING...' : 'EXECUTE REGISTER' }}
          </button>
        </el-form>
      </div>

      <div class="tips mono">
        <div><span class="k">USER</span>4-20 位字母 / 数字 / 下划线</div>
        <div><span class="k">PASS</span>6-32 位，需同时含字母和数字</div>
      </div>

      <div class="foot mono">
        已有账号？<router-link to="/login">返回登录 / SIGN IN</router-link>
      </div>
    </div>

    <div class="copyright mono">VUE3 + ELEMENT-PLUS + SPRING-BOOT</div>
  </div>
</template>

<script>
import { register as registerApi } from '../api/auth'

export default {
  name: "Register",
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
      form: {
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
      },
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

.brand { text-align: center; padding: 22px 0 14px; }
.brand-mark {
  width: 44px;
  height: 44px;
  margin: 0 auto 10px;
  background: var(--accent);
  color: #fff;
  font-family: var(--mono);
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  clip-path: polygon(0 0, 100% 0, 100% 72%, 72% 100%, 0 100%);
}
.brand h1 { font-size: 19px; color: var(--text); margin-bottom: 5px; letter-spacing: 1px; }
.brand p { font-size: 10px; color: var(--text-3); letter-spacing: 2px; }

.form-wrap { padding: 0 26px; }
.login-box :deep(.el-form-item) { margin-bottom: 16px; }
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
  margin: 16px 26px 0;
  padding: 12px 0 0;
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
