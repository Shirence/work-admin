import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import {
  House,
  User,
  UserFilled,
  Avatar,
  Menu,
  DataLine,
  ChatDotRound,
  Bell
} from '@element-plus/icons-vue'
import reveal from './directives/reveal'
import { initTheme } from './utils/theme'

// 主题在挂载前初始化，避免闪白/闪黑
initTheme()

const app = createApp(App)

// 全局注册图标：动态菜单用字符串名（route.meta.icon）也能找到
const icons = {
  House,
  User,
  UserFilled,
  Avatar,
  Menu,
  DataLine,
  ChatDotRound,
  Bell
}
Object.keys(icons).forEach(name => app.component(name, icons[name]))

// v-reveal：滚动进入视口时淡入
app.directive('reveal', reveal)

app.use(store).use(router).use(ElementPlus).mount('#app')
