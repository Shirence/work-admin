# work-admin-server

[`work-admin`](..) 工作管理中台的后端 API，位于 `work-admin/server/`。

- Spring Boot 3.5.16 + Spring Web + Spring JDBC（JdbcTemplate 显式 SQL）+ Bean Validation
- 远程 MySQL 8.0（直接复用 stu 的 `user` / `sys_*` / `chat_*` 表，不建自己的表）
- 统一响应体 `{ code, msg, data }`，与前端 axios 拦截器约定一致
- 请求头 `token` 鉴权（内存 token）

---

## 一、环境要求

- JDK 17+
- Maven 3.9+

---

## 二、启动

```bash
cd server

# 数据库凭据（该文件不入库）
cp application-local.yml.example application-local.yml
# 编辑 application-local.yml，填入 DB_URL / DB_USERNAME / DB_PASSWORD
# 也可以用同名环境变量覆盖

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
    │   │   ├── AccessController.java        # /api/auth/access、/api/users（用户 CRUD）
    │   │   ├── RoleController.java          # /api/roles
    │   │   ├── MenuController.java          # /api/menus
    │   │   ├── DashboardController.java     # /api/dashboard/stats
    │   │   └── GroupChatController.java     # /api/groups（群列表 / 消息 / 发送 / 建群 / 加群）
    │   ├── dto/                             # 请求 / 响应对象
    │   ├── entity/                          # Account / Role / Menu，纯值对象（无 JPA 注解）
    │   └── service/
    │       ├── AuthService.java             # 登录/注册：user 表 + 三重 MD5
    │       ├── AccessService.java           # 角色 / 权限点 / 菜单装配
    │       ├── RoleService.java             # 用户与角色 CRUD
    │       ├── MenuService.java             # 菜单 CRUD
    │       ├── GroupChatService.java        # 群聊业务
    │       ├── DashboardService.java        # 统计聚合
    │       ├── StuStore.java                # ★ 唯一允许写 stu 表 SQL 的地方
    │       └── TokenStore.java              # 内存 token
    └── resources/application.yml            # 端口 8081 / MySQL 数据源
```

---

## 四、接口一览

| 方法 | 路径 | 权限点 | 说明 |
|---|---|---|---|
| GET | `/api/health` | — | 探活 |
| POST | `/api/login` | — | 登录，body `{username,password}` |
| POST | `/api/register` | — | 注册，body `{username,password,confirmPassword,name}` |
| POST | `/api/logout` | 登录即可 | 退出登录，作废 token |
| GET | `/api/me` | 登录即可 | 当前登录用户 |
| GET | `/api/auth/access` | 登录即可 | 当前用户的角色、权限点、菜单 |
| GET | `/api/users` | `user:read` | 用户列表（`keyword` 可搜索姓名或账号） |
| POST | `/api/users` | `user:add` | 新增用户 |
| PUT | `/api/users/{id}` | `user:edit` | 编辑用户 |
| DELETE | `/api/users/{id}` | `user:delete` | 删除用户 |
| PUT | `/api/users/{id}/role` | `user:assign-role` | 绑定 / 替换角色 |
| GET | `/api/roles` | `role:read` | 角色列表 |
| GET | `/api/roles/options` | `role:read` | 角色下拉选项 |
| POST / PUT / DELETE | `/api/roles[/{id}]` | `role:create` / `role:edit` / `role:delete` | 角色增删改 |
| GET | `/api/roles/{id}/permissions` | `role:read` | 角色已授权的菜单 id |
| PUT | `/api/roles/{id}/permissions` | `role:grant` | 保存角色权限 |
| GET | `/api/menus` | `menu:read` | 菜单树 |
| GET | `/api/menus/permission-tree` | `menu:read` | 权限树（含按钮节点） |
| POST / PUT / DELETE | `/api/menus[/{id}]` | `menu:create` / `menu:edit` / `menu:delete` | 菜单增删改 |
| GET | `/api/dashboard/stats` | `dashboard:view` | 统计卡与最近消息趋势 |
| GET | `/api/groups` | `chat:access` | 群列表（普通用户仅已加入，`super_admin` 全站 + `joined` 标记） |
| GET | `/api/groups/candidates` | `chat:access` | 可拉入群的候选人 |
| GET | `/api/groups/{id}` | `chat:access` | 群详情 |
| GET | `/api/groups/{id}/messages` | `chat:access` | 历史消息，`beforeId` 向上翻页 |
| POST | `/api/groups/{id}/messages` | `chat:access` | 发送消息，`clientMessageId` 做幂等 |
| POST | `/api/groups` | `chat:access` | 建群 |
| POST | `/api/groups/{id}/join` | `chat:access` | 加入群 |

群聊接口的请求 / 响应细节见 [`CHAT_API.md`](./CHAT_API.md)。

### 统一响应格式

```json
{ "code": 200, "msg": "success", "data": {} }
```

- `code == 200` 成功；`401` 未登录、`403` 无权限、`400` 参数错误、`404` 数据不存在、`500` 服务器错误
- 需要鉴权的接口请在请求头带 `token: <登录返回的 token>`
- 业务错误统一返回 **HTTP 200 + 业务 code**，方便前端 axios 拦截器统一处理

### 注册规则

注册接口在服务端会再校验一遍（前端校验只是提升体验，不能当安全边界）：

| 字段 | 规则 |
|---|---|
| username | 4-20 位字母 / 数字 / 下划线，且未被占用 |
| password | 6-32 位，必须同时包含字母和数字 |
| confirmPassword | 必须与 password 一致 |
| name | 可留空，默认与 username 相同 |

密码按 stu 的规则做三重 MD5（`md5(md5(md5(pwd)))`）后写入 `user.passwd`，与 stu 登录共用同一份凭据。

---

## 五、快速验证

PowerShell：

```powershell
$base = "http://localhost:8081"

# 探活
Invoke-RestMethod "$base/api/health"

# 登录（换成数据库里真实存在的账号）
$login = Invoke-RestMethod "$base/api/login" -Method Post -ContentType 'application/json' `
  -Body '{"username":"<account>","password":"<password>"}'
$token = $login.data.token

# 带 token 调受保护接口
Invoke-RestMethod "$base/api/users" -Headers @{ token = $token }
Invoke-RestMethod "$base/api/dashboard/stats" -Headers @{ token = $token }
Invoke-RestMethod "$base/api/groups" -Headers @{ token = $token }
```

---

## 六、数据访问约定

- **不建任何自己的表**：账号/角色/菜单/群聊全部直接用 stu 的 `user`、`sys_role`、`sys_user_role`、
  `sys_menu`、`sys_role_menu`、`chat_group`、`chat_member`、`chat_message`。
- 数据访问只有一层 `StuStore`（`JdbcTemplate` + 显式 SQL），项目不引 JPA，
  不存在 `ddl-auto` 去改别人表结构的可能。
- 涉及 stu 表的 SQL **只允许**出现在 `StuStore` 里，其他类不得直接注入 `JdbcTemplate` 写这些表。
- 登录直接用 stu 已有账号，不种入任何账号；注册写入的是同一张 `user` 表。
- H2 只作为集成测试的内存库（`test` scope），测试用 `src/test/resources/schema.sql` 造表，不碰远程库。

---

## 七、和前端联调

前端 `vue.config.js` 已配置开发代理：

```js
proxy: { '/api': { target: 'http://localhost:8081', changeOrigin: true } }
```

> 后端接口路径本身就带 `/api` 前缀，所以代理**不要**配 `pathRewrite`。

### CORS

`WebConfig` 对 `/api/**` 开放了跨域，所以前端不经代理、直接请求 8081 也能通。
本项目默认走 Vue CLI 代理（同源），跨域配置只是方便用 Postman 或换端口调试。

---

## 八、安全边界

以下都是简化实现，**不要直接上生产**：

- token 存在内存里，重启即失效，也没有过期 / 刷新机制
- 接口级鉴权由 `@RequirePermission` 完成；`super_admin` 直通，其余按 `sys_user_role → sys_role_menu → sys_menu` 计算
- 注册没有验证码、没有邮箱激活
- 没有 HTTPS、限流、审计日志
