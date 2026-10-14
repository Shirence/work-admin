import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { House, User, Plus } from '@element-plus/icons-vue'

// 第 4 课：引入 Mock，拦截 /api 开头的请求。
// 第 10 课接入真实后端时删除这一行并重新构建。
import './mock'

const app = createApp(App)

// 第 1 课：只注册课程用到的图标，避免把整个图标库打进主包
app.component('House', House)
app.component('User', User)
app.component('Plus', Plus)

app.use(store).use(router).use(ElementPlus).mount('#app')
