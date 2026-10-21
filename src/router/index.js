import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import store from '../store'
import Layout from '../views/Layout.vue'

// 第 10 课：页面组件改为路由懒加载（Layout 是公共骨架，保持静态导入）
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/remote-login',
    name: 'RemoteLogin',
    component: () => import('../views/RemoteLogin.vue')
  },
  {
    path: '/remote-register',
    name: 'RemoteRegister',
    component: () => import('../views/RemoteRegister.vue')
  },
  {
    path: '/remote-chat',
    name: 'RemoteChatRoom',
    component: () => import('../views/RemoteChatRoom.vue'),
    meta: { remoteAuth: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'House', menu: true, permission: 'dashboard:view' }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/Chat.vue'),
        meta: { title: '消息', icon: 'ChatDotRound', menu: true, permission: 'chat:access' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('../views/User.vue'),
        meta: { title: '用户管理', icon: 'User', menu: true, permission: 'user:read' }
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('../views/Roles.vue'),
        meta: { title: '角色管理', icon: 'Avatar', menu: true, permission: 'role:read' }
      },
      {
        path: 'menus',
        name: 'Menus',
        component: () => import('../views/Menus.vue'),
        meta: { title: '菜单管理', icon: 'Menu', menu: true, permission: 'menu:read' }
      },
      {
        path: 'forbidden',
        name: 'Forbidden',
        component: () => import('../views/Forbidden.vue'),
        meta: { title: '无权访问' }
      }
    ]
  }
]

const router = createRouter({
  // history 模式：URL 不带 #（如 /dashboard）。部署时服务器必须做 SPA 回退，见 deploy/nginx.conf
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// 第 9 课：全局路由守卫 —— 登录校验 + 后端权限点控制
router.beforeEach(async to => {
  const token = store.state.token

  // 白名单：登录、注册、远程账号登录/注册页无需本地 token
  const whiteList = ['/login', '/register', '/remote-login', '/remote-register']
  if (whiteList.includes(to.path)) {
    if (to.path === '/remote-login' || to.path === '/remote-register') return true
    return token ? '/dashboard' : true
  }

  // 远程 stu 账号页面使用独立的 remoteToken，不依赖本地 H2 登录态
  if (to.meta.remoteAuth) {
    return localStorage.getItem('remoteToken') ? true : '/remote-login'
  }

  if (!token) return '/login'

  try {
    // 首次进入或权限点不明确时，从后端刷新 access。
    if (!store.state.access.role || to.meta.permission) {
      await store.dispatch('refreshAccess')
    }
    if (to.meta.permission && !store.getters.can(to.meta.permission)) {
      ElMessage.warning('您没有权限访问该页面')
      return to.path === '/forbidden' ? true : '/forbidden'
    }
  } catch (error) {
    if (!store.state.token) return '/login'
    if (to.path !== '/forbidden') return '/forbidden'
  }

  return true
})

// 懒加载 chunk 失败时（例如开发服务器重启、部署后旧页面还引用旧 chunk），
// 自动刷新一次去拿新资源，避免直接白屏并抛出 ChunkLoadError。
router.onError(error => {
  const msg = String(error && error.message ? error.message : error)
  const isChunkError = /Loading chunk .* failed|ChunkLoadError|Failed to fetch dynamically imported module/i.test(msg)
  if (!isChunkError) return
  const KEY = 'chunk-reload-at'
  const last = Number(sessionStorage.getItem(KEY) || 0)
  if (Date.now() - last > 10000) {
    sessionStorage.setItem(KEY, String(Date.now()))
    window.location.reload()
  }
})

export default router
