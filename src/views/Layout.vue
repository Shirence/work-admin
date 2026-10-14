<template>
  <div class="layout">
    <!-- 左侧导航 -->
    <aside class="aside">
      <div class="logo">
        <div class="logo-mark">W</div>
        <div class="logo-text">
          <span class="logo-title mono">WORK_ADMIN</span>
          <span class="logo-sub">工作管理中台</span>
        </div>
      </div>

      <div class="nav-label mono">// NAVIGATION</div>

      <nav class="nav">
        <router-link
          v-for="(item, i) in menuList"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: $route.path === item.path }"
        >
          <span class="nav-index mono">{{ String(i + 1).padStart(2, '0') }}</span>
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span class="nav-title">{{ item.title }}</span>
        </router-link>
      </nav>

      <div class="aside-footer">
        <div class="status-line mono"><span class="dot ok"></span>SYS.ONLINE</div>
        <div class="ver mono">v1.0.0 · 教学演示</div>
      </div>
    </aside>

    <!-- 右侧主体 -->
    <div class="main-wrap">
      <header class="header">
        <div class="header-left">
          <span class="crumb mono">/ {{ $route.path }}</span>
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <span class="role mono">[ {{ roleLabel }} ]</span>
          <div class="user">
            <el-avatar :size="28" class="avatar">{{ avatarText }}</el-avatar>
            <span class="username mono">{{ username }}</span>
          </div>
          <button class="logout mono" type="button" @click="handleLogout">EXIT</button>
        </div>
      </header>

      <main class="content tech-grid">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script>
import { logout as logoutApi } from '../api/auth'

export default {
  name: "Layout",
  computed: {
    username() {
      return this.$store.state.userInfo?.username || ''
    },
    userRole() {
      return this.$store.state.userInfo?.role
    },
    roleLabel() {
      const map = { admin: 'ADMIN', manager: 'MANAGER', viewer: 'VIEWER' }
      return map[this.userRole] || 'USER'
    },
    avatarText() {
      return (this.username.charAt(0) || 'U').toUpperCase()
    },
    pageTitle() {
      return (this.$route.meta && this.$route.meta.title) || ''
    },
    menuList() {
      return this.$router
        .getRoutes()
        .filter(route => route.meta.menu)
        .filter(route => !route.meta.roles || route.meta.roles.includes(this.userRole))
        .map(route => ({ path: route.path, title: route.meta.title, icon: route.meta.icon }))
    }
  },
  methods: {
    async handleLogout() {
      try {
        await logoutApi()   // 通知后端作废 token（失败也不影响本地退出）
      } catch (e) {
        // 忽略退出接口的错误
      }
      this.$store.commit("clearUserInfo")
      this.$router.push("/login")
    }
  }
}
</script>

<style scoped>
.layout { display: flex; height: 100vh; overflow: hidden; }

/* ---------------- 侧栏 ---------------- */
.aside {
  width: 220px;
  flex-shrink: 0;
  background: var(--bg);
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  position: relative;
}
.aside::after {
  content: '';
  position: absolute;
  top: 0;
  right: -1px;
  width: 2px;
  height: 64px;
  background: var(--accent);
}
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid var(--line);
}
.logo-mark {
  width: 32px;
  height: 32px;
  background: var(--accent);
  color: #fff;
  font-family: var(--mono);
  font-weight: 700;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  clip-path: polygon(0 0, 100% 0, 100% 72%, 72% 100%, 0 100%);
}
.logo-text { display: flex; flex-direction: column; line-height: 1.35; }
.logo-title { font-size: 12px; font-weight: 700; letter-spacing: 1px; color: var(--text); }
.logo-sub { font-size: 11px; color: var(--text-3); }

.nav-label {
  padding: 16px 16px 8px;
  font-size: 10px;
  color: var(--text-3);
  letter-spacing: 1px;
}
.nav { flex: 1; padding: 0 8px; overflow-y: auto; }
.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 42px;
  padding: 0 10px;
  margin-bottom: 4px;
  color: var(--text-2);
  text-decoration: none;
  font-size: 13px;
  border: 1px solid transparent;
  border-left: 2px solid transparent;
  transition: all .15s;
}
.nav-item:hover { background: var(--bg-sub); color: var(--text); }
.nav-item.active {
  background: var(--accent-soft);
  border-color: rgba(0, 166, 224, .25);
  border-left: 2px solid var(--accent);
  color: var(--accent);
  font-weight: 600;
}
.nav-index { font-size: 10px; opacity: .55; }
.nav-icon { font-size: 15px; }
.nav-title { flex: 1; }

.aside-footer { border-top: 1px solid var(--line); padding: 12px 16px; }
.status-line {
  font-size: 10px;
  color: var(--text-3);
  display: flex;
  align-items: center;
  gap: 6px;
  letter-spacing: .5px;
}
.dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; }
.dot.ok { background: var(--ok); box-shadow: 0 0 0 3px rgba(38, 179, 107, .15); }
.ver { font-size: 10px; color: var(--text-3); margin-top: 5px; }

/* ---------------- 右侧 ---------------- */
.main-wrap { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.header {
  height: 64px;
  flex-shrink: 0;
  background: var(--bg);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.header-left { display: flex; flex-direction: column; gap: 1px; }
.crumb { font-size: 10px; color: var(--text-3); letter-spacing: .5px; }
.page-title { font-size: 16px; font-weight: 600; color: var(--text); }
.header-right { display: flex; align-items: center; gap: 14px; }
.role { font-size: 11px; color: var(--accent); letter-spacing: 1px; }
.user { display: flex; align-items: center; gap: 8px; }
.avatar { background: var(--accent); color: #fff; font-size: 12px; border-radius: 2px; }
.username { font-size: 12px; color: var(--text-2); }
.logout {
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--text-2);
  background: transparent;
  border: 1px solid var(--line-2);
  padding: 5px 12px;
  cursor: pointer;
  transition: all .15s;
}
.logout:hover { color: #fff; background: var(--danger); border-color: var(--danger); }

.content { flex: 1; overflow-y: auto; padding: 18px; background: var(--bg-sub); }
</style>
