# work-admin 工作管理中台

Vue 3（选项式 API，纯 JavaScript）+ Element Plus + Vuex + Vue Router + Axios + ECharts。

配套后端：[`server`](./server)（Spring Boot 3.5 + JdbcTemplate + 远程 MySQL）。

数据层与接口约定见 [`ARCHITECTURE.md`](./ARCHITECTURE.md)，接口清单见 [`server/CHAT_API.md`](./server/CHAT_API.md)。

---

## 一、启动

需要 JDK 17+、Maven、Node.js 18+。

```bash
# 1) 后端（http://localhost:8081）
cd server
mvn spring-boot:run

# 2) 前端（http://localhost:8080）
npm install
npm run serve
```

浏览器打开 http://localhost:8080。账号为数据库中真实的用户账号，由管理员在「用户管理」中维护。

首次启动后端需要配置数据库连接：复制 `server/application-local.yml.example` 为
`server/application-local.yml` 并填入账号密码（该文件不入库，也可用 `DB_URL` / `DB_USERNAME` /
`DB_PASSWORD` 环境变量覆盖）。

---

## 二、功能

| 模块 | 说明 |
|---|---|
| 登录 / 注册 | `POST /api/login`、`POST /api/register`；token 存 Vuex + localStorage，退出调 `POST /api/logout` |
| 路由守卫 | 未登录跳登录页；按后端返回的权限点（`meta.permission`）拦截无权限页面 |
| 用户管理 | 用户列表、搜索、新增、编辑、删除、绑定角色；`GET/POST/PUT/DELETE /api/users` |
| 角色管理 | 角色 CRUD + 权限树分配；`/api/roles` 系列接口 |
| 菜单与按钮权限 | 目录 / 页面 / 按钮三级节点维护，内置节点仅可改名与排序；`/api/menus` 系列接口 |
| 群聊 | 群列表、历史消息分页、发送消息、建群、加群、成员抽屉；`/api/groups` 系列接口，5 秒轮询增量合并 |
| 仪表盘 | 四个统计卡（用户 / 群组 / 消息 / 群成员）+ 最近消息趋势，数据来自后端聚合查询 |

- 按钮级权限由 `can('user:edit')` 之类的 `v-if` 控制，仅隐藏 UI，不构成安全边界；接口级校验由后端 `@RequirePermission` 完成。
- 页面组件按路由懒加载，chunk 加载失败时自动刷新一次。

---

## 三、请求流程

```text
组件（Login.vue / User.vue / Dashboard.vue / Chat.vue）
   │  调用 src/api/*.js
   ▼
src/api/request.js                 ← baseURL、请求拦截器、响应拆包、401 处理
   │  axios
   ▼
开发环境：Vue CLI devServer 代理 /api  ->  http://localhost:8081
生产环境：Nginx 反向代理        /api  ->  http://127.0.0.1:8081
   ▼
Spring Boot（work-admin-server）
```

统一响应体：

```json
{ "code": 200, "msg": "success", "data": {} }
```

---

## 四、目录结构

```text
work-admin/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── api/
│   │   ├── request.js          # axios 实例 + 拦截器 + 401 处理
│   │   ├── auth.js             # 登录 / 注册 / 退出
│   │   ├── access.js           # 用户、角色、菜单与权限
│   │   ├── dashboard.js        # 仪表盘统计
│   │   └── groups.js           # 群聊
│   ├── components/
│   │   ├── ParticleField.vue   # 登录 / 注册页的粒子背景
│   │   └── ThemeToggle.vue     # 亮暗主题切换
│   ├── directives/
│   │   └── reveal.js           # v-reveal 滚动淡入指令
│   ├── router/
│   │   └── index.js            # 路由 + 全局守卫（history 模式）
│   ├── store/
│   │   └── index.js            # Vuex + localStorage 持久化
│   ├── utils/
│   │   ├── menu.js             # 菜单树工具
│   │   └── theme.js            # 主题读写
│   ├── views/
│   │   ├── Layout.vue          # 中台布局骨架
│   │   ├── Login.vue
│   │   ├── Register.vue
│   │   ├── Dashboard.vue
│   │   ├── User.vue
│   │   ├── Roles.vue
│   │   ├── Menus.vue
│   │   ├── Chat.vue
│   │   └── Forbidden.vue
│   ├── App.vue                 # 全局样式与 design tokens
│   └── main.js                 # 入口
├── deploy/
│   └── nginx.conf              # history 回退 + /api 反向代理
├── server/                     # Spring Boot 后端
├── .env.development            # 开发环境接口前缀
├── .env.production             # 生产环境接口前缀
├── vue.config.js               # publicPath + /api 开发代理
└── package.json                # serve / build
```

---

## 五、界面设计系统

按「材质感 / 呼吸感 / 空间感 / 组件系统」四层组织，亮暗双主题。

| 层 | 实现 |
|---|---|
| 材质感 | 细边框 + 分层轻阴影 + `backdrop-filter` 玻璃 + 噪点纹理 + 柔和渐变网格（`.mesh` / `.noise` / `.glass` / `.surface`） |
| 呼吸感 | 自定义缓动曲线 + `v-reveal` 滚动淡入 + hover 反馈 + 错落延迟；尊重 `prefers-reduced-motion` |
| 空间感 | 登录 / 注册页的轻量 Canvas 粒子场，只做氛围不抢主体 |
| 组件系统 | `App.vue` 的 `:root` 集中管理 design tokens，并对齐 Element Plus 主题变量 |

- 颜色 / 圆角 / 阴影 / 缓动 / 字体等变量全在 `src/App.vue` 的 `:root` 与 `html.dark`，改一处全局生效。
- 强调色只有一个（亮色 `--accent: #00a88f`、暗色 `#5ef2d6`），避免多色堆叠。
- 主题持久化在 `localStorage` 的 `workadmin-theme`，`public/index.html` 里有内联脚本，首屏前就定好主题，避免闪白。

---

## 六、打包与部署

```bash
npm run build        # 产物 dist/
```

- 路由是 **history 模式**：部署时服务器必须做 SPA 回退，否则刷新 `/user` 会 404（见 `deploy/nginx.conf`）。
- 生产环境还要把 `/api` 反向代理到后端（`deploy/nginx.conf` 里已给示例）。
- `dist` 不能双击打开，要用静态服务器：`npx serve -s dist`。

---

## 七、说明与边界

- 前端只负责界面与交互；**鉴权和数据持久化在后端**。
- 前端按钮权限只隐藏 UI，**不构成安全边界**，接口级校验由后端 `@RequirePermission` 负责。
- `VUE_APP_` 开头的环境变量会在构建时写进包里，**不能放密钥**。
- 数据库凭据只放在 `server/application-local.yml`（已 gitignore），不要提交。
