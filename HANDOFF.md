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
| 端到端加密 | `@privacyresearch/libsignal-protocol-typescript`（X3DH + Double Ratchet，浏览器本地密钥） |
| 主题 | light / dark 双主题，`localStorage.workadmin-theme`，`ThemeToggle.vue` 全局切换 |
| Mock | MockJS（可通过开关关闭） |
| 构建 | Vue CLI 5（webpack 5）+ Babel |
| 后端 | Spring Boot 3.5.16 + Spring Web + Spring Data JPA + Bean Validation |
| 数据库 | H2 文件库（`jdbc:h2:file:./data/workadmin`，文件在 `server/data/workadmin.mv.db`） |
| 远程 MySQL | MySQL Connector/J + 只读观察池 + 可写聊天池；支持远程 stu 账号登录、加入群聊、收发消息 |
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
│   │   ├── user.js             # getUserList / addUser / editUser / delUser
│   │   ├── remote.js           # 远程 MySQL 观察台（仅 admin）
│   │   ├── remoteChat.js       # 远程 stu 账号登录 + 远程群聊
│   │   └── chat.js             # 本地 E2EE 聊天 API（设备/会话/消息）
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
│   ├── utils/
│   │   ├── bytes.js            # ArrayBuffer/Base64
│   │   ├── signalStore.js      # libsignal StorageType 的 localStorage 实现
│   │   ├── chatCrypto.js       # 设备密钥生成、会话建立、逐设备加解密
│   │   └── theme.js            # light/dark 主题持久化与切换
│   ├── components/
│   │   ├── ThemeToggle.vue     # light/dark 主题切换按钮
│   │   └── SourceSwitch.vue    # 本地 H2 / 远程 MySQL 统一切换
│   ├── views/
│   │   ├── Layout.vue          # 中台骨架（玻璃顶栏 + 侧栏 + 内容区）
│   │   ├── Login.vue
│   │   ├── Register.vue
│   │   ├── Dashboard.vue       # Bento 栅格 + ECharts + 实时用户统计
│   │   ├── Chat.vue            # 本地 E2EE 消息页（设备初始化/会话/收发）
│   │   ├── User.vue
│   │   ├── RemoteChat.vue      # 远程 MySQL 群聊/消息观察台（admin）
│   │   ├── RemoteLogin.vue     # 远程 stu 账号登录
│   │   ├── RemoteRegister.vue  # 远程 stu 账号注册
│   │   └── RemoteChatRoom.vue  # 远程群聊（加入/建群/收发消息）
│   ├── App.vue                 # 设计系统 tokens + 全局样式（关键！）
│   └── main.js                 # 入口：注册图标 / v-reveal / Mock 开关
├── server/                     # 见第 5 节
│   ├── CHAT_API.md             # 聊天后端接口
│   ├── REMOTE_MYSQL.md         # 远程 MySQL 只读观察台
│   └── application-local.yml   # 远程库凭据（已 gitignore，运行时加载）
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

- H2 **文件库**，重启后端数据保留
- `jdbc:h2:file:./data/workadmin;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1`
- 数据库文件：`server/data/workadmin.mv.db`（已加入 `.gitignore`）
- 用户 `sa`，密码空
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
- ❌ **没有接口级角色鉴权**（只校验「有没有登录」；聊天管理员接口除外），前端 `v-if` 按钮权限只是 UI
- ❌ 无 HTTPS / 限流 / 审计日志

### 5.8 聊天模块（E2EE / 多设备）

聊天后端已按以下模型实现，完整接口见 [`server/CHAT_API.md`](server/CHAT_API.md)：

- 设备：`chat_device`、`chat_one_time_pre_key`、`chat_device_link`、`chat_device_grant`
- 会话：`chat_conversation`（`version` 群版本号）、`chat_member`、`chat_read_cursor`
- 消息：`chat_message`（`clientMessageId` 幂等 + 服务端 `serverSeq` 定序）、`chat_envelope`（逐设备密文）
- 建群权限：`chat_group_policy`（全局）+ `chat_account_group_permission`（账号覆盖）
- 服务端只存密文；新设备 PENDING，必须由已有 ACTIVE 设备授权后才能同步
- 集成测试：`server/src/test/java/com/example/workadmin/ChatFlowIntegrationTest.java`（12 项）
- 远程观察台：H2 主库之外额外挂一个**只读 MySQL**，管理员可在 `/remote`
  查看 `stu` 库的群组、成员、消息；配置见 `server/REMOTE_MYSQL.md`。远程不可用不影响 H2。
- 远程账号群聊：`/remote-login` 用远程 `demo.user` 账号登录或注册（独立 `remoteToken`），
  `/remote-chat` 可加入群聊、读历史、发消息；写入远程 `chat_group/chat_member/chat_message`。
  远程 token 只能访问 `/api/remote-auth/**` 和 `/api/remote-chat/**`，不能调用本地 H2 接口。
  远程注册直接写 `user` 表并绑定 `member` 角色，不允许指定角色。
- 本地 E2EE 消息页：`/chat` 首次进入生成 identity key / signed pre-key / one-time pre-key，
  私钥只存浏览器 `localStorage`（按 clientDeviceKey 隔离）；发送时对每个目标设备
  `SessionCipher.encrypt`，每条消息生成多个 `chat_envelope`。
- 新设备：第二台设备注册后为 PENDING，现有设备在「设备」对话框批准后激活；
  批准前无法同步，批准后只能解密之后发给它的消息（旧消息需额外历史密钥授权）。
- Vue 构建：`vue.config.js` 里给 libsignal 的 curve25519 wasm 包设置 `resolve.fallback.fs=false`。
- 数据源切换：本地 `/chat` 页头有「本地 H2 / 远程 MySQL」切换；远程 `/remote-chat`
  页头有「本地 H2 / 远程 MySQL」切换。远程聊天不出现在本地侧栏，只从远程登录进入。
  远程聊天需要独立远程账号登录，本地 token 与 remoteToken 互不覆盖。

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
   ├─ chat      → Chat.vue       meta:{title:'消息', icon:'ChatDotRound', menu:true, roles:[admin,manager,viewer]}
   ├─ user      → User.vue       meta:{title:'用户管理', icon:'User', menu:true, roles:[admin,manager]}
   └─ remote    → RemoteChat.vue meta:{title:'远程观察', icon:'ChatDotRound', menu:true, roles:[admin]}

/remote-login → RemoteLogin.vue    （公开；远程 stu 账号登录）
/remote-register → RemoteRegister.vue（公开；远程 stu 账号注册）
/remote-chat  → RemoteChatRoom.vue （meta.remoteAuth；独立 remoteToken，不依赖本地 token）
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

## 7. 界面设计系统（深空工业 HUD · 双主题）

### 7.1 美学方向

**工业未来主义 / 深空控制台**，刻意避开 Inter、白底紫色渐变、通用卡片堆叠：

- 深色主题：深空黑底 + 薄荷青主色 + 冰蓝/品红点缀 + 网格/扫描线/辉光
- 浅色主题：冷白实验室 + 薄荷青 + 极细网格 + 深墨文字
- 展示字体 **Syne**，正文 **DM Sans**，等宽 **JetBrains Mono**
- 圆角收窄到 2/4/8px，用 HUD 角标、细线、序号、状态灯建立工业感
- 动效克制：`v-reveal`、hover 位移、扫描线、状态灯呼吸，尊重 reduced-motion

### 7.2 Design Tokens

所有 token 在 `src/App.vue`：

- `:root` = 浅色主题
- `html.dark` = 深色主题
- 页面组件只使用 `var(--*)`，不要硬编码颜色

核心颜色：

```css
/* 浅色 */
--bg:#f3f7f8; --surface:#ffffff; --text:#081418; --text-2:#415a63; --text-3:#78909a;
--accent:#00a88f; --accent-2:#3fd8bf; --ice:#0b6fd0; --violet:#d61f69;

/* 深色 */
--bg:#05080d; --surface:#0a1018; --text:#e8f4f6; --text-2:#9fb4be; --text-3:#607680;
--accent:#5ef2d6; --accent-2:#9dffcf; --ice:#58a6ff; --violet:#ff3d81;
```

同时覆盖 Element Plus 变量，并在 `html` 上用 `!important` 兜底
（EP 的 `:root` 变量在部分构建顺序下会覆盖 App 样式）：

```css
html {
  --el-color-primary: var(--accent) !important;
  --el-bg-color: var(--surface) !important;
  --el-text-color-primary: var(--text) !important;
  /* ... */
}
```

### 7.3 主题切换

- `src/utils/theme.js`
  - `getTheme()`：优先 `localStorage.workadmin-theme`，其次系统 `prefers-color-scheme`
  - `applyTheme(theme)`：切换 `html.dark`、写入 localStorage、更新 `meta[theme-color]`
  - 派发 `window` 事件 `theme-change`
- `src/components/ThemeToggle.vue`
  - 顶栏/登录页右上角的 `LIGHT / DARK` 按钮
  - 已放在 `Layout.vue`、四个 auth 页、`RemoteChatRoom.vue`
- `src/components/SourceSwitch.vue`
  - 本地 `/chat` 与远程 `/remote-chat` 共用同一个「本地 H2 / 远程 MySQL」切换组件
  - 两处 active 色、尺寸、字体完全一致
- `main.js` 在挂载前 `initTheme()`，避免刷新闪白/闪黑
- `Dashboard.vue` 监听 `theme-change`，销毁并重建 ECharts，保证图表颜色跟随主题

### 7.4 全局工具类

| 类 | 作用 |
|---|---|
| `.mono` | 等宽字体 |
| `.u-label` | 10px 等宽大写小标签（letter-spacing .16em） |
| `.u-hint` | 12px 次要说明文字 |
| `.mesh` | 网格 + 三处 radial-gradient 光晕背景（随主题切换；`meshDrift` 5s 漂移，浅色对比更强） |
| `.noise` | 扫描线 + 动态 `scanBeam`（3.6s）+ SVG feTurbulence 噪点 |
| `.glass` | 半透明主题面板 + `backdrop-filter` |
| `.surface` | 面板渐变 + 细边框 + 圆角 + 轻阴影 |
| `.hud-corners` | HUD 角标（左上/右下细线） |
| `.reveal` / `.reveal.is-visible` | 滚动淡入 + 轻微模糊过渡 |

### 7.5 页面结构要点

| 页面 | 结构 |
|---|---|
| `Layout.vue` | 248px 侧栏（HUD 网格、六边形品牌标、序号导航、LINK STABLE）+ 玻璃顶栏（主题切换/角色/用户/EXIT）+ `.content.mesh.noise` |
| `Login.vue` / `Register.vue` | 全屏 `.mesh.noise` + `<ParticleField>` + 玻璃卡（渐变描边）+ 薄荷渐变主按钮 + 右上角主题切换 |
| `RemoteLogin.vue` / `RemoteRegister.vue` | 同上，但使用品红/冰蓝远程色系；注册独立为 `/remote-register` |
| `Dashboard.vue` | Bento 栅格 + 主题感知的 ECharts（柱状/环形） |
| `User.vue` | 页头 + 面板 + 表格；表头由全局深/浅色样式接管 |
| `Chat.vue` | 本地 E2EE 消息页；顶部「本地 H2 / 远程 MySQL」切换 |
| `RemoteChat.vue` | 管理员只读观察台；4 指标 + 群组/消息双栏 |
| `RemoteChatRoom.vue` | 远程群聊；独立登录态 + 248px 远程侧栏 + 72px 顶栏 + ThemeToggle/SourceSwitch，和主 Layout 结构对齐 |

### 7.6 注意

- `main.js` 已引入 `element-plus/theme-chalk/dark/css-vars.css`，主题切换依赖 `html.dark`
- 如果 EP 主色仍是默认蓝，检查 `App.vue` 末尾 `html { --el-color-primary: var(--accent) !important; }` 是否被后续样式覆盖
- ECharts 不读取 CSS 变量，颜色在 `Dashboard.vue#chartPalette()` 里按主题显式返回

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
mvn spring-boot:run                     # 开发启动（会加载 server/application-local.yml）
mvn -DskipTests package                 # 打包 → target/work-admin-server-1.0.0.jar
java -jar target\work-admin-server-1.0.0.jar
# 远程 MySQL 凭据放在 server/application-local.yml（已 gitignore）
# 不配置时远程观察台返回 503，H2 业务照常运行

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
| **H2 控制台** | `http://localhost:8081/h2-console` 能看到 `sys_user` / `sys_account` 及聊天表 |
| **重启后端** | H2 文件库数据保留（不再是内存库） |
| **远程观察台** | admin 登录后打开 `/remote`，能看到远程库的群组/消息即说明 MySQL 旁路连接生效 |
| **远程账号群聊** | 登录页点「远程账号登录 →」，可登录或注册远程账号；进入 `/remote-chat` 后能加入群并发消息即说明远程读写链路生效 |
| **本地 E2EE 消息** | 登录后进入 `/chat`，首次会生成设备密钥；发送消息后 Network 里 `/api/chat/messages` 的 envelopes 是密文，接收端能显示明文即说明 libsignal 链路生效 |
| **双主题** | 右上角 `LIGHT / DARK` 切换，刷新后保持；Dashboard 图表、Element Plus 表格/弹窗颜色一起变化 |
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
