# 底层架构（重新整理版）

> 这份文档定义「谁拥有数据、代码怎么分层、接口边界在哪」。
> 之前的问题出在：中台自带了一套账号/角色/菜单（`wa_sys_*`），又去读写 stu 的真实表，
> 两套身份体系并存，导致「连了远程库却只有 3 个假用户」。现在统一为一套。

## 1. 数据归属：谁的表，谁负责

| 归属 | 表 | 中台怎么用 |
|---|---|---|
| **stu 项目（共享，真实数据）** | `user`、`sys_role`、`sys_user_role`、`sys_menu`、`sys_role_menu` | **只通过 `StuStore` 的显式 SQL 读写**，不挂 JPA 实体，绝不让 Hibernate 去改表结构 |
| **stu 项目（业务数据）** | `chat_group`、`chat_member`、`chat_message` | 同上，只读统计 + 展示 |
| **中台自有** | `wa_chat_*`（设备、会话、密文信封等 E2EE 表） | 正常 JPA 实体 + Repository，`ddl-auto=update` 只管这些表 |

结论：**账号、角色、菜单、群聊消息全部是 stu 的真实数据**，中台不再有「本地用户」这个概念。
`wa_sys_account` / `wa_sys_role` / `wa_sys_menu` / `wa_sys_role_menu` / `wa_sys_user` 已停用（表保留在库里，没有 DROP 权限，也不再被任何代码引用）。

## 2. 代码分层

```
controller/        只做三件事：收参数、挂 @RequirePermission、返回 Result
   │
service/           业务规则都在这一层
   ├─ AuthService        登录/注册：user 表 + 三重 MD5
   ├─ AccessService      身份装配：user → sys_user_role → sys_role → sys_role_menu，产出权限点
   ├─ RoleService        用户 CRUD + 角色 CRUD + 授权
   ├─ MenuService        sys_menu CRUD
   ├─ DashboardService   真实计数
   └─ StuStore           ★ 唯一允许出现 stu 表 SQL 的地方
repository/        只有中台自己的 wa_chat_* 才允许出现 Spring Data 仓储
entity/            Account / Role / Menu 已降级为**值对象**（无 JPA 注解），只用于层间传递
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
- 权限校验：`@RequirePermission` + `AuthInterceptor` → `AccessService.require`，逻辑不变。

## 4. 接口边界（前端只认这些）

| 能力 | 接口 | 数据来源 |
|---|---|---|
| 登录 / 注册 / 退出 / 当前用户 | `/api/login`、`/api/register`、`/api/logout`、`/api/me` | `user` |
| 权限与菜单 | `GET /api/auth/access` | `sys_user_role` / `sys_role_menu` / `sys_menu` |
| 用户管理 | `GET/POST /api/users`、`PUT/DELETE /api/users/{id}`、`PUT /api/users/{id}/role` | `user` + `sys_user_role` |
| 角色管理 | `/api/roles`（含 `/options`、`/{id}/permissions`） | `sys_role` + `sys_role_menu` |
| 菜单管理 | `/api/menus`（含 `/permission-tree`） | `sys_menu` |
| 仪表盘 | `GET /api/dashboard/stats` | `user` / `chat_group` / `chat_message` / `chat_member` |
| E2EE 聊天 | `/api/chat/**` | `wa_chat_*` |

用户行字段从「username + email + enabled」改为真实的「name + account + 角色」：
`stu.user` 只有 `id / name / account / passwd` 四列，没有邮箱、没有启停位，接口不再假装有。

## 5. 安全与护栏

- 删除用户：不能删自己、不能删最后一个 `super_admin`、不能删还有群聊数据的用户。
- 修改/删除 `super_admin` 账号：只有 `super_admin` 本人可以操作。
- 角色：内置角色（`builtin=1`）不能删除、不能改编码、不能停用；仍有用户绑定时不能删。
- `super_admin` 不走逐条授权（否则一旦误删 `sys_role_menu` 就会把自己锁死）。
- 数据库账号 `stu` 没有 DROP 权限：任何「删表」都不可能发生，代码里也不做这类操作。

## 6. 迁移后的自检清单

```bash
curl -s --noproxy '*' -X POST http://127.0.0.1:8081/api/login \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"123456"}'
# 期望：permissions 含 "*"，roleCode=super_admin

curl -s --noproxy '*' http://127.0.0.1:8081/api/users -H "token: <token>"
# 期望：27 条真实用户（id 50 起），带 name / account / roleName

curl -s --noproxy '*' http://127.0.0.1:8081/api/dashboard/stats -H "token: <token>"
# 期望：用户 27、群组 10、消息 16、成员 49
```
