<template>
  <div class="layout">
    <!-- 侧栏 -->
    <aside class="aside">
      <div class="brand">
        <div class="brand-mark">
          <span>W</span>
          <i></i>
        </div>
        <div class="brand-text">
          <strong>WORK ADMIN</strong>
          <small class="mono">工作管理中台 · NODE 01</small>
        </div>
      </div>

      <div class="nav-group u-label">SYSTEM NAV</div>

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
          <span class="mono">LINK STABLE</span>
        </div>
        <div class="u-label">V1.0.0</div>
      </div>
      <div class="aside-scan" aria-hidden="true"></div>
    </aside>

    <!-- 主区 -->
    <div class="main">
      <header class="topbar glass">
        <div class="topbar-left">
          <div class="u-label topbar-path">{{ $route.path }}</div>
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>

        <div class="topbar-right">
          <ThemeToggle />
          <span class="sys-status mono"><span class="dot"></span>SYS.ONLINE</span>
          <span class="role-chip mono">{{ roleLabel }}</span>
          <div class="who">
            <el-avatar :size="30" class="avatar">{{ avatarText }}</el-avatar>
            <div class="who-text">
              <strong>{{ username }}</strong>
              <small class="u-label">SIGNED IN</small>
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
import ThemeToggle from '../components/ThemeToggle.vue'

export default {
  name: "Layout",
  components: { ThemeToggle },
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
  position: relative;
  width: 248px;
  flex-shrink: 0;
  background: linear-gradient(180deg, var(--surface), var(--bg-sub));
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  padding: 18px 14px 14px;
  overflow: hidden;
  transition: background .35s var(--ease-2);
}
html.dark .aside {
  background: linear-gradient(180deg, rgba(8, 14, 21, .98), rgba(4, 8, 12, .99));
}
.aside::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(var(--grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--grid-line) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: linear-gradient(180deg, #000, transparent 78%);
  -webkit-mask-image: linear-gradient(180deg, #000, transparent 78%);
}
.aside::after {
  content: '';
  position: absolute;
  top: 0;
  right: -1px;
  width: 1px;
  height: 36%;
  background: linear-gradient(180deg, var(--accent), transparent);
  opacity: .5;
}
.aside-scan {
  position: absolute;
  left: 0;
  right: 0;
  top: -30%;
  height: 30%;
  pointer-events: none;
  background: linear-gradient(180deg, transparent, rgba(94, 242, 214, .055), transparent);
  animation: scanDown 12s linear infinite;
}

.brand { position: relative; z-index: 1; display: flex; align-items: center; gap: 11px; padding: 2px 6px 20px; }
.brand-mark {
  position: relative;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--accent), var(--ice));
  color: var(--on-accent);
  font-family: var(--display);
  font-weight: 800;
  font-size: 17px;
  clip-path: polygon(25% 0, 75% 0, 100% 50%, 75% 100%, 25% 100%, 0 50%);
  box-shadow: 0 0 26px -6px rgba(94, 242, 214, .55);
}
.brand-mark i {
  position: absolute;
  inset: 4px;
  border: 1px solid rgba(3, 17, 13, .35);
  clip-path: polygon(25% 0, 75% 0, 100% 50%, 75% 100%, 25% 100%, 0 50%);
}
.brand-text { display: flex; flex-direction: column; line-height: 1.35; min-width: 0; }
.brand-text strong {
  font-family: var(--display);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: .16em;
  white-space: nowrap;
}
.brand-text small {
  font-size: 9px;
  letter-spacing: .1em;
  color: var(--text-3);
  white-space: nowrap;
}

.nav-group { position: relative; z-index: 1; padding: 6px 8px; margin-bottom: 8px; color: var(--text-3); }

.nav { position: relative; z-index: 1; flex: 1; display: flex; flex-direction: column; gap: 3px; overflow-y: auto; }
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  padding: 0 10px 0 16px;
  border: 1px solid transparent;
  border-radius: 3px;
  color: var(--text-2);
  text-decoration: none;
  font-size: 13px;
  transition: background var(--dur-1) var(--ease-2), color var(--dur-1) var(--ease-2), border-color var(--dur-1) var(--ease-2);
}
.nav-rail {
  position: absolute;
  left: 4px;
  top: 50%;
  width: 2px;
  height: 0;
  border-radius: 2px;
  background: var(--accent);
  box-shadow: 0 0 10px var(--accent);
  transform: translateY(-50%);
  transition: height var(--dur-2) var(--ease);
}
.nav-item:hover { background: var(--accent-soft); color: var(--text); border-color: var(--line); }
.nav-item.active {
  background: linear-gradient(90deg, rgba(94, 242, 214, .12), rgba(94, 242, 214, .02));
  border-color: var(--accent-line);
  color: var(--accent);
  font-weight: 600;
}
.nav-item.active .nav-rail { height: 20px; }
.nav-icon { font-size: 16px; }
.nav-title { flex: 1; }
.nav-index { font-size: 9px; color: var(--text-3); opacity: .85; }
.nav-item.active .nav-index { color: var(--accent); opacity: .7; }

.aside-foot {
  position: relative;
  z-index: 1;
  border-top: 1px solid var(--line);
  padding-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.status { display: flex; align-items: center; gap: 7px; font-size: 9.5px; color: var(--text-3); letter-spacing: .08em; }
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 0 3px rgba(94, 242, 214, .12), 0 0 10px rgba(94, 242, 214, .8);
  animation: hudPulse 2.4s ease-in-out infinite;
}

/* ---------------- 主区 ---------------- */
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 72px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid var(--line);
  background: var(--surface-2);
}
.topbar::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--accent-line), transparent);
  opacity: .7;
}
.topbar-left { display: flex; flex-direction: column; gap: 2px; }
.topbar-path {
  color: var(--accent);
  opacity: .8;
  letter-spacing: .18em;
  font-size: 9px;
}
.topbar-path::before { content: '// '; opacity: .5; }
.page-title {
  font-family: var(--display);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -.04em;
}

.topbar-right { display: flex; align-items: center; gap: 16px; }
.sys-status {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 9px;
  letter-spacing: .12em;
  color: var(--text-3);
}
.sys-status .dot { width: 5px; height: 5px; }
.role-chip {
  font-size: 9.5px;
  letter-spacing: .14em;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid var(--accent-line);
  border-radius: 2px;
  padding: 4px 9px;
  box-shadow: 0 0 18px -8px rgba(94, 242, 214, .5);
}
.who { display: flex; align-items: center; gap: 9px; }
.avatar {
  background: linear-gradient(135deg, var(--accent), var(--ice));
  color: var(--on-accent);
  font-family: var(--display);
  font-weight: 700;
  font-size: 12px;
  clip-path: polygon(25% 0, 75% 0, 100% 50%, 75% 100%, 25% 100%, 0 50%);
}
.who-text { display: flex; flex-direction: column; line-height: 1.25; }
.who-text strong { font-size: 12.5px; font-weight: 600; }

.btn-ghost {
  font-size: 10px;
  letter-spacing: .12em;
  color: var(--text-2);
  background: transparent;
  border: 1px solid var(--line-2);
  border-radius: 2px;
  padding: 6px 11px;
  cursor: pointer;
  transition: all var(--dur-1) var(--ease-2);
}
.btn-ghost:hover { color: var(--danger); border-color: rgba(255, 61, 129, .45); background: rgba(255, 61, 129, .06); }

.content {
  position: relative;
  flex: 1;
  overflow-y: auto;
  padding: 28px;
}
.content :deep(.el-card) { border: 1px solid var(--line); box-shadow: var(--shadow-xs); }

@media (max-width: 900px) {
  .aside { width: 76px; padding: 18px 8px 14px; }
  .brand-text, .nav-group, .nav-title, .nav-index, .aside-foot { display: none; }
  .nav-item { justify-content: center; padding: 0; }
  .topbar-right .sys-status, .who-text { display: none; }
}
</style>
