import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import {
  House,
  User,
  UserFilled,
  Plus,
  Search,
  Refresh,
  Clock,
  CircleCheck,
  Bell,
  SwitchButton,
  Menu,
  DataLine
} from '@element-plus/icons-vue'
import reveal from './directives/reveal'

// 是否启用 MockJS 假数据。默认关闭，直接走 Spring Boot 后端。
// 需要离线演示时，把 .env 里的 VUE_APP_USE_MOCK 设为 true。
if (process.env.VUE_APP_USE_MOCK === 'true') {
  // eslint-disable-next-line global-require
  require('./mock')
}

const app = createApp(App)

// 全局注册图标：动态菜单用字符串名（route.meta.icon）也能找到
const icons = {
  House,
  User,
  UserFilled,
  Plus,
  Search,
  Refresh,
  Clock,
  CircleCheck,
  Bell,
  SwitchButton,
  Menu,
  DataLine
}
Object.keys(icons).forEach(name => app.component(name, icons[name]))

// v-reveal：滚动进入视口时淡入
app.directive('reveal', reveal)

app.use(store).use(router).use(ElementPlus).mount('#app')
