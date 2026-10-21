# 课件适配说明

对照 `D:\Downloads\work-admin-新功能课堂课件.md`，当前项目已完成可兼容的 RBAC 部分；
群聊与 AI SSE 因现有架构不同，暂不直接照搬课件代码。

## 已完成

### 1. 用户与账号管理

- 登录账号统一在「用户管理」页管理 MySQL 里的 `wa_sys_account`：
  - 支持新增、编辑、改密、启用/停用、绑定角色、删除；
  - `GET/POST /api/users`、`PUT/DELETE /api/users/{id}`、`PUT /api/users/{id}/role`。
- 业务用户 `sys_user` 的旧前端表格已移除，避免和登录账号管理重复；后端旧接口保留但不在导航中使用。
- 仪表盘「账号总数」「角色分布」使用真实 `sys_account` 数据。
- 早期引入的外部 MySQL 租户账号与远程群聊已整体下线（含 `remote-admin` / `remote-auth` / `remote-chat`
  接口与三个远程页面），项目只依赖远程 MySQL（`demo` 库、`wa_` 前缀表）；启动时会自动清理遗留的 `remote:*` 权限节点。

### 2. 权限 / 注册 / 角色绑定

- `GET /api/auth/access` 返回 `{role, permissions, menus}`。
- 前端 `store.state.access` + `getters.can` + `refreshAccess`。
- 路由改为 `meta.permission`，无权限进入 `/forbidden`。
- `Layout` 按权限过滤导航。
- 用户页支持通过 `PUT /api/users/{id}/role` 绑定角色。
- 注册账号默认绑定 `viewer` 角色。

### 3. 角色管理

- `GET/POST/PUT/DELETE /api/roles`
- `GET /api/roles/options`
- `GET/PUT /api/roles/{id}/permissions`
- `Roles.vue` 支持角色 CRUD 和权限树回显、保存；保存时自动补齐祖先节点。

### 4. 菜单与按钮权限

- `GET/POST/PUT/DELETE /api/menus`
- `GET /api/menus/permission-tree`
- `Menus.vue` 支持目录 / 页面 / 按钮三类节点、树形表格、内置节点保护。
- 后端 `@RequirePermission` 统一校验；前端 `can()` 控制按钮。

## 与课件不同、暂不直接实现

### 群聊 WebSocket

课件使用：
- `GET/POST /api/groups`
- `WS /ws/chat`
- 明文 `chat_message`

当前项目已有一套更强的聊天实现：
- 本地 `/chat`：libsignal E2EE，逐设备密文、会话版本、已读游标。

直接加课件式明文群聊会破坏 E2EE 边界并产生第二套聊天模型。
如果要做实时能力，建议给现有 E2EE 聊天加 WebSocket 只做「有新消息」通知，
消息体仍走现有加密接口，而不是照搬 `/ws/chat` 明文协议。

### AI SSE

课件使用 `POST /api/messages` + SSE，且要求后端配置 AI 密钥。
当前项目没有 AI 模块和密钥；可以先实现：
- 前端 SSE 解析器；
- 后端无密钥时返回 503；
- 配置密钥后再接真实模型。

在用户明确需要 AI 页面前，暂不新增空页面。

## 验证

```powershell
cd E:\my-project\work-admin\server
mvn test          # 12 项聊天集成测试通过
mvn -DskipTests package
cd ..
npm run build
```

实测：
- admin：仪表盘 / 消息 / 用户管理 / 角色管理 / 菜单管理
- manager：仪表盘 / 消息 / 用户管理；访问 `/roles` → `/forbidden`
- viewer：仪表盘 / 消息；访问 `/user` → `/forbidden`
- 后端权限：manager `/api/roles` → `code=403`；viewer `/api/user/list` → `code=403`
