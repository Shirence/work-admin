import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import { installSessionSync } from './store'
//引入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
//element图标
import { icons } from './plugins/icons'
import './styles/theme.css'
import { setUnauthorizedHandler } from './api/request'

// 登录失效时由 request.js 触发，避免 request.js 与 router 相互导入
setUnauthorizedHandler(() => {
  if (router.currentRoute.value.path !== '/login') {
    router.replace('/login')
  }
})

const app = createApp(App)
try { document.documentElement.classList.toggle('dark', localStorage.getItem('work-admin-theme') === 'dark') } catch { /* 存储不可用时保持浅色 */ }
//批量注册所有图标
for (const [key, component] of Object.entries(icons)) {
  app.component(key, component)
}
app.use(store).use(router).use(ElementPlus).mount('#app')
const stopSessionSync = installSessionSync(store)
if (module.hot) module.hot.dispose(stopSessionSync)
