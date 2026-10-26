# 底层架构（重新整理版）

> 这份文档定义「谁拥有数据、代码怎么分层、接口边界在哪」。
> 两次返工的根因都是同一件事：中台自己又造了一套数据（`wa_sys_*` 账号体系、`wa_chat_*` 端到端加密会话），
> 和 stu 的真实数据并行存在，于是「连了远程库却只有 3 个假用户」「消息页看不到真实群消息」。
> 现在只有一套数据：**全部来自 stu 的真表**。

## 1. 数据归属：只用 stu 的表，一行都不自己建

| 用途 | 表（全部属于 stu） | 中台怎么用 |
|---|---|---|
| 账号 / 角色 / 权限 / 菜单 | `user`、`sys_role`、`sys_user_role`、`sys_menu`、`sys_role_menu` | **只通过 `StuStore` 的显式 SQL 读写** |
| 群聊 | `chat_group`、`chat_member`、`chat_message` | 同上，读写统一的聊天数据 |

- 项目**不引入 JPA**（`spring-boot-starter-jdbc` + `JdbcTemplate` 显式 SQL），
  所以不存在 `ddl-auto` 去 `ALTER` 别人家表的风险。
- 也没有任何自建表：`wa_sys_*`、`wa_chat_*` 都是历史遗留，代码已完全不引用（表还在库里，`stu` 账号没有 DROP 权限）。

## 2. 代码分层

```
controller/        只做三件事：收参数、挂 @RequirePermission、返回 Result
   │
service/           业务规则都在这一层
   ├─ AuthService        登录/注册：user 表 + 三重 MD5
   ├─ AccessService      身份装配：user → sys_user_role → sys_role → sys_role_menu，产出权限点
   ├─ RoleService        用户 CRUD + 角色 CRUD + 授权
   ├─ MenuService        sys_menu CRUD
   ├─ GroupChatService   群列表 / 历史消息 / 发送 / 建群 / 加群
   ├─ DashboardService   真实计数
   ├─ TokenStore         token 存取接口（create / get / remove）
   │   ├─ RedisTokenStore    ★ 默认：token 存 Redis，服务重启后依然有效
   │   └─ MemoryTokenStore   降级：进程内存，仅离线开发 / 测试
   └─ StuStore           ★ 唯一允许出现 stu 表 SQL 的地方
entity/            Account / Role / Menu 是**值对象**（无 JPA 注解），只用于层间传递
dto/               对外契约，字段与前端一一对应
```

**一条硬规则**：任何 `select/insert/update/delete` 只要涉及 stu 的表，必须写在 `StuStore` 里。
新增需求时先加 Store 方法，再在 Service 里用，不允许 Controller 或别的 Service 直接写 SQL。

## 3. 身份与权限

- 登录：`user.account` + `user.passwd`，密码算法与 stu 完全一致
  （`md5(md5(md5(明文)))`，兼容历史单次 md5）——见 `common/StuPassword`。
- 角色：`sys_role`（`super_admin` / `member` / `coursedemo_readonly`）。
- 权限点：`sys_role_menu` → `sys_menu.permission`。
  - `super_admin` 在 `sys_role_menu` 里没有逐条授权，按约定直接给 `*`。
  - 所有登录用户额外获得基础权限 `dashboard:view`，否则 `member` 连概览页都进不去。
- 权限校验：`@RequirePermission` + `AuthInterceptor` → `AccessService.require`。
- token：登录成功后由 `TokenStore` 生成，**存在 Redis 里**（Redisson，key `work-admin:login:token:<uuid>`，
  值是 `UserInfo` 的 JSON，TTL 默认 7 天、每次访问滑动续期）。
  - 因为不落在进程内存，**重启服务 / 换个实例部署都不会把用户踢下线**。
  - `AuthInterceptor` 每次请求用请求头 `token` 去 Redis 换身份；换不到直接返回 `code=401`。
  - `POST /api/logout` 删掉 Redis 里的 key，token 立即失效。
  - 本机没有 Redis 时可设 `work-admin.auth.token-store=memory` 退回内存实现（重启即失效，仅用于离线开发与测试）。

## 4. 群聊为什么不做端到端加密

stu 的聊天是**服务端明文**：WebSocket 收到消息直接写 `chat_message.content`，
其他客户端读同一张表再渲染。中台要和它共用一份会话，就只能读明文。

之前那套 E2EE（`wa_chat_*` + libsignal）有两个死结：

1. 它只加密中台自己写进 `wa_chat_*` 的消息，stu 的 35 条真实消息在另一张表里，永远显示不出来；
2. 多设备模型要求「新设备由老设备授权」，账号在浏览器里首次登录根本没有授权方，页面就卡在 PENDING。

所以 E2EE 整套已移除（含 `libsignal` 依赖、7 个控制器、4 个服务、11 张表映射与 12 项集成测试），
换来一条真正可用的链路：**中台发的消息 stu 能看到，stu 发的消息中台能看到**。

群聊接口规则：

- `GET /api/groups`：普通用户只看自己加入的群；`super_admin` 看全站群组（带 `joined` 标记）。
- `GET /api/groups/{id}/messages`：必须是群成员（`super_admin` 例外），按 `id` 升序返回，`beforeId` 向上翻页。
- `POST /api/groups/{id}/messages`：`client_message_id` 去重，重复提交不会产生第二条。
- `POST /api/groups`：建群同时把群主和邀请的人写进 `chat_member`。
- 权限点统一 `chat:access`（对应 `sys_menu` 里「群组聊天」）。

## 5. 接口边界（前端只认这些）

| 能力 | 接口 | 数据来源 |
|---|---|---|
| 登录 / 注册 / 退出 / 当前用户 | `/api/login`、`/api/register`、`/api/logout`、`/api/me` | `user` |
| 权限与菜单 | `GET /api/auth/access` | `sys_user_role` / `sys_role_menu` / `sys_menu` |
| 用户管理 | `GET/POST /api/users`、`PUT/DELETE /api/users/{id}`、`PUT /api/users/{id}/role` | `user` + `sys_user_role` |
| 角色管理 | `/api/roles`（含 `/options`、`/{id}/permissions`） | `sys_role` + `sys_role_menu` |
| 菜单管理 | `/api/menus`（含 `/permission-tree`） | `sys_menu` |
| 仪表盘 | `GET /api/dashboard/stats` | `user` / `chat_group` / `chat_message` / `chat_member` |
| 群聊 | `/api/groups`（列表、`/candidates`、`/{id}`、`/{id}/messages`、`/{id}/join`） | `chat_group` / `chat_member` / `chat_message` |

用户行字段是真实的「name + account + 角色」：
`stu.user` 只有 `id / name / account / passwd` 四列，没有邮箱、没有启停位，接口不再假装有。

## 6. 安全与护栏

- 删除用户：不能删自己、不能删最后一个 `super_admin`、不能删还有群聊数据的用户。
- 修改/删除 `super_admin` 账号：只有 `super_admin` 本人可以操作。
- 角色：内置角色（`builtin=1`）不能删除、不能改编码、不能停用；仍有用户绑定时不能删。
- `super_admin` 不走逐条授权（否则一旦误删 `sys_role_menu` 就会把自己锁死）。
- 数据库账号 `stu` 没有 DROP 权限：任何「删表」都不可能发生，代码里也不做这类操作。
- 群聊发送只接受登录用户，内容长度上限 2000 字，发送者身份取自 token 而非请求体。
- token 只存身份快照（`UserInfo`），不存密码；Redis 里的 key 一律带 `work-admin:login:token:` 前缀和 TTL，
  不会长期堆积，也不会和 stu 自己的 key 混在一起。
- `work-admin.auth.token-store=redis`（默认）时，Redis 连不上服务直接启动失败——这是故意的，
  否则会静默退化成「重启就掉登录」，而用户以为登录态是持久化的。

## 7. 自检清单

```bash
curl -s --noproxy '*' -X POST http://127.0.0.1:8081/api/login \
  -H 'Content-Type: application/json' -d '{"username":"<account>","password":"<password>"}'
# 期望：permissions 含 "*"，roleCode=super_admin

curl -s --noproxy '*' http://127.0.0.1:8081/api/users -H "token: <token>"
# 期望：真实用户列表（id 50 起），带 name / account / roleName

curl -s --noproxy '*' http://127.0.0.1:8081/api/groups -H "token: <token>"
# 期望：stu 的真实群组，带 memberCount / lastMessage

curl -s --noproxy '*' "http://127.0.0.1:8081/api/groups/1/messages?limit=10" -H "token: <token>"
# 期望：该群的真实历史消息，mine 标记自己发的

curl -s --noproxy '*' -X POST http://127.0.0.1:8081/api/groups/1/messages \
  -H "token: <token>" -H 'Content-Type: application/json' \
  -d '{"content":"hello","clientMessageId":"self-check-1"}'
# 期望：code=200，消息进 chat_message，stu 前端同步可见

# token 是否真的落在 Redis（应看到 work-admin:login:token:<uuid>，TTL 接近 7 天）
redis-cli -h 118.25.25.164 --scan --pattern 'work-admin:login:token:*'
redis-cli -h 118.25.25.164 ttl 'work-admin:login:token:<uuid>'
# 重启后端后，用同一个 token 再请求 /api/me
# 期望：code=200，登录态没有被重启清掉
curl -s --noproxy '*' http://127.0.0.1:8081/api/me -H "token: <token>"
```
