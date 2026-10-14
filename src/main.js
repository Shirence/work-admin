import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { House, User, Plus } from '@element-plus/icons-vue'

// 是否启用 MockJS 假数据。默认关闭，直接走 Spring Boot 后端。
// 需要离线演示时，把 .env 里的 VUE_APP_USE_MOCK 设为 true。
if (process.env.VUE_APP_USE_MOCK === 'true') {
  // eslint-disable-next-line global-require
  require('./mock')
}

const app = createApp(App)

// 第 1 课：只注册课程用到的图标，避免把整个图标库打进主包
app.component('House', House)
app.component('User', User)
app.component('Plus', Plus)

app.use(store).use(router).use(ElementPlus).mount('#app')
