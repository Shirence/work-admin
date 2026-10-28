<script>
import { logoutApi } from '../api/auth'
import { navigation } from '../utils/menu'
import AppNavigation from '../components/layout/AppNavigation.vue'

export default {
  name: 'AdminLayout',
  components: { AppNavigation },
  data: () => ({ refreshing: false, leaving: false, navOpen: false, dark: typeof document !== 'undefined' && document.documentElement.classList.contains('dark') }),
  computed: {
    user() { return this.$store.state.userInfo || {} },
    menus() { return navigation(this.$store.state.access.menus) },
    pageTitle() { return this.$route.meta?.title || '工作空间' },
    avatar() { return Array.from(this.user.name || this.user.account || '用户')[0] }
  },
  watch: { '$route.path'() { this.navOpen = false } },
  methods: {
    toggleTheme() {
      this.dark = !this.dark
      document.documentElement.classList.toggle('dark', this.dark)
      try { localStorage.setItem('work-admin-theme', this.dark ? 'dark' : 'light') } catch { /* 忽略存储失败 */ }
    },
    focusContent() { this.$refs.contentRef?.$el?.focus() },
    async refresh() {
      if (this.refreshing) return
      this.refreshing = true
      try {
        await this.$store.dispatch('refreshAccess')
        if (this.$route.meta.permission && !this.$store.getters.can(this.$route.meta.permission)) {
          await this.$router.replace('/forbidden')
        }
      } catch { /* 请求层统一提示错误。 */ }
      finally { this.refreshing = false }
    },
    async logout() {
      if (this.leaving) return
      this.leaving = true
      const token = this.$store.state.token
      try { await logoutApi() }
      catch { /* 请求失败也退出当前本地会话。 */ }
      finally {
        try {
          if (token === this.$store.state.token) {
            this.$store.commit('clearUserInfo')
            await this.$router.replace('/login')
          }
        } finally { this.leaving = false }
      }
    }
  }
}
</script>

<template>
  <el-container class="layout">
    <a class="skip-link" href="#main-content" @click.prevent="focusContent">跳转到主要内容</a>
    <el-aside width="236px" class="aside">
      <div class="logo"><span class="brand-mark" aria-hidden="true">W</span><div><strong>工作管理中台</strong><small>WORK ADMIN</small></div></div>
      <AppNavigation :menus="menus" :active-path="$route.path" />
    </el-aside>
    <el-container class="main-wrap">
      <el-header class="header">
        <div class="header-context">
          <button class="navigation-toggle" type="button" aria-label="打开导航菜单" :aria-expanded="navOpen" aria-controls="mobile-navigation" @click="navOpen = true"><el-icon><Menu /></el-icon></button>
          <span class="workspace-label">工作空间</span><span class="breadcrumb-divider" aria-hidden="true">/</span><strong class="current-page">{{ pageTitle }}</strong>
        </div>
        <div class="header-actions">
          <div class="account" :title="`欢迎，${user.name || user.account || '用户'} / ${$store.state.access.role?.name || '未分配角色'}`">
            <span class="account-avatar" aria-hidden="true">{{ avatar }}</span>
            <div class="account-copy"><span>欢迎，{{ user.name || user.account || '用户' }}</span><small>{{ $store.state.access.role?.name || '未分配角色' }}</small></div>
          </div>
          <el-button :loading="refreshing" @click="refresh">刷新权限</el-button>
          <el-button class="theme-button" :aria-pressed="dark" @click="toggleTheme">{{ dark ? '浅色' : '深色' }}</el-button>
          <el-button class="logout-button" :loading="leaving" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main ref="contentRef" id="main-content" class="content" tabindex="-1"><router-view :key="$store.state.token" /></el-main>
    </el-container>
    <el-drawer id="mobile-navigation" v-model="navOpen" title="工作管理中台" direction="ltr" size="268px" class="app-mobile-navigation" append-to-body>
      <AppNavigation :menus="menus" :active-path="$route.path" @navigate="navOpen = false" />
    </el-drawer>
  </el-container>
</template>

<style scoped>
.layout { display:flex; height:100dvh; overflow:hidden; }
.aside { display:flex; flex-direction:column; flex-shrink:0; background:linear-gradient(180deg,#1d304a,#14263c); border-right:1px solid #102137; overflow:auto; }
.logo { display:flex; align-items:center; gap:11px; padding:25px 24px 18px; color:#fff; }
.brand-mark { display:grid; place-items:center; flex-shrink:0; width:35px; height:35px; border-radius:10px; font-weight:750; font-size:20px; background:linear-gradient(145deg,#6a9aff,#4673de); box-shadow:0 4px 12px #051a4930; }
.logo strong { display:block; font-size:15px; letter-spacing:.5px; }
.logo small { display:block; margin-top:5px; color:#7992b2; font-size: 12px; letter-spacing:2px; }
.sidebar-footer { margin-top:auto; padding:20px 28px; color:#607a98; font-size: 12px; letter-spacing:1px; }
.main-wrap { flex:1; min-width:0; display:flex; flex-direction:column; }
.header { display:flex; justify-content:space-between; align-items:center; flex-shrink:0; gap:16px; padding:0 28px; height:var(--app-header-height); background:#ffffffee; border-bottom:1px solid var(--app-border); }
.header-context { display:flex; align-items:center; gap:12px; min-width:0; }
.workspace-label { color:#91a0b2; font-size:12px; white-space:nowrap; }
.breadcrumb-divider { color:#c1cbd7; font-size:12px; }
.current-page { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:#30435b; font-size:14px; font-weight:600; }
.header-actions { display:flex; align-items:center; flex-shrink:0; gap:8px; }
.header-actions .el-button + .el-button { margin-left:0; }
.account { display:flex; gap:9px; align-items:center; margin-right:14px; }
.account-avatar { display:grid; place-items:center; flex-shrink:0; width:32px; height:32px; color:#4476c5; background:#eaf1fd; border:1px solid #dfe9f9; border-radius:10px; font-size:13px; font-weight:600; }
.account-copy span { display:block; max-width:160px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:12px; color:#455871; }
.account-copy small { display:block; margin-top:3px; font-size: 12px; color:#95a0af; }
.logout-button { color:#7d8b9e; }
.content { padding:var(--app-content-gap); min-width:0; min-height:0; overflow:auto; background:var(--app-background); }
.navigation-toggle { display:none; place-items:center; width:30px; height:32px; padding:0; border:0; background:transparent; color:#3d5570; border-radius:6px; font-size:20px; cursor:pointer; }
.skip-link { position:fixed; left:16px; top:-80px; z-index:3000; padding:10px 16px; border-radius:6px; background:#fff; color:var(--app-primary); box-shadow:0 4px 16px #15294320; }
.skip-link:focus { top:12px; }
@media(max-width:1000px) { .workspace-label,.breadcrumb-divider { display:none; }.account { margin-right:4px; } }
@media(max-width:768px) { .aside { display:none; }.navigation-toggle { display:grid; }.header { padding:0 14px; gap:8px; }.header-context { gap:8px; }.account-copy { display:none; }.header-actions { gap:6px; }.header-actions .el-button { padding:8px 10px; font-size:12px; }.account { margin-right:0; } }
@media(max-width:380px) { .account { display:none; } }
</style>
