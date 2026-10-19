# 远程 MySQL：远程账号群聊

> H2 是本地主库；远程 MySQL 只服务独立的远程账号体系。
> 远程登录后只显示远程消息，本地登录后只显示本地消息，两者完全独立。
>
> 最后验证：2026-10-19（远程 `demo` 库；远程账号登录、建群、双人聊天、幂等、权限隔离均通过）

## 1. 配置

远程连接配置在 **`server/application-local.yml`**（已加入 `.gitignore`）：

```yaml
workadmin:
  remote-mysql:
    enabled: true
    url: "jdbc:mysql://118.25.25.164:3306/demo?allowPublicKeyRetrieval=true&useSSL=false&characterEncoding=UTF-8&connectionTimeZone=Asia/Shanghai&connectTimeout=5000&socketTimeout=10000"
    username: stu
    password: "******"
    query-timeout-seconds: 10
    max-rows: 500
```

也可以用环境变量：

```powershell
$env:REMOTE_MYSQL_ENABLED='true'
$env:REMOTE_MYSQL_URL='jdbc:mysql://host:3306/demo?...'
$env:REMOTE_MYSQL_USERNAME='stu'
$env:REMOTE_MYSQL_PASSWORD='******'
```

`application.yml` 默认 `enabled=false`。未配置时 H2 业务照常启动，远程登录/群聊接口返回 503。

## 2. 连接池 / 事务边界

| 组件 | 用途 | 特性 |
|---|---|---|
| `remoteMysqlDataSource` | 远程登录查询 | readOnly=true，最多 3 连接 |
| `remoteChatDataSource` | 远程群聊写入 | 读写，最多 5 连接 |
| `remoteChatTransactionManager` | 远程群聊事务 | READ_COMMITTED，独立于 H2 |
| 主 `dataSource` + `transactionManager` | H2 / JPA | `@Primary`，本地业务 |

远程池初始化失败不会阻止应用启动；远程不可用时业务层返回 503。H2 的 JPA 事务管理器显式声明为 primary，避免和远程事务管理器冲突。

## 3. 安全边界

- 远程账号登录 `/api/remote-auth/login`：公开接口，账号密码校验远程 `user` 表。
- 远程群聊 `/api/remote-chat/**`：必须 `authSource=REMOTE` 的 token。
- 远程 token 只能访问 `/api/remote-auth/**` 和 `/api/remote-chat/**`；
  访问本地 H2 用户/聊天接口会被 `AuthInterceptor` 拒绝（403）。
- 本地 token 访问远程群聊会被 `RemoteChatController` 拒绝（403）。
- 远程登录密码格式与 stu 项目一致：连续三次 MD5，兼容旧单次 MD5；不在登录时改写远程密码。
- 远程聊天消息在 `chat_message` 中是**明文**（stu 库本身如此），不是 E2EE。
- 凭据不进入 Git；如果长期使用，建议把 `stu` 这个宽权限账号换成只授予所需表的专用账号。

## 4. 远程账号登录 / 注册

| 方法 | 路径 | 请求体 |
|---|---|---|
| POST | `/api/remote-auth/login` | `{"account":"admin","password":"..."}` |
| POST | `/api/remote-auth/register` | `{"name":"昵称","account":"...","password":"...","confirmPassword":"..."}` |
| GET | `/api/remote-auth/me` | 远程 token |
| POST | `/api/remote-auth/logout` | 远程 token |

注册行为与 stu 项目一致：

- 写入远程 `user(name, account, passwd)`，密码为三次 MD5；
- 自动绑定 `sys_role.code = 'member'`（普通用户）角色；
- 不允许指定角色，不能通过接口注册超级管理员；
- 账号唯一，重复注册返回 409；
- 注册成功后直接签发远程 token，可立即进入 `/remote-chat`。

登录返回与本地一致的 `{token, userInfo}`；`userInfo` 额外包含：

```json
{
  "username": "admin",
  "displayName": "凌文杰",
  "role": "remote",
  "authSource": "REMOTE",
  "remoteUserId": 50,
  "permissions": []
}
```

## 5. 远程群聊接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/remote-chat/groups?afterId=&limit=` | 所有公开群 + 当前远程用户是否已加入 |
| GET | `/api/remote-chat/groups/{id}` | 群详情 |
| POST | `/api/remote-chat/groups` | 创建群，body `{name}`，创建者自动入群 |
| POST | `/api/remote-chat/groups/{id}/join` | 幂等加入，记录入群历史边界 |
| GET | `/api/remote-chat/groups/{id}/messages?afterId=&limit=` | 已加入成员读取历史 |
| POST | `/api/remote-chat/groups/{id}/messages` | 发消息，body `{content, clientMessageId}` |

远程群聊行为对齐 stu 项目：

- 公开群，登录用户可查看列表并加入。
- 首次入群只能读入群前最近 30 条；更早消息不可读；入群后的消息全部可读。
- 加入和发送前对 `chat_group` 行加 `FOR UPDATE`，和 stu 项目的并发模型一致。
- 消息幂等键 `(group_id, sender_id, client_message_id)`；同编号不同正文返回 409。
- 发送者身份取自远程 token 的 `remoteUserId`，不信任客户端传入的 senderId。
- 消息写入后，stu 项目刷新历史即可看到；本项目页面用 3 秒轮询增量刷新（不是 WebSocket）。

## 6. 前端页面

| 路径 | 说明 |
|---|---|
| `/remote-login` | 远程 stu 账号登录（独立登录态） |
| `/remote-register` | 远程 stu 账号注册；注册成功自动登录 |
| `/remote-chat` | 远程群聊：群列表、加入、新建、历史、发消息 |
| `/chat` | 本地 H2 E2EE 聊天，只显示本地消息 |

远程聊天不出现在本地侧栏，只从 `/remote-login` 或 `/remote-register` 成功后进入。
远程登录态使用 `localStorage.remoteToken` / `remoteUserInfo`，与本地 `token` / `userInfo` 完全分离。

## 7. 远程表结构

远程群聊按 `stu` 当前 schema 查询/写入：

- `chat_group(id, name, owner_id, created_at)`
- `chat_member(group_id, user_id, joined_at, joined_after_id, history_start_id)`
- `chat_message(id, group_id, sender_id, sender_name, content, client_message_id, sent_at)`
- `user(id, name, account, passwd)`
- `sys_user_role(user_id, role_id)`、`sys_role(id, name, code)`

如果远程 schema 变化，修改 `RemoteChatService` / `RemoteAuthService` 中的 SQL。

## 8. 验证

```powershell
cd E:\my-project\work-admin\server
mvn test          # 12 项本地聊天测试；测试里 remote 默认关闭
mvn -DskipTests package
java -jar target\work-admin-server-1.0.0.jar
```

启动日志应包含：

```text
HikariPool-1 - Added connection conn0: url=jdbc:h2:file:./data/workadmin
remote-mysql-readonly - Start completed.
remote-mysql-chat - Start completed.
```

手工验证：

1. 打开 `http://localhost:8080/login`，点击「远程账号登录 →」。
2. 远程登录页右下角点击「注册账号 →」进入 `/remote-register`，填写昵称、账号、密码；注册成功后自动进入远程群聊。
3. 也可以直接用远程 `demo.user` 中已有账号登录。
4. 进入 `/remote-chat`，选择群、加入、发消息。
5. 另开浏览器用另一个远程账号登录同一群，验证消息可见。
