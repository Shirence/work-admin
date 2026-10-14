# work-admin 工作管理中台

Vue 3（选项式 API，纯 JavaScript）+ Element Plus + Vuex + Vue Router + Axios + MockJS。
按《Vue 3 + Element Plus 工作管理中台：项目实战课》第 0 ~ 10 课搭建，包含登录、中台布局、
仪表盘、用户管理 CRUD、路由权限守卫、Mock 模拟后端、ECharts 可视化、组件封装、批量删除、
角色权限与路由懒加载。

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务（默认 http://localhost:8080）
npm run serve

# 生产打包，产物在 dist/
npm run build
```

> 打包后的 `dist` 不能直接双击 `index.html` 打开，请用静态服务器预览，例如
> `npx serve -s dist`（`-s` 会做 SPA 回退）。本项目使用 history 路由，
> URL 形如 `http://localhost:8080/dashboard`；部署时必须配置 SPA 回退（见 `deploy/nginx.conf`）。

## 演示账号

| 账号 | 密码 | 可访问页面 | 用户管理操作 |
|---|---|---|---|
| `admin` | `123456` | 仪表盘、用户管理 | 新增、编辑、删除 |
| `manager` | `123456` | 仪表盘、用户管理 | 编辑 |
| `viewer` | `123456` | 仪表盘 | 无 |

## 功能清单

- 登录与退出：Vuex 管理状态，localStorage 持久化，刷新不丢登录
- 路由全局守卫：未登录拦截、`meta.roles` 页面级角色权限
- 中台布局：flex 骨架、侧边菜单、内容区独立滚动
- 动态菜单：由路由 `meta` 派生，只显示当前角色可访问的页面
- 用户管理 CRUD：`el-table` + 弹窗表单 + 搜索 + 批量删除 + 表格多选
- 接口分层：组件 -> `src/api/user.js` -> `src/api/request.js` -> axios
- MockJS 模拟后端：拦截 `/api` 请求，维护可变数组（刷新页面重置）
- 公共组件封装：`UserFormDialog`（自定义 `v-model`）、`AvatarUpload`、可选 `SearchBar`
- 数据可视化：ECharts 柱状图 / 环形图，`mounted` 初始化、`resize`、`dispose`
- 仪表盘实时统计：用户总数与角色分布来自 `getUserList()`，增删用户后重新进入仪表盘会同步变化（月度任务图仍为静态演示数据）
- 本地头像预览：`el-upload` 手动上传 + `on-change` 校验 + Blob URL 释放
- 按钮权限：`permissions` 配合 `v-if` 控制新增 / 编辑 / 删除
- 路由懒加载：页面组件使用 `import()` 拆分 chunk

## 目录结构

```text
work-admin/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── api/
│   │   ├── request.js          # axios 实例 + 拦截器
│   │   └── user.js             # 用户相关接口
│   ├── components/
│   │   ├── UserFormDialog.vue  # 用户弹窗表单（第 6 课）
│   │   ├── SearchBar.vue       # 可选：搜索栏（第 6 课）
│   │   └── AvatarUpload.vue    # 本地头像预览（第 8 课）
│   ├── mock/
│   │   └── index.js            # Mock 接口
│   ├── router/
│   │   └── index.js            # 路由 + 全局守卫（history 模式）
│   ├── store/
│   │   └── index.js            # Vuex + localStorage 持久化
│   ├── views/
│   │   ├── Layout.vue          # 中台布局骨架
│   │   ├── Login.vue
│   │   ├── Dashboard.vue       # 含 ECharts 图表 + 实时用户统计
│   │   └── User.vue
│   ├── App.vue
│   └── main.js
├── deploy/
│   └── nginx.conf              # history 模式 SPA 回退（第 10 课）
├── .env.development            # 开发环境变量（第 10 课）
├── .env.production             # 生产环境变量（第 10 课）
├── vue.config.js               # publicPath / 开发代理（第 10 课）
└── package.json
```

## 说明与边界

- 这是前端教学项目：登录凭证、角色与权限均为本地模拟，Mock 数据仅存在于浏览器内存，
  刷新后重置，不能代替真实后端鉴权。
- 连接真实后端时：删除 `src/main.js` 中的 `import './mock'`，按第 10 课在
  `vue.config.js` 中启用 `devServer.proxy`，并在生产环境用 Nginx 配置 `/api` 反向代理。
- 环境变量以 `VUE_APP_` 开头才会注入，且在构建时写入包中，不能存放密钥。
