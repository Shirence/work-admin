<template>
  <div class="auth mesh noise">
    <ParticleField class="bg-fx" />

    <div class="card reveal is-visible">
      <header class="card-head">
        <div class="logo">S</div>
        <div>
          <h1>注册远程账号</h1>
          <p class="u-label">STU Remote Register</p>
        </div>
      </header>

      <div class="remote-note">
        <span class="dot"></span>
        <span>注册会直接写入远程 MySQL <code>user</code> 表并绑定 <code>member</code> 角色，注册成功自动登录。</span>
      </div>

      <el-form :model="form" size="large" class="form" @submit.prevent>
        <div class="field">
          <label class="u-label">昵称（可选）</label>
          <el-input v-model="form.name" placeholder="显示名称，留空则使用账号" clearable @keyup.enter="handleRegister" />
        </div>
        <div class="field">
          <label class="u-label">远程账号</label>
          <el-input v-model="form.account" placeholder="2-100 位，不能包含空白" clearable @keyup.enter="handleRegister" />
        </div>
        <div class="field">
          <label class="u-label">密码</label>
          <el-input v-model="form.password" type="password" placeholder="6-32 位密码" show-password @keyup.enter="handleRegister" />
        </div>
        <div class="field">
          <label class="u-label">确认密码</label>
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" show-password @keyup.enter="handleRegister" />
        </div>

        <p v-if="errorText" class="error-text">{{ errorText }}</p>

        <button class="primary mono" type="button" :disabled="loading" @click="handleRegister">
          {{ loading ? 'CREATING...' : 'CREATE ACCOUNT' }}
        </button>
      </el-form>

      <footer class="card-foot">
        <span class="u-label">注册后自动绑定 member 角色</span>
        <router-link class="link" to="/remote-login">← 返回登录</router-link>
      </footer>
    </div>

    <p class="u-label copyright">远程账号与本地 H2 账号相互独立</p>
  </div>
</template>

<script>
import { remoteRegister } from '../api/remoteChat'
import ParticleField from '../components/ParticleField.vue'

export default {
  name: 'RemoteRegister',
  components: { ParticleField },
  data() {
    return {
      loading: false,
      errorText: '',
      form: {
        name: '',
        account: '',
        password: '',
        confirmPassword: ''
      }
    }
  },
  methods: {
    validate() {
      const account = this.form.account.trim()
      if (!account) {
        this.errorText = '请输入远程账号'
        return false
      }
      if (account.length < 2) {
        this.errorText = '远程账号至少 2 个字符'
        return false
      }
      if (/\s/.test(account)) {
        this.errorText = '远程账号不能包含空白字符'
        return false
      }
      if (this.form.password.length < 6 || this.form.password.length > 32) {
        this.errorText = '密码长度必须为 6-32 个字符'
        return false
      }
      if (this.form.password !== this.form.confirmPassword) {
        this.errorText = '两次输入的密码不一致'
        return false
      }
      return true
    },

    async handleRegister() {
      if (this.loading || !this.validate()) return
      this.loading = true
      this.errorText = ''
      try {
        const res = await remoteRegister({
          name: this.form.name.trim() || undefined,
          account: this.form.account.trim(),
          password: this.form.password,
          confirmPassword: this.form.confirmPassword
        })
        localStorage.setItem('remoteToken', res.data.token)
        localStorage.setItem('remoteUserInfo', JSON.stringify(res.data.userInfo))
        this.$message.success(res.msg || '远程账号注册成功')
        this.$router.push('/remote-chat')
      } catch (e) {
        this.errorText = e.message || '远程注册失败'
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
  background: rgba(255, 255, 255, .84);
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
  background: linear-gradient(150deg, rgba(124, 92, 255, .45), rgba(10, 132, 255, .18), transparent 60%);
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.card-head { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; }
.logo {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  background: linear-gradient(150deg, var(--violet), var(--accent));
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 18px -6px rgba(124, 92, 255, .55);
}
.card-head h1 { font-size: 18px; font-weight: 650; letter-spacing: -.01em; margin-bottom: 3px; }

.remote-note {
  display: flex;
  gap: 9px;
  align-items: flex-start;
  padding: 10px 12px;
  margin-bottom: 18px;
  border: 1px solid var(--line);
  background: var(--bg-sub);
  border-radius: var(--r-md);
  color: var(--text-2);
  font-size: 11.5px;
  line-height: 1.6;
}
.remote-note code { font-family: var(--mono); font-size: 11px; color: var(--violet); }
.dot {
  width: 7px;
  height: 7px;
  margin-top: 5px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--violet);
  box-shadow: 0 0 0 3px rgba(124, 92, 255, .14);
}

.form { display: flex; flex-direction: column; gap: 14px; }
.field { display: flex; flex-direction: column; gap: 7px; }
.field :deep(.el-input__wrapper) {
  border-radius: var(--r-md);
  box-shadow: 0 0 0 1px var(--line-2) inset;
  transition: box-shadow var(--dur-1) var(--ease-2);
}
.field :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px var(--line-3) inset; }
.field :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--violet) inset, 0 0 0 3px rgba(124, 92, 255, .10); }

.error-text { color: var(--danger); font-size: 11.5px; margin-top: -2px; }

.primary {
  height: 44px;
  margin-top: 4px;
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
.link { font-size: 12px; color: var(--violet); text-decoration: none; white-space: nowrap; }
.link:hover { text-decoration: underline; }
.copyright { position: relative; z-index: 1; margin-top: 22px; }
</style>
