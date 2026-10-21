# work-admin-server

[`work-admin`](..) 工作管理中台的后端 API，位于 `work-admin/server/`。

- Spring Boot 3.5.16 + Spring Web + Spring JDBC（JdbcTemplate 显式 SQL）+ Bean Validation
- 远程 MySQL 8.0（直接复用 stu 的 `user` / `sys_*` / `chat_*` 表，不建自己的表）
- 统一响应体 `{ code, msg, data }`，与前端 axios 拦截器约定一致
- 请求头 `token` 鉴权（内存 token）

---

## 一、环境要求

- JDK 17+（本机使用 `D:\Environment\Java\jdk-22`）
- Maven 3.9+（本机使用 `D:\Environment\maven\apache-maven-3.9.16`）

> 已把 `JAVA_HOME` 和 Maven 的 `bin` 写入用户环境变量，**新开一个终端**即可直接用 `mvn`。
> 当前终端想立即生效：
> ```powershell
> $env:JAVA_HOME = "D:\Environment\Java\jdk-22"
> $env:Path = "D:\Environment\maven\apache-maven-3.9.16\bin;$env:Path"
> ```

---

## 二、启动

```bash
cd E:\my-project\work-admin\server

# 开发模式（推荐）
mvn spring-boot:run

# 或打包后运行
mvn -DskipTests package
java -jar target/work-admin-server-1.0.0.jar
```

启动后地址：http://localhost:8081

---

## 三、项目结构

```text
work-admin-server/
├── pom.xml
└── src/main/
    ├── java/com/example/workadmin/
    │   ├── WorkAdminApplication.java        # 启动类
    │   ├── common/                          # Result / BizException / @RequirePermission / GlobalExceptionHandler / StuPassword
    │   ├── config/
    │   │   ├── WebConfig.java               # 注册拦截器 + CORS
    │   │   └── AuthInterceptor.java         # 校验请求头 token
    │   ├── controller/
    │   │   ├── AuthController.java          # /api/login /register /logout /me /health
    │   │   ├── AccessController.java        # /api/auth/access、/api/users（真实用户 CRUD）
    │   │   ├── RoleController.java          # /api/roles
    │   │   ├── MenuController.java          # /api/menus
    │   │   ├── DashboardController.java     # /api/dashboard/stats
    │   │   └── GroupChatController.java     # /api/groups（群列表 / 消息 / 发送 / 建群 / 加群）
    │   ├── dto/                             # 请求 / 响应对象（含 GroupChatDtos）
    │   ├── entity/                          # Account / Role / Menu，纯值对象（无 JPA 注解）
    │   └── service/
    │       ├── AuthService.java             # 登录/注册：user 表 + 三重 MD5
    │       ├── AccessService.java           # 角色 / 权限点 / 菜单装配
    │       ├── RoleService.java             # 用户与角色 CRUD
    │       ├── MenuService.java             # 菜单 CRUD
    │       ├── GroupChatService.java        # 群聊业务
    │       ├── DashboardService.java        # 真实统计
    │       ├── StuStore.java                # ★ 唯一允许写 stu 表 SQL 的地方
    │       └── TokenStore.java              # 内存 token
    └── resources/application.yml            # 端口 8081 / MySQL 数据源
```

---

## 四、接口一览

| 方法 | 路径 | 需要 token | 说明 |
|---|---|---|---|
| GET | `/api/health` | 否 | 探活 |
| POST | `/api/login` | 否 | 登录，body `{username,password}` |
| POST | `/api/register` | 否 | 注册，body `{username,password,confirmPassword,email}` |
| POST | `/api/logout` | 是 | 退出登录，作废 token |
| GET | `/api/me` | 是 | 当前登录用户 |
| GET | `/api/user/list?keyword=` | 是 | 用户列表（可按用户名搜索） |
| POST | `/api/user/add` | 是 | 新增，body `{username,phone,role}` |
| POST | `/api/user/edit` | 是 | 编辑，body `{id,username,phone,role}` |
| POST | `/api/user/delete` | 是 | 删除，body `{id}` |

### 统一响应格式

```json
{ "code": 200, "msg": "success", "data": {} }
```

- `code == 200` 成功；`401` 未登录、`400` 参数错误、`404` 数据不存在、`500` 服务器错误
- 需要鉴权的接口请在请求头带 `token: <登录返回的 token>`
- 业务错误统一返回 **HTTP 200 + 业务 code**，方便前端 axios 拦截器统一处理

### 注册规则

注册接口在服务端会再校验一遍（前端校验只是提升体验，不能当安全边界）：

| 字段 | 规则 |
|---|---|
| username | 4-20 位字母 / 数字 / 下划线，且未被占用 |
| password | 6-32 位，必须同时包含字母和数字 |
| confirmPassword | 必须与 password 一致 |
| email | 合法邮箱格式，且未被占用 |

密码经 `BCryptPasswordEncoder` 哈希后存入 `sys_account`，**数据库里看不到明文**。

### 演示账号

| 账号 | 密码 | 角色 | 权限 |
|---|---|---|---|
| admin | 123456 | admin | user:add / user:edit / user:delete |
| manager | 123456 | manager | user:edit |
| viewer | 123456 | viewer | （无） |

---

## 五、快速验证

PowerShell：

```powershell
$base = "http://localhost:8081"

# 探活
Invoke-RestMethod "$base/api/health"

# 登录，拿 token
$login = Invoke-RestMethod "$base/api/login" -Method Post `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"123456"}'
$token = $login.data.token

# 带 token 查列表
Invoke-RestMethod "$base/api/user/list" -Headers @{ token = $token }

# 新增
Invoke-RestMethod "$base/api/user/add" -Method Post `
  -ContentType "application/json" -Headers @{ token = $token } `
  -Body '{"username":"王五","phone":"13700137000","role":"user"}'
```

curl：

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}' | jq -r .data.token)

curl -s http://localhost:8081/api/user/list -H "token: $TOKEN"
```

---

## 六、数据库

主库是远程 MySQL：`118.25.25.164:3306/demo`，账号 `stu`。

- 凭据放 `server/application-local.yml`（已 gitignore），模板见 `server/application-local.yml.example`；
  也可用环境变量 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 覆盖。
- **不建任何自己的表**：账号/角色/菜单/群聊全部直接用 stu 的 `user`、`sys_role`、`sys_user_role`、
  `sys_menu`、`sys_role_menu`、`chat_group`、`chat_member`、`chat_message`。
- 数据访问只有一层 `StuStore`（`JdbcTemplate` + 显式 SQL），项目不引 JPA，
  不存在 `ddl-auto` 去改别人表结构的可能。
- 登录用 stu 已有账号（`admin` / 123456），不再种入任何账号。
- H2 只作为集成测试的内存库（`test` scope），测试用 `src/test/resources/schema.sql` 造表，不碰远程库。

---

## 七、和前端联调

前端 `work-admin/vue.config.js` 已配置开发代理：

```js
proxy: { '/api': { target: 'http://localhost:8081', changeOrigin: true } }
```

> 后端接口路径本身就带 `/api` 前缀，所以代理**不要**配 `pathRewrite`。

前端 `.env.development` 里 `VUE_APP_USE_MOCK=false`，默认直接走这个后端。
想切回离线 Mock：把 `VUE_APP_USE_MOCK` 设为 `true`，或直接 `npm run build:mock` 打包。

### CORS

`WebConfig` 对 `/api/**` 开放了跨域，所以前端不经代理、直接请求 8081 也能通。
本项目默认走 Vue CLI 代理（同源），跨域配置只是方便用 Postman 或换端口调试。

---

## 八、安全边界（重要）

教学项目，以下都是简化实现，**不要直接上生产**：

- token 存在内存里，重启即失效，也没有过期 / 刷新机制
- 账号已入库（`sys_account`），密码用 **BCrypt** 哈希；但注册没有验证码、没有邮箱激活
- 新注册账号一律是 `viewer`（最小权限）
- 接口只校验「有没有登录」，**没有做「这个角色能不能调这个接口」的服务端鉴权**
- 没有 HTTPS、限流、审计日志

真实项目建议：Spring Security + JWT、密码 BCrypt、账号入库、接口级 `@PreAuthorize` 授权。
