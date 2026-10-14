<template>
  <div class="layout">
    <!-- 侧栏 -->
    <aside class="aside">
      <div class="brand">
        <div class="brand-mark">
          <span>W</span>
        </div>
        <div class="brand-text">
          <strong>WORK ADMIN</strong>
          <small>工作管理中台</small>
        </div>
      </div>

      <div class="nav-group u-label">Navigation</div>

      <nav class="nav">
        <router-link
          v-for="(item, i) in menuList"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: $route.path === item.path }"
        >
          <span class="nav-rail"></span>
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span class="nav-title">{{ item.title }}</span>
          <span class="nav-index mono">{{ String(i + 1).padStart(2, '0') }}</span>
        </router-link>
      </nav>

      <div class="aside-foot">
        <div class="status">
          <span class="dot"></span>
          <span class="mono">All systems normal</span>
        </div>
        <div class="u-label">v1.0.0</div>
      </div>
    </aside>

    <!-- 主区 -->
    <div class="main">
      <header class="topbar glass">
        <div class="topbar-left">
          <div class="u-label">{{ $route.path }}</div>
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>

        <div class="topbar-right">
          <span class="role-chip mono">{{ roleLabel }}</span>
          <div class="who">
            <el-avatar :size="30" class="avatar">{{ avatarText }}</el-avatar>
            <div class="who-text">
              <strong>{{ username }}</strong>
              <small class="u-label">Signed in</small>
            </div>
          </div>
          <button class="btn-ghost mono" type="button" @click="handleLogout">EXIT</button>
        </div>
      </header>

      <main class="content mesh noise">
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
        await logoutApi()
      } catch (e) {
        // 忽略退出接口错误
      }
      this.$store.commit("clearUserInfo")
      this.$router.push("/login")
    }
  }
}
</script>

<style scoped>
.layout { display: flex; height: 100vh; overflow: hidden; background: var(--bg); }

/* ---------------- 侧栏 ---------------- */
.aside {
  width: 232px;
  flex-shrink: 0;
  background: var(--bg);
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  padding: 18px 14px 14px;
}
.brand { display: flex; align-items: center; gap: 10px; padding: 0 6px 18px; }
.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(150deg, var(--accent), var(--accent-2));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
  box-shadow: 0 6px 14px -4px rgba(10, 132, 255, .5);
}
.brand-text { display: flex; flex-direction: column; line-height: 1.3; }
.brand-text strong { font-size: 13px; letter-spacing: .04em; }
.brand-text small { font-size: 11px; color: var(--text-3); }

.nav-group { padding: 6px; margin-bottom: 6px; }

.nav { flex: 1; display: flex; flex-direction: column; gap: 2px; overflow-y: auto; }
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 42px;
  padding: 0 10px 0 14px;
  border-radius: var(--r-md);
  color: var(--text-2);
  text-decoration: none;
  font-size: 13.5px;
  transition: background var(--dur-1) var(--ease-2), color var(--dur-1) var(--ease-2);
}
.nav-rail {
  position: absolute;
  left: 4px;
  top: 50%;
  width: 3px;
  height: 0;
  border-radius: 2px;
  background: var(--accent);
  transform: translateY(-50%);
  transition: height var(--dur-2) var(--ease);
}
.nav-item:hover { background: var(--bg-inset); color: var(--text); }
.nav-item.active { background: var(--accent-soft); color: var(--accent); font-weight: 600; }
.nav-item.active .nav-rail { height: 18px; }
.nav-icon { font-size: 16px; }
.nav-title { flex: 1; }
.nav-index { font-size: 10px; color: var(--text-3); opacity: .8; }

.aside-foot {
  border-top: 1px solid var(--line);
  padding-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.status { display: flex; align-items: center; gap: 7px; font-size: 10px; color: var(--text-3); }
.dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--ok);
  box-shadow: 0 0 0 3px rgba(23, 166, 115, .16);
}

/* ---------------- 主区 ---------------- */
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 68px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 26px;
  border-bottom: 1px solid var(--line);
}
.page-title { font-size: 18px; font-weight: 650; letter-spacing: -.01em; margin-top: 2px; }

.topbar-right { display: flex; align-items: center; gap: 18px; }
.role-chip {
  font-size: 10px;
  letter-spacing: .1em;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid var(--accent-line);
  border-radius: 999px;
  padding: 4px 10px;
}
.who { display: flex; align-items: center; gap: 9px; }
.avatar { background: linear-gradient(150deg, var(--accent), var(--accent-2)); color: #fff; font-size: 13px; }
.who-text { display: flex; flex-direction: column; line-height: 1.25; }
.who-text strong { font-size: 13px; font-weight: 600; }

.btn-ghost {
  font-size: 11px;
  letter-spacing: .1em;
  color: var(--text-2);
  background: transparent;
  border: 1px solid var(--line-2);
  border-radius: var(--r-sm);
  padding: 6px 12px;
  cursor: pointer;
  transition: all var(--dur-1) var(--ease-2);
}
.btn-ghost:hover { color: var(--danger); border-color: rgba(229, 72, 77, .4); background: rgba(229, 72, 77, .06); }

.content {
  position: relative;
  flex: 1;
  overflow-y: auto;
  padding: 26px;
}
.content :deep(.el-card) { border: 1px solid var(--line); box-shadow: var(--shadow-xs); }
</style>
