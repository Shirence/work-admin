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
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'House', menu: true, roles: ['admin', 'manager', 'viewer'] }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('../views/User.vue'),
        meta: { title: '用户管理', icon: 'User', menu: true, roles: ['admin', 'manager'] }
      }
    ]
  }
]

const router = createRouter({
  // history 模式：URL 不带 #（如 /dashboard）。部署时服务器必须做 SPA 回退，见 deploy/nginx.conf
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// 第 9 课：全局路由守卫 —— 登录校验 + 角色权限控制
router.beforeEach(to => {
  const token = store.state.token

  // 白名单：登录、注册页无需 token；已登录则直接去仪表盘
  const whiteList = ['/login', '/register']
  if (whiteList.includes(to.path)) return token ? '/dashboard' : true

  // 未登录：一律去登录页
  if (!token) return '/login'

  // 角色不在允许列表内：提示并回到仪表盘
  const role = store.state.userInfo?.role
  const allowed = to.meta.roles
  if (Array.isArray(allowed) && !allowed.includes(role)) {
    ElMessage.warning('您没有权限访问该页面')
    return '/dashboard'
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
