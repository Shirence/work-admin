import { createRouter, createWebHashHistory } from 'vue-router'
import store from '../store'
import Layout from '../views/Layout.vue'
const router = createRouter({
    history: createWebHashHistory(),
    routes: [{
            path: '/login',
            name: 'Login',
            meta: { title: '登录' },
            component: () =>
                import ('../views/Login.vue')
        },
        {
            path: '/register',
            name: 'Register',
            meta: { title: '注册' },
            component: () =>
                import ('../views/Register.vue')
        },
        {
            path: '/',
            component: Layout,
            redirect: '/dashboard',
            children: [{
                    path: 'dashboard',
                    meta: { title: '仪表盘' },
                    component: () =>
                        import ('../views/Dashboard.vue')
                },
                {
                    path: 'user',
                    component: () =>
                        import ('../views/User.vue'),
                    meta: { permission: 'user:read', title: '用户管理' }
                },
                {
                    path: 'roles',
                    component: () =>
                        import ('../views/Roles.vue'),
                    meta: { permission: 'role:read', title: '角色管理' }
                },
                {
                    path: 'menus',
                    component: () =>
                        import ('../views/Menus.vue'),
                    meta: { permission: 'menu:read', title: '菜单管理' }
                },
                {
                    path: 'messages',
                    component: () =>
                        import ('../views/Messages.vue'),
                    meta: { permission: 'message:access', title: 'AI 对话' }
                },
                {
                    path: 'chat',
                    component: () =>
                        import ('../views/Chat.vue'),
                    meta: { permission: 'chat:access', title: '群组聊天' }
                },
                ...[
                    ['tasks', '任务管理', 'task:read'],
                    ['announcements', '公告中心', 'announcement:read'],
                    ['reports', '工作周报', 'report:access']
                ].map(([path, title, permission]) => ({ path, meta: { title, permission, resource: path }, component: () => import('../views/Work.vue') })),
                { path: 'notifications', meta: { title: '通知中心', permission: 'notification:access' }, component: () => import('../views/Notifications.vue') },
                {
                    path: 'forbidden',
                    name: 'Forbidden',
                    meta: { title: '访问受限' },
                    component: () => import('../views/Forbidden.vue')
                }
            ]
        },
        { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
    ]
})
router.beforeEach(async to => {
    if (['/login', '/register'].includes(to.path)) return true
    if (!store.state.token) return '/login'
    try {
        await store.dispatch('refreshAccess')
        if (to.meta.permission && !store.getters.can(to.meta.permission)) return '/forbidden'
    } catch {
        return store.state.token ? (to.path === '/forbidden' ? true : '/forbidden') : '/login'
    }
    return true
})
export default router
