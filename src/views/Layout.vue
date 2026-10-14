<template>
  <div class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <div class="logo-mark">W</div>
        <div class="logo-text">
          <span class="logo-title">工作管理中台</span>
          <span class="logo-sub">WORK ADMIN</span>
        </div>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        class="side-menu"
        background-color="transparent"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in menuList" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">v1.0.0 · 教学演示</div>
    </el-aside>

    <div class="main-wrap">
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag class="role-tag" size="small" round effect="light">{{ roleLabel }}</el-tag>
          <el-avatar :size="30" class="avatar">{{ avatarText }}</el-avatar>
          <span class="username">{{ username }}</span>
          <el-button link class="logout-btn" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出</span>
          </el-button>
        </div>
      </el-header>

      <el-main class="content">
        <router-view />
      </el-main>
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
      const map = { admin: '管理员', manager: '主管', viewer: '访客' }
      return map[this.userRole] || '用户'
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
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ---------------- 侧边栏 ---------------- */
.aside {
  background: linear-gradient(180deg, #2b3a4d 0%, #1f2d3d 100%);
  display: flex;
  flex-direction: column;
}
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, .08);
}
.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(135deg, #409eff, #6ab0ff);
  color: #fff;
  font-weight: 700;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(64, 158, 255, .45);
}
.logo-text { display: flex; flex-direction: column; line-height: 1.25; }
.logo-title { color: #fff; font-size: 15px; font-weight: 600; }
.logo-sub { color: #7f8fa4; font-size: 10px; letter-spacing: 1.5px; }

.side-menu {
  flex: 1;
  border-right: none;
  padding: 10px 0;
}
.side-menu :deep(.el-menu-item) {
  height: 46px;
  margin: 4px 10px;
  border-radius: 8px;
}
.side-menu :deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, .08) !important;
}
.side-menu :deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
  box-shadow: 0 4px 12px rgba(64, 158, 255, .4);
}
.aside-footer {
  padding: 14px 18px;
  color: #5d6b7c;
  font-size: 11px;
  border-top: 1px solid rgba(255, 255, 255, .06);
}

/* ---------------- 右侧 ---------------- */
.main-wrap { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.header {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, .08);
  z-index: 10;
}
.page-title { font-size: 17px; font-weight: 600; color: #1f2d3d; }
.header-right { display: flex; align-items: center; gap: 10px; }
.role-tag { border: none; }
.avatar {
  background: linear-gradient(135deg, #409eff, #6ab0ff);
  color: #fff;
  font-size: 13px;
}
.username { font-size: 14px; color: #303133; }
.logout-btn { display: flex; align-items: center; gap: 4px; color: #606266; }
.logout-btn:hover { color: #409eff; }

.content {
  flex: 1;
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
