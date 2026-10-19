# work-admin 项目交接文档（AI Context Handoff）

> **用途**：把本文档整体投喂给新的 AI 窗口，即可接续开发，无需重新摸索。
> **更新时间**：2026-10-14
> **仓库**：https://gitee.com/shirence/work-admin.git（分支 `master`）

---

## 0. 一句话概况

Vue 3 + Element Plus 的「工作管理中台」前端 + Spring Boot 3.5 后端。
已实现：登录、注册、中台布局、仪表盘（ECharts + 实时统计）、用户 CRUD、批量删除、
角色/按钮权限、路由守卫、路由懒加载、离线 Mock 开关。
界面是**大留白白色基调的高级产品风**（材质感 / 呼吸感 / 空间感 / 组件系统四层）。

---

## 1. 项目位置与启动

```text
E:\my-project\work-admin\          ← 项目根（也是一个 git 仓库）
├── src/                           ← 前端源码
├── public/
├── server/                        ← 后端（Spring Boot 工程）
├── deploy/nginx.conf
├── HANDOFF.md                     ← 本文档
├── README.md                      ← 给人看的说明
└── package.json
```

启动（**两个终端**）：

```powershell
# 终端 1：后端，http://localhost:8081
cd E:\my-project\work-admin\server
mvn spring-boot:run

# 终端 2：前端，http://localhost:8080
cd E:\my-project\work-admin
npm install      # 首次
npm run serve
```

**演示账号**（密码都是 `123456`）：

| 账号 | 角色 | 可访问页面 | 用户管理操作 |
|---|---|---|---|
| admin | admin | 仪表盘、用户管理 | 新增/编辑/删除 |
| manager | manager | 仪表盘、用户管理 | 编辑 |
| viewer | viewer | 仪表盘 | 无 |

新注册账号默认 `viewer`（最小权限）。

---

## 2. 技术栈与版本

| 层 | 技术 |
|---|---|
| 前端框架 | Vue 3.5（**选项式 API，纯 JavaScript，不用 TS**） |
| UI | Element Plus 2.9 + @element-plus/icons-vue |
| 状态 | Vuex 4（+ localStorage 持久化） |
| 路由 | Vue Router 4，**history 模式** |
| 请求 | Axios（封装在 `src/api/request.js`） |
| 图表 | ECharts 5 |
| Mock | MockJS（可通过开关关闭） |
| 构建 | Vue CLI 5（webpack 5）+ Babel |
| 后端 | Spring Boot 3.5.16 + Spring Web + Spring Data JPA + Bean Validation |
| 数据库 | H2 内存库（`jdbc:h2:mem:workadmin`） |
| 密码 | `spring-security-crypto` 的 BCrypt（**不引完整 Spring Security**） |
| JDK / Maven | JDK 17+（本机用 jdk-22）；Maven 3.9.16 |

---

## 3. 本机环境特殊性（重要，换机器要重做）

| 项 | 状态 |
|---|---|
| `JAVA_HOME` | 用户级已设为 `D:\Environment\Java\jdk-22`（系统级仍是坏的 `jdk-25`，用户级优先） |
| Maven | 装在 `D:\Environment\maven\apache-maven-3.9.16`，`bin` 已加入用户 PATH |
| 生效条件 | **必须新开终端**；VS Code 要完全退出重启 |
| Maven 镜像 | `C:\Users\<user>\.m2\settings.xml` 配了阿里云 `https://maven.aliyun.com/repository/public` |
| npm 镜像 | `https://registry.npmmirror.com` |
| 未使用 Maven Wrapper | 本机 PowerShell 5.1 默认 TLS1.0，`mvnw` 下载 Maven 失败，已删除 |

临时在当前终端生效：

```powershell
$env:JAVA_HOME = "D:\Environment\Java\jdk-22"
$env:Path = "D:\Environment\maven\apache-maven-3.9.16\bin;$env:Path"
```

---

## 4. 完整目录结构

```text
work-admin/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── api/
│   │   ├── request.js          # axios 实例 + 请求拦截器(token) + 响应拦截器(拆包/401)
│   │   ├── auth.js             # login / register / logout / getMe
│   │   └── user.js             # getUserList / addUser / editUser / delUser
│   ├── components/
│   │   ├── UserFormDialog.vue  # 用户弹窗表单（自定义 v-model）
│   │   ├── AvatarUpload.vue    # 本地头像预览（Blob URL + 释放）
│   │   ├── SearchBar.vue       # 可选：搜索栏封装
│   │   └── ParticleField.vue   # Canvas 2D 环境粒子场（登录/注册页背景）
│   ├── directives/
│   │   └── reveal.js           # v-reveal 滚动淡入指令
│   ├── mock/
│   │   └── index.js            # MockJS 接口（VUE_APP_USE_MOCK=true 才加载）
│   ├── router/
│   │   └── index.js            # 路由表 + meta + 全局守卫 + ChunkLoadError 兜底
│   ├── store/
│   │   └── index.js            # Vuex：token / userInfo，localStorage 双写
│   ├── views/
│   │   ├── Layout.vue          # 中台骨架（玻璃顶栏 + 侧栏 + 内容区）
│   │   ├── Login.vue
│   │   ├── Register.vue
│   │   ├── Dashboard.vue       # Bento 栅格 + ECharts + 实时用户统计
│   │   └── User.vue
│   ├── App.vue                 # 设计系统 tokens + 全局样式（关键！）
│   └── main.js                 # 入口：注册图标 / v-reveal / Mock 开关
├── server/                     # 见第 5 节
├── deploy/nginx.conf           # history 回退 + /api 反代
├── .env.development            # VUE_APP_USE_MOCK=false
├── .env.production             # VUE_APP_USE_MOCK=false
├── .env.mock                   # VUE_APP_USE_MOCK=true
├── vue.config.js               # publicPath:'/' + /api 代理到 8081
├── package.json                # serve / build / build:mock
└── HANDOFF.md
```

---

## 5. 后端设计（`server/`）

### 5.1 包结构与职责

```text
com.example.workadmin
├── WorkAdminApplication.java       # 启动类
├── common/
│   ├── Result.java                 # 统一响应 { code, msg, data }
│   ├── BizException.java           # 业务异常（带 code）
│   ├── GlobalExceptionHandler.java # @RestControllerAdvice，异常 → Result
│   └── RolePermissions.java        # 角色 → 权限点映射；DEFAULT_ROLE = viewer
├── config/
│   ├── WebConfig.java              # 注册拦截器 + CORS
│   ├── AuthInterceptor.java        # 校验请求头 token
│   ├── PasswordConfig.java         # 提供 BCryptPasswordEncoder Bean
│   └── DataInitializer.java        # 启动时种 demo 数据
├── controller/
│   ├── AuthController.java         # /api/login /register /logout /me /health
│   └── UserController.java         # /api/user/list /add /edit /delete
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java          # { token, userInfo }
│   ├── UserInfo.java               # { username, role, permissions }
│   ├── UserRequest.java            # 用户 CRUD 入参
│   └── RegisterRequest.java        # 注册入参（含 confirmPassword）
├── entity/
│   ├── Account.java                # 登录账号表 sys_account（密码 BCrypt）
│   └── User.java                   # 业务用户表 sys_user
├── repository/
│   ├── AccountRepository.java
│   └── UserRepository.java
└── service/
    ├── AuthService.java            # 登录/注册（查库 + BCrypt）
    ├── TokenStore.java             # 内存 token（ConcurrentHashMap）
    └── UserService.java            # 用户 CRUD
```

### 5.2 接口契约（**前端严格依赖这些**）

| 方法 | 路径 | 需 token | 请求体 / 参数 | 返回 data |
|---|---|---|---|---|
| GET | `/api/health` | 否 | - | `"UP"` |
| POST | `/api/login` | 否 | `{username, password}` | `{token, userInfo:{username, role, permissions}}` |
| POST | `/api/register` | 否 | `{username, password, confirmPassword, email}` | `{username, role, permissions}` |
| POST | `/api/logout` | 是 | - | `null` |
| GET | `/api/me` | 是 | - | `{username, role, permissions}` |
| GET | `/api/user/list?keyword=` | 是 | query | `[{id, username, phone, role}]` |
| POST | `/api/user/add` | 是 | `{username, phone, role}` | 新增后的 User |
| POST | `/api/user/edit` | 是 | `{id, username, phone, role}` | 编辑后的 User |
| POST | `/api/user/delete` | 是 | `{id}` | `null` |

### 5.3 统一响应与错误码

```json
{ "code": 200, "msg": "success", "data": {} }
```

- **业务错误也返回 HTTP 200**，靠 `code` 区分（前端 axios 拦截器统一 `reject(new Error(msg))`）
- `401` 未登录 / `400` 参数错误 / `404` 不存在 / `409` 冲突（用户名、邮箱重复）/ `500` 服务器错误

### 5.4 认证流程

1. `POST /api/login` → `AuthService` 查 `sys_account`，`BCryptPasswordEncoder.matches()` 校验
2. 通过则 `TokenStore.create()` 生成 **32 位 UUID**（去掉横线），存内存
3. 前端存 `localStorage.token` + `userInfo`（Vuex 双写）
4. 每个请求由 axios 请求拦截器写入 `token` 请求头
5. `AuthInterceptor` 校验 `/api/**`，**放行** `/api/login`、`/api/register`、`/api/health`
6. 登录失败不区分「用户不存在」和「密码错误」（防账号枚举）

### 5.5 数据库

- H2 **内存库**，重启后端数据复位
- `jdbc:h2:mem:workadmin`，用户 `sa`，密码空
- H2 控制台：<http://localhost:8081/h2-console>
- `sys_user` 种子：张三(admin)、李四(user)
- `sys_account` 种子：admin / manager / viewer，密码 BCrypt("123456")

### 5.6 注册校验规则（前后端一致）

| 字段 | 规则 |
|---|---|
| username | `^[A-Za-z0-9_]{4,20}$`，且未占用 |
| password | 6-32 位，必须同时含字母和数字，`^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{6,32}$` |
| confirmPassword | 必须等于 password |
| email | 合法邮箱，且未占用 |

### 5.7 安全边界（务必告知用户）

- ✅ 密码 BCrypt 哈希，库里无明文
- ❌ token 存内存、无过期/刷新机制
- ❌ 无验证码、无邮箱激活、无找回密码
- ❌ **没有接口级角色鉴权**（只校验「有没有登录」），前端 `v-if` 按钮权限只是 UI
- ❌ 无 HTTPS / 限流 / 审计日志

---

## 6. 前端设计

### 6.1 请求链路

```text
组件 (Login/User/Dashboard)
  │  调用 src/api/auth.js / src/api/user.js     ← 只写接口路径
  ▼
src/api/request.js                              ← baseURL + token 拦截 + 拆 res.data + 401 处理
  │  axios
  ▼
开发：Vue CLI devServer 代理 /api → http://localhost:8081
生产：Nginx 反代 /api → http://127.0.0.1:8081
  ▼
Spring Boot
```

`request.js` 关键行为：

- `baseURL: process.env.VUE_APP_BASE_API`（= `/api`）
- 请求拦截：`config.headers.token = localStorage.getItem('token')`
- 响应拦截：`code !== 200` → `Promise.reject(new Error(result.msg))`
- `code === 401` 且不是 `/login` → 清 token/userInfo 并 `location.href = '/login'`

### 6.2 路由与权限

路由表（`src/router/index.js`）：

```text
/login      → Login.vue            （白名单）
/register   → Register.vue         （白名单）
/           → Layout.vue  redirect /dashboard
   ├─ dashboard → Dashboard.vue  meta:{title:'仪表盘', icon:'House', menu:true, roles:[admin,manager,viewer]}
   └─ user      → User.vue       meta:{title:'用户管理', icon:'User', menu:true, roles:[admin,manager]}
```

- **history 模式**（`createWebHistory(process.env.BASE_URL)`），生产必须配 SPA 回退
- 守卫：白名单直接放行；无 token → `/login`；角色不在 `meta.roles` → 提示并回 `/dashboard`
- `router.onError` 捕获 ChunkLoadError 自动刷新一次（防旧 chunk）
- 页面组件全部 `() => import(...)` 懒加载

### 6.3 状态管理

`src/store/index.js`：`token` / `userInfo`，初始值从 localStorage 读；mutation 内双写。

### 6.4 Mock 开关

```js
// src/main.js
if (process.env.VUE_APP_USE_MOCK === 'true') {
  require('./mock')
}
```

| 文件 | 值 |
|---|---|
| `.env.development` | `VUE_APP_USE_MOCK=false` |
| `.env.production` | `VUE_APP_USE_MOCK=false` |
| `.env.mock` | `VUE_APP_USE_MOCK=true` |

`npm run build:mock` = `vue-cli-service build --mode mock`（离线演示包）。

> **Mock 模式下 MockJS 会替换 XMLHttpRequest，网络面板里看不到 /api 请求**；
> 真实后端模式下能看到。这是区分真假的最快方法之一。

### 6.5 组件清单

| 组件 | 职责 | 关键点 |
|---|---|---|
| `UserFormDialog.vue` | 用户新增/编辑弹窗 | 自定义 `v-model`（`modelValue` + `update:modelValue`）、内部 form 副本、`watch immediate` |
| `AvatarUpload.vue` | 本地头像预览 | `auto-upload=false`、`on-change` 校验类型/2MB、`URL.createObjectURL` + `revokeObjectURL` |
| `SearchBar.vue` | 可选搜索栏封装 | props/emit |
| `ParticleField.vue` | 环境粒子场 | Canvas 2D、dpr 适配、`prefers-reduced-motion` 时静止 |

---

## 7. 界面设计系统（白色基调）

### 7.1 四层结构

| 层 | 目标 | 实现 |
|---|---|---|
| **材质感** | 页面不像模板 | 细边框 + 分层轻阴影 + `backdrop-filter` 玻璃 + SVG 噪点 + 柔和渐变网格 |
| **呼吸感** | 页面会呼吸 | 自定义缓动 + `v-reveal` 滚动淡入 + hover 反馈 + 错落延迟 + 尊重 reduced-motion |
| **空间感** | 有深度 | `ParticleField.vue` Canvas 2D 粒子场（登录/注册页），只做氛围 |
| **组件系统** | 像真实产品 | `App.vue :root` 单一 token 源 + 对齐 Element Plus 变量 + 统一 Bento 栅格 |

### 7.2 Design Tokens（全部在 `src/App.vue` 的 `:root`）

```css
/* 颜色 */
--bg:#ffffff; --bg-sub:#fafbfc; --bg-inset:#f4f6f9;
--line:#eef1f4; --line-2:#e3e8ee; --line-3:#d5dce4;
--text:#0d1520; --text-2:#5a6672; --text-3:#98a2ae;
--accent:#0a84ff; --accent-2:#4da3ff;
--accent-soft:rgba(10,132,255,.08); --accent-line:rgba(10,132,255,.28);
--ok:#17a673; --warn:#d99100; --danger:#e5484d; --violet:#7c5cff;

/* 圆角 */
--r-sm:6px; --r-md:10px; --r-lg:14px;

/* 阴影（分层、低对比） */
--shadow-xs:0 1px 2px rgba(13,21,32,.04);
--shadow-sm:0 1px 2px ..., 0 2px 6px -1px ...;
--shadow-md:0 2px 4px ..., 0 10px 24px -6px ...;
--shadow-lg:0 4px 8px ..., 0 24px 48px -12px ...;

/* 动效 */
--ease:cubic-bezier(.16,1,.3,1); --ease-2:cubic-bezier(.4,0,.2,1);
--dur-1:.18s; --dur-2:.32s; --dur-3:.6s;

/* 字体 */
--mono:'JetBrains Mono','Cascadia Mono',Consolas,'SF Mono',Menlo,monospace;
--sans:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;
```

同时覆盖了 Element Plus 变量：`--el-color-primary:#0a84ff`、`--el-border-radius-base:8px`、
`--el-text-color-*`、`--el-border-color` 等。

### 7.3 全局工具类

| 类 | 作用 |
|---|---|
| `.mono` | 等宽字体 |
| `.u-label` | 10px 等宽大写小标签（letter-spacing .12em） |
| `.u-hint` | 12px 次要说明文字 |
| `.mesh` | 三处柔和 radial-gradient 组成的渐变网格背景 |
| `.noise` | `::after` 叠加 SVG feTurbulence 噪点（需要父元素 `position:relative`） |
| `.glass` | 半透明白 + `backdrop-filter: saturate(180%) blur(14px)` |
| `.surface` | 白底 + 细边框 + 圆角 + 轻阴影 |
| `.reveal` / `.reveal.is-visible` | 滚动淡入的起止态 |

### 7.4 `v-reveal` 指令（`src/directives/reveal.js`）

- 单例 `IntersectionObserver`（threshold .08，rootMargin 底部 -32px）
- 进入视口 → 加 `.is-visible` → unobserve
- 用法：`v-reveal` / `v-reveal="120"` / `v-reveal="{delay:120}"`（数字=延迟 ms，做错落）
- 不支持 IntersectionObserver 时直接显示

### 7.5 页面结构要点

| 页面 | 结构 |
|---|---|
| `Layout.vue` | 232px 侧栏（白底、`nav-rail` 滑动高亮条、底部状态灯）+ 玻璃吸顶顶栏（sticky + backdrop-filter）+ `.content.mesh.noise` 内容区 |
| `Login.vue` / `Register.vue` | 全屏 `.mesh.noise` + `<ParticleField>` + 玻璃卡（`mask-composite` 做渐变描边）+ 深色主按钮 |
| `Dashboard.vue` | CSS Grid `repeat(12,1fr)` Bento：4 个 `span 3` 指标块 + 柱状图 `span 8` + 环形图 `span 4`；断点 1200/680 |
| `User.vue` | 页头 + `surface` 面板 + 工具栏 + 精修表格（首字母头像、角色胶囊、`el-empty` 空状态） |

> ⚠️ **Element Plus 样式顺序**：`main.js` 里 `import App from './App.vue'` 在
> `import 'element-plus/dist/index.css'` 之前，理论上 App 的 `:root` 可能被 EP 覆盖。
> 如果发现主色不是 `#0a84ff`，把 `element-plus/dist/index.css` 的 import 提到 `App.vue` 之前即可。

---

## 8. 常用命令

```powershell
# ---------- 前端 ----------
cd E:\my-project\work-admin
npm install                 # 安装依赖
npm run serve               # 开发，http://localhost:8080
npm run build               # 生产打包（走后端），产物 dist/
npm run build:mock          # 离线演示包（内置 Mock）

# ---------- 后端 ----------
cd E:\my-project\work-admin\server
mvn spring-boot:run                     # 开发启动
mvn -DskipTests package                 # 打包 → target/work-admin-server-1.0.0.jar
java -jar target\work-admin-server-1.0.0.jar

# ---------- 部署 ----------
npx serve -s dist           # 本地预览 dist（-s 必须有，history 模式需要回退）
# Nginx 参考 deploy/nginx.conf
```

---

## 9. 如何验证「前端数据是真的来自后端」

| 方法 | 判断标准 |
|---|---|
| **F12 → Network → Fetch/XHR** | 能看到 `POST /api/login`、`GET /api/user/list` 等真实请求（Mock 模式下看不到） |
| **`localStorage.token`** | 真实后端：32 位十六进制；Mock：`admin-demo-token` 这种 |
| **停掉后端再操作** | 前端报「获取列表失败」/ 500，说明确实依赖后端 |
| **H2 控制台** | `http://localhost:8081/h2-console` 能看到 `sys_user` / `sys_account` 表 |
| **重启后端** | 数据回到张三、李四（内存库） |
| **curl 直连** | `Invoke-RestMethod http://localhost:8081/api/health` |

> 之前实测：登录后 Network 出现
> `POST /api/login`、`GET /api/user/list`、`POST /api/user/add` 等 5 个 /api 请求，0 控制台报错。

---

## 10. 已完成的操作时间线（按顺序）

1. **按课程文档（第 0~10 课）搭建前端**：Vue CLI 5 + Element Plus + Vuex + Router + Axios + MockJS + ECharts。
2. **修复启动问题**：favicon 404、ChunkLoadError（`router.onError` 自动刷新）、端口冲突。
3. **仪表盘接真数据**：用户总数 / 角色分布改为 `getUserList()` 计算。
4. **迁移项目**：`D:\Downloads\work-admin` → `E:\my-project\work-admin`（用 robocopy 排除 node_modules 后 `npm install`）。
5. **切换 history 路由**：`createWebHistory` + `deploy/nginx.conf` 的 `try_files`。
6. **新增 Spring Boot 后端**：`server/`，Spring Boot 3.5.16 + JPA + H2，接口对齐前端契约。
7. **前端接入后端**：新增 `src/api/auth.js`、`request.js` 加 401、`VUE_APP_USE_MOCK` 开关、`build:mock`。
8. **移除数据来源标注** + 首轮美化。
9. **改成科技终端风**（白底 + 青色 + 等宽标注 + HUD 角标）。
10. **新增注册功能**：`sys_account` 表、BCrypt、`POST /api/register`、`/register` 页面。
11. **按「美化手册」重构为高级白色设计系统**：四层（材质/呼吸/空间/组件系统）、Bento 栅格、玻璃顶栏、粒子场、`v-reveal`。
12. **全部提交并推送到 Gitee**（提交信息用英文）。

**当前 git 提交（从旧到新）**：

```text
9ec7938  new add springboot
23cc418  feat: 接入 Spring Boot 后端，后端工程迁入 server/
8d46159  style: refresh the UI and drop the data-source annotations
4a9f1ab  style: switch the frontend to a futuristic terminal theme (light)
09479d8  feat: add user registration (backend API + terminal-style page)
d8ab149  style: rebuild the UI as a premium white-based design system
bbe0cb2  docs: document the design system and v-reveal usage   ← HEAD
```

---

## 11. 已知坑 & 排错

| 现象 | 原因 | 解决 |
|---|---|---|
| **改了代码但界面没变** | 浏览器缓存旧 JS/CSS，或 8080 上跑着旧的 dev server | `Ctrl+Shift+R` 硬刷新；`Get-Process node` 杀掉旧 vue-cli-service 再 `npm run serve`；用无痕窗口 |
| `npm run serve` 跑到了 8081/8082 | 8080 被占用，Vue CLI 自动换端口 | 看终端 `Local:` 那一行；先清掉占用 8080 的进程 |
| `ChunkLoadError` | 页面开着时 dev server 重启/挂了 | 已加 `router.onError` 自动刷新一次；否则手动刷新 |
| `favicon.ico 404` | 没有图标文件 | 已加 `public/favicon.ico` |
| `mvn` 命令找不到 | 终端是改 PATH 之前开的 | 新开终端；VS Code 要完全重启 |
| `JAVA_HOME` 指向不存在的 jdk-25 | 系统级环境变量是坏的 | 用户级已覆盖为 jdk-22；或临时 `$env:JAVA_HOME=...` |
| 刷新 `/user` 404 | history 模式 + 服务器没回退 | Nginx `try_files $uri $uri/ /index.html;`；本地用 `serve -s dist` |
| 联调 404 | 代理用了 `pathRewrite` 把 `/api` 去掉了，但后端本身就带 `/api` | **不要**配 pathRewrite |
| 注册接口 403/401 | `/api/register` 没加进拦截器白名单 | `WebConfig.excludePathPatterns("/api/login","/api/register","/api/health")` |
| 中文乱码（PowerShell 脚本输出） | Windows 控制台编码 | Python 里 `sys.stdout.reconfigure(encoding='utf-8')` |

---

## 12. 待办 / 可扩展方向

- [ ] **服务端权限鉴权**：接口级 `@PreAuthorize` 或自定义注解，别只靠前端 `v-if`
- [ ] **JWT + 过期/刷新**，替代内存 token
- [ ] **验证码**（图形/滑块）防脚本注册
- [ ] **邮箱激活**流程
- [ ] **换 MySQL**：加 `mysql-connector-j` + 改 `application.yml` 即可（JPA 自动建表）
- [ ] **注册后自动建业务用户**（目前 `sys_account` 和 `sys_user` 两张表独立）
- [ ] **Three.js 3D 背景**（手册第三层升级；当前用 Canvas 2D 粒子替代）
- [ ] **命令面板 Cmd+K**（手册第四层提到的 command palette）
- [ ] **统一 Toast / Modal 封装 + 骨架屏**
- [ ] **数字滚动动画**
- [ ] **任务管理页**（教程课后作业）

---

## 13. 给新 AI 的接续建议

1. 先读 `src/App.vue`（设计 tokens）→ `src/views/Layout.vue`（骨架）→ `src/api/request.js`（请求链路）。
2. 改后端接口时，**务必保持第 5.2 节的响应结构** `{code,msg,data}`，否则前端拦截器会 reject。
3. 改样式时优先用 `:root` 里的变量，不要硬编码颜色/圆角/阴影。
4. 任何"看起来没生效"的问题，先排查：**浏览器缓存 / 旧 dev server / 端口不对**（见第 11 节）。
5. 提交信息用**英文**（用户要求）。
6. `screenshots/` 不进 git（已在 `.gitignore`）。

---

*本文档由 AI 助手根据实际代码与操作过程整理，可作为新会话的上下文注入。*
