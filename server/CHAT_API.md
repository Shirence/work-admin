# 群聊后端 API

中台的「群聊」页和 stu 项目共用同一份数据：`chat_group` / `chat_member` / `chat_message`。
消息是**服务端明文**（stu 的 WebSocket 直接写 `content`），所以中台发的消息 stu 能看到，
stu 发的消息中台也能看到。

- 权限点：`chat:access`（`sys_menu` 里「群组聊天」）；`super_admin` 走 `*`。
- 鉴权：请求头 `token`，由 `AuthInterceptor` 统一校验。
- 统一响应：`{ code, msg, data }`，HTTP 恒为 200，业务码见文末。

## 1. 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/groups` | 群列表。普通用户只返回自己加入的群；`super_admin` 返回全站群组 |
| GET | `/api/groups/candidates` | 建群可选用户（`id / name / account`），不要求 `user:read` |
| GET | `/api/groups/{id}` | 群详情：`group` + `members` |
| GET | `/api/groups/{id}/messages?beforeId=&limit=` | 历史消息，按 `id` 升序；`limit` 默认 50，上限 200 |
| POST | `/api/groups/{id}/messages` | 发送消息，body `{content, clientMessageId}` |
| POST | `/api/groups` | 建群，body `{name, memberIds[]}` |
| POST | `/api/groups/{id}/join` | 加入群聊 |

### 数据结构

```json
// 群
{ "id": 1, "name": "测试", "ownerId": 50, "ownerName": "凌文杰",
  "memberCount": 9, "lastMessage": "1", "lastSender": "李易",
  "lastAt": "2026-10-21T13:55:51.401736", "joined": true }

// 消息
{ "id": 41, "groupId": 1, "senderId": 50, "senderName": "凌文杰",
  "content": "hello", "mine": true, "sentAt": "2026-10-21T14:06:14.427213" }

// 成员
{ "id": 50, "name": "凌文杰", "account": "admin", "owner": true, "joinedAt": "2026-10-19T13:42:22.802615" }
```

时间统一为 `yyyy-MM-ddTHH:mm:ss[.SSSSSS]`（本地时间，前端 `new Date()` 直接解析）。

## 2. 规则

- **可见性**：读消息/发消息都要求是群成员；`super_admin` 可以旁路查看（列表里带 `joined=false`，页面显示「加入群聊」按钮）。
- **幂等**：`clientMessageId` 相同视为同一条，重复提交返回已存在的那条，不产生重复行。
- **内容**：去首尾空格后不能为空，长度 ≤ 2000 字；发送者 `sender_id / sender_name` 取自 token 对应的 `user`，不信请求体。
- **建群**：建群时把群主和 `memberIds` 一起写入 `chat_member`；`joined_after_id` / `history_start_id` 写 0，新成员可见全部历史。
- **排序**：群列表按「最后一条消息时间，没有则建群时间」倒序；消息按 `id` 升序。

## 3. 错误码

| code | 场景 |
|---|---|
| 200 | 成功 |
| 400 | 内容为空 / 超长 / 群名称不合法 / 成员不存在 |
| 401 | 未登录或 token 失效 |
| 403 | 不是群成员，或缺少 `chat:access` |
| 404 | 群不存在 |

## 4. 运行与验证

```bash
cd server
mvn spring-boot:run          # 或 java -jar target/work-admin-server-1.0.0.jar --server.port=8081

# 登录拿 token
TOKEN=$(curl -s --noproxy '*' -X POST http://127.0.0.1:8081/api/login \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"123456"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

curl -s --noproxy '*' http://127.0.0.1:8081/api/groups -H "token: $TOKEN"
curl -s --noproxy '*' "http://127.0.0.1:8081/api/groups/1/messages?limit=10" -H "token: $TOKEN"
curl -s --noproxy '*' -X POST http://127.0.0.1:8081/api/groups/1/messages \
  -H "token: $TOKEN" -H 'Content-Type: application/json' \
  -d '{"content":"hello","clientMessageId":"manual-1"}'
```

集成测试：`server/src/test/java/com/example/workadmin/GroupChatIntegrationTest.java`（8 项）。
测试用 H2 内存库 + `src/test/resources/schema.sql` 造 stu 的表，不碰远程 MySQL：

```bash
cd server && mvn test
```

## 5. 前端实现

`src/views/Chat.vue`：左侧群列表（名称、最后一条消息、成员数、时间），右侧消息流（自己的消息靠右、别人的靠左），
底部输入框（Enter 发送 / Shift+Enter 换行），顶部成员抽屉与新建群弹窗。
每 5 秒轮询一次当前群消息和群列表，不引入 WebSocket 也能做到近似实时。

## 6. 已废弃

`/api/chat/**`（设备注册、密钥分发、逐设备密文信封）整套已移除，
对应的 `wa_chat_*` 表不再使用。原因见 [`../ARCHITECTURE.md`](../ARCHITECTURE.md) 第 4 节：
端到端加密和中台/stu 共用的明文消息表无法共存，且多设备授权会让浏览器首登卡在 PENDING。
