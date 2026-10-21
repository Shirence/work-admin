# work-admin 工作管理中台（前端）

Vue 3（选项式 API，纯 JavaScript）+ Element Plus + Vuex + Vue Router + Axios。

配套后端：[`server`](./server)（Spring Boot 3.5 + Spring Data JPA + 远程 MySQL）。

> 前端按《Vue 3 + Element Plus 工作管理中台：项目实战课》第 0 ~ 10 课完成，
> 之后扩展了「第 11 课」：接入 Spring Boot 真实后端；MockJS 保留为可切换的离线模式。

---

## 一、两种运行方式

| 模式 | 怎么启动 | 需要后端 | 数据存放 |
|---|---|---|---|
| **联调模式（默认）** | `npm run serve` + 启动 server/ 后端 | 是 | 远程 MySQL（本项目表带 `wa_` 前缀） |
| **离线 Mock** | `VUE_APP_USE_MOCK=true`，或 `npm run build:mock` | 否 | 浏览器内存，刷新复位 |

### 1. 联调模式（推荐）

```bash
# 1) 启动后端（http://localhost:8081）
cd E:\my-project\work-admin\server
mvn spring-boot:run

# 2) 启动前端（http://localhost:8080）
cd E:\my-project\work-admin
npm install
npm run serve
```

浏览器打开 http://localhost:8080 ，用下面的演示账号登录。

### 2. 离线 Mock（不需要后端）

```bash
# 方式 A：开发时临时切 Mock
#   把 .env.development 里的 VUE_APP_USE_MOCK 改成 true，然后：
npm run serve

# 方式 B：打一个自带 Mock 的演示包
npm run build:mock
```

---

## 二、演示账号

| 账号 | 密码 | 可访问页面 | 用户管理操作 |
|---|---|---|---|
| `admin` | `123456` | 仪表盘、用户管理 | 新增、编辑、删除 |
| `manager` | `123456` | 仪表盘、用户管理 | 编辑 |
| `viewer` | `123456` | 仪表盘 | 无 |

> 联调模式下账号、角色、权限都由后端返回；离线 Mock 模式下由 `src/views/Login.vue` 和 `src/mock/index.js` 本地模拟。

---

## 三、功能清单

- 登录与退出：调用后端 `POST /api/login`，token 存 Vuex + localStorage，退出调 `POST /api/logout`
- 注册：`/register` 页面（终端风格），前端校验用户名/密码复杂度/两次一致，调 `POST /api/register`
- 路由全局守卫：未登录拦截、`meta.roles` 页面级角色权限
- 请求封装：`src/api/request.js` 统一 baseURL、自动带 token、拆掉一层 `res.data`、401 自动回登录页
- 中台布局：flex 骨架、侧边菜单、内容区独立滚动
- 动态菜单：由路由 `meta` 派生，只显示当前角色可访问的页面
- 用户管理 CRUD：`el-table` + 弹窗表单 + 搜索 + 批量删除 + 表格多选
- 接口分层：组件 → `src/api/*.js` → `src/api/request.js` → axios
- 公共组件封装：`UserFormDialog`（自定义 `v-model`）、`AvatarUpload`、可选 `SearchBar`
- 数据可视化：ECharts 柱状图 / 环形图，`mounted` 初始化、`resize`、`dispose`
- 仪表盘实时统计：用户总数与角色分布来自 `getUserList()`，增删用户后重新进入会同步变化
- 说明：仪表盘里的「待办任务 / 已完成任务 / 系统消息」和「月度任务完成情况」柱状图仍是静态演示数据，暂无对应后端接口
- 本地头像预览：`el-upload` 手动上传 + `on-change` 校验 + Blob URL 释放
- 按钮权限：`permissions` 配合 `v-if` 控制新增 / 编辑 / 删除
- 路由懒加载：页面组件使用 `import()` 拆分 chunk

---

## 四、请求流程

```text
组件 (Login.vue / User.vue / Dashboard.vue)
   │  调用 src/api/auth.js、src/api/user.js     ← 只写接口地址，不关心 token
   ▼
src/api/request.js                              ← baseURL、请求拦截器、响应拆包、401 处理
   │  axios
   ▼
开发环境：Vue CLI devServer 代理 /api  ->  http://localhost:8081
生产环境：Nginx 反向代理        /api  ->  http://127.0.0.1:8081
   ▼
Spring Boot (work-admin-server)
```

统一响应体：

```json
{ "code": 200, "msg": "success", "data": {} }
```

---

## 五、目录结构

```text
work-admin/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── api/
│   │   ├── request.js          # axios 实例 + 拦截器 + 401 处理
│   │   ├── auth.js             # 登录 / 退出 / 当前用户（对接后端）
│   │   └── user.js             # 用户 CRUD 接口
│   ├── components/
│   │   ├── UserFormDialog.vue  # 用户弹窗表单（第 6 课）
│   │   ├── SearchBar.vue       # 可选：搜索栏（第 6 课）
│   │   ├── AvatarUpload.vue    # 本地头像预览（第 8 课）
│   │   └── ParticleField.vue   # 环境粒子场（空间感）
│   ├── directives/
│   │   └── reveal.js           # v-reveal 滚动淡入指令
│   ├── mock/
│   │   └── index.js            # Mock 接口（仅 VUE_APP_USE_MOCK=true 时加载）
│   ├── router/
│   │   └── index.js            # 路由 + 全局守卫（history 模式）
│   ├── store/
│   │   └── index.js            # Vuex + localStorage 持久化
│   ├── views/
│   │   ├── Layout.vue          # 中台布局骨架
│   │   ├── Login.vue           # 登录（调用后端）
│   │   ├── Register.vue        # 注册（调用后端）
│   │   ├── Dashboard.vue       # ECharts + 实时用户统计
│   │   └── User.vue
│   ├── App.vue
│   └── main.js                 # 入口，按 VUE_APP_USE_MOCK 决定是否加载 Mock
├── deploy/
│   └── nginx.conf              # history 回退 + /api 反向代理（第 10 课）
├── .env.development            # 开发：VUE_APP_USE_MOCK=false
├── .env.production             # 生产：VUE_APP_USE_MOCK=false
├── .env.mock                   # 离线：VUE_APP_USE_MOCK=true
├── vue.config.js               # publicPath + /api 开发代理
└── package.json                # serve / build / build:mock
```

---

## 六、界面设计系统（白色基调）

按「材质感 / 呼吸感 / 空间感 / 组件系统」四层组织，整体是大留白白色基调：

| 层 | 实现 |
|---|---|
| 材质感 | 细边框 + 分层轻阴影 + `backdrop-filter` 玻璃 + 噪点纹理 + 柔和渐变网格（`.mesh` / `.noise` / `.glass` / `.surface`） |
| 呼吸感 | 自定义缓动曲线 + `v-reveal` 滚动淡入 + hover 反馈 + 错落延迟；尊重 `prefers-reduced-motion` |
| 空间感 | 登录 / 注册页的轻量 Canvas 粒子场（`components/ParticleField.vue`），只做氛围不抢主体 |
| 组件系统 | `App.vue` 的 `:root` 集中管理 design tokens，并对齐 Element Plus 主题变量 |

要点：

- 设计变量（颜色 / 圆角 / 阴影 / 缓动 / 字体）全在 `src/App.vue` 的 `:root`，改一处全局生效
- 动效指令：`v-reveal` 或 `v-reveal="120"`（数字是延迟毫秒，用来做错落）
- 强调色只有一个（`--accent: #0a84ff`），避免多色堆叠
- 未引入 Three.js，3D 那层用 Canvas 2D 粒子实现，控制打包体积

## 七、打包与部署

```bash
npm run build        # 产物 dist/，需要后端
npm run build:mock   # 产物 dist/，自带 Mock，可离线运行
```

- 路由是 **history 模式**：部署时服务器必须做 SPA 回退，否则刷新 `/user` 会 404（见 `deploy/nginx.conf`）
- 生产环境还要把 `/api` 反向代理到后端（`deploy/nginx.conf` 里已给注释示例）
- `dist` 不能双击打开，要用静态服务器：`npx serve -s dist`

---

## 八、说明与边界

- 前端只负责界面与交互；**鉴权和数据持久化在后端**。
- 前端按钮权限（`v-if="can('user:edit')"`）只隐藏 UI，**不构成安全边界**。
  当前教学版后端只校验「有没有登录」，没有做接口级角色鉴权，详见后端 README 的安全边界一节。
- `VUE_APP_` 开头的环境变量会在构建时写进包里，**不能放密钥**。
- 联调模式下数据存在远程 MySQL（`demo` 库的 `wa_*` 表），后端重启不会复位。
