# 聊天后端 API（E2EE / 多设备 / 多端同步）

> 实现位置：`server/src/main/java/com/example/workadmin`
> 测试：`server/src/test/java/com/example/workadmin/ChatFlowIntegrationTest.java`（12 项，已通过）
> 最后验证：2026-10-19

## 1. 安全边界

- 服务端**只保存密文**。消息正文在 `chat_envelope.body`，按目标设备逐条加密；
  `chat_message` 只有元数据（发送者、`serverSeq`、时间、`clientMessageId`）。
- 服务端只接收公钥：identity key、signed pre-key、one-time pre-key。
  身份私钥、签名私钥、预共享密钥私钥永远留在客户端。
- 登录 token 仍然是现有 `TokenStore` 的 32 位 UUID，聊天身份 = 登录 `username`。
- 管理员接口通过 `ChatAccess.requireAdmin` 校验 role=admin。
- 当前 token 无过期/刷新，生产化前需替换为 JWT/Redis；服务端也不做明文内容审核。

## 2. 持久化

- 主库：远程 MySQL `118.25.25.164:3306/demo`（账号 `stu`），凭据在 `server/application-local.yml`（已 gitignore）。
- 本项目所有表统一加 `wa_` 前缀（如 `wa_chat_message`、`wa_sys_account`），
  与同库中 stu 项目的 `sys_role` / `chat_message` 等表完全隔离，互不影响。
- 数据库驱动：`mysql-connector-j`；H2 只保留为集成测试用的内存库（`test` scope）。
- `spring.jpa.hibernate.ddl-auto=update`，表由 JPA 自动创建。
- 没有可视控制台；用任意 MySQL 客户端或 `information_schema` 查 `wa_*` 表。

主要表（实际表名均带 `wa_` 前缀）：

| 表 | 作用 |
|---|---|
| `wa_chat_device` | 设备身份/签名公钥/状态（PENDING/ACTIVE/REVOKED） |
| `wa_chat_one_time_pre_key` | X3DH 一次性预共享公钥，分发一次即消费 |
| `wa_chat_device_link` | 新设备授权申请 |
| `wa_chat_device_grant` | 旧设备给新设备的加密历史授权（服务端仅存密文） |
| `wa_chat_conversation` | 私聊/群聊、群版本号 `version`、`last_message_seq` |
| `wa_chat_member` | 成员与账号级已读游标 |
| `wa_chat_read_cursor` | 每设备已读游标 |
| `wa_chat_message` | 消息元数据与 `serverSeq` |
| `wa_chat_envelope` | 逐设备密文信封 |
| `chat_group_policy` | 全局建群策略（单行） |
| `chat_account_group_permission` | 账号级建群覆盖 |

## 3. 设备与密钥

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/chat/devices/register` | 注册/更新设备；账号第一台自动 ACTIVE，之后为 PENDING |
| GET | `/api/chat/devices` | 当前账号设备列表 |
| GET | `/api/chat/devices/pending?deviceKey=` | 待授权设备（需 ACTIVE 设备） |
| POST | `/api/chat/devices/approve` | 已有设备批准新设备，可附带历史授权密文 |
| POST | `/api/chat/devices/reject` | 拒绝新设备 |
| POST | `/api/chat/devices/revoke` | 撤销设备（不能撤销当前设备） |
| GET | `/api/chat/devices/grants?deviceKey=` | 新设备拉取并消费历史授权密文 |
| POST | `/api/chat/devices/heartbeat` | 更新 lastSeen |
| GET | `/api/chat/users/{username}/device-bundles` | 获取目标账号所有 ACTIVE 设备的密钥包，消费 OTPK |

注册请求示例：

```json
{
  "clientDeviceKey": "uuid-...",
  "deviceName": "Chrome / Windows",
  "registrationId": 1234,
  "identityKey": "base64(33B)",
  "signedPreKeyId": 1,
  "signedPreKeyPublic": "base64(33B)",
  "signedPreKeySignature": "base64(64B)",
  "oneTimePreKeys": [{"keyId": 1, "publicKey": "base64(33B)"}]
}
```

新设备处理流程：

1. 注册返回 `pendingApproval=true` + `status=PENDING`。
2. PENDING 设备调用同步/发送/已读均返回 `403`。
3. 已有 ACTIVE 设备调用 `/approve`，可附带一个或多个
   `{"cipherType":2,"body":"base64(只对新设备可解的密文)"}`。
4. 新设备 ACTIVE 后拉取 `/grants`，解密后获得旧消息所需历史密钥。
5. 旧消息本身没有发给新设备的信封，因此客户端需用授权材料重放/重加密历史；
   服务端不会为了新设备重新明文分发历史消息。

## 4. 会话与群聊

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/chat/conversations/private` | body `{targetUsername}`，幂等获取私聊 |
| POST | `/api/chat/conversations/group` | body `{name, memberUsernames[]}`，需建群权限 |
| GET | `/api/chat/conversations?deviceKey=` | 会话列表，含未读数 |
| GET | `/api/chat/conversations/{id}?deviceKey=` | 会话详情 |
| POST | `/api/chat/conversations/{id}/members` | 群主加人，`version++` |
| POST | `/api/chat/conversations/{id}/members/remove` | 群主移人，`version++` |
| POST | `/api/chat/conversations/{id}/leave` | 成员退群，`version++` |
| POST | `/api/chat/conversations/{id}/rename` | 群主改名，`version++` |
| POST | `/api/chat/conversations/{id}/read` | body `{deviceKey,lastReadSeq}`，设备级已读游标 |

私聊唯一键为排序后的 `usernameA|usernameB`，同一对账号只有一条会话。

## 5. 消息与同步

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/chat/messages` | 发送消息（逐设备密文） |
| GET | `/api/chat/messages?conversationId=&deviceKey=&afterSeq=&limit=` | 增量拉取 |
| GET | `/api/chat/messages/history?conversationId=&deviceKey=&limit=` | 向更早翻页 |
| GET | `/api/chat/sync/state?deviceKey=` | 多端同步状态（版本号/最后序号/游标/未读） |

发送请求：

```json
{
  "conversationId": 1,
  "clientMessageId": "client-uuid-1",
  "senderDeviceKey": "uuid-sender-device",
  "contentType": "TEXT",
  "expectedVersion": 4,
  "envelopes": [
    {"targetDeviceKey": "uuid-device-a", "cipherType": 3, "body": "base64-ciphertext"},
    {"targetDeviceKey": "uuid-device-b", "cipherType": 2, "body": "base64-ciphertext"}
  ]
}
```

规则：

- 幂等键：`(conversationId, senderUsername, senderClientDeviceKey, clientMessageId)`。
  同一键重复提交返回首次结果，`duplicate=true`，不会重复分配 `serverSeq`。
- `serverSeq` 在会话行锁内单调分配；客户端按 `afterSeq` 增量拉取。
- `expectedVersion` 可选；与当前群版本不一致返回 `409`，用于成员已变化时强制刷新。
- 目标设备必须是会话成员的 ACTIVE 设备；必须覆盖“所有成员 ACTIVE 设备 - 发送设备自己”。
  缺设备副本或包含非成员设备都返回 `400`。
- 同步时只返回当前设备自己的信封；其他设备的密文不会下发。
- 已读游标按设备保存；`chat_member.last_read_seq` 保存账号级最大值用于通知聚合。

## 6. 建群权限

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/chat/group-permission` | 当前账号是否可建群 |
| GET/POST | `/api/chat/admin/group-policy` | 全局 `AUTO_ALLOW/AUTO_DENY/MANUAL` |
| GET/POST | `/api/chat/admin/account-permissions` | 账号级 `INHERIT/ALLOW/DENY` |
| GET | `/api/chat/admin/devices?username=` | 管理员查看设备 |
| POST | `/api/chat/admin/devices/reset` | 管理员重置账号设备（应急恢复） |

解析顺序：账号 `ALLOW` → 允许；账号 `DENY` → 拒绝；`INHERIT` 时跟随全局。
`MANUAL` 下默认拒绝，管理员把账号覆盖设为 `ALLOW` 才可建群。

## 7. 错误码

沿用统一响应 `{code,msg,data}`，HTTP 始终 200：

- `400` 参数/密文/设备副本不完整
- `401` 未登录（拦截器）
- `403` 非成员、设备未激活、无建群权限、非管理员
- `404` 账号/会话/设备不存在
- `409` 群版本冲突、设备已撤销等
- `500` 服务器错误

## 8. 运行与测试

```powershell
cd E:\my-project\work-admin\server
mvn test
mvn -DskipTests package
java -jar target\work-admin-server-1.0.0.jar
```

集成测试覆盖：登录、首台设备自动激活、密钥包消费 OTPK、私聊创建、
`clientMessageId` 幂等、逐设备密文分发、新设备 PENDING/授权/历史 grant、
设备级已读游标、建群全局/账号权限、群成员版本号、群消息 fanout、管理员重置设备。

## 9. 浏览器端 E2EE 实现

前端页面：`/chat`（[`Chat.vue`](../src/views/Chat.vue)）

- 设备初始化：`src/utils/chatCrypto.js` 用 `KeyHelper` 生成 identity key、signed pre-key、
  10 个 one-time pre-key；只有公钥通过 `/api/chat/devices/register` 上传。
- 私钥存储：`src/utils/signalStore.js` 实现 libsignal `StorageType`，按 `clientDeviceKey`
  存在浏览器 `localStorage`，不同账号/设备互不覆盖。
- 发送：对会话所有成员调用 `/api/chat/users/{username}/device-bundles`，
  排除当前设备后，对每台设备用 `SessionBuilder.processPreKey` + `SessionCipher.encrypt`
  生成一个信封；服务端要求覆盖所有 ACTIVE 目标设备。
- 接收：按 `serverSeq` 顺序调用 `decryptPreKeyWhisperMessage` /
  `decryptWhisperMessage`；解密后的明文按 message id / clientMessageId 缓存到本地。
- 多设备：第二台设备注册后 PENDING，现有设备在「设备」对话框批准；
  新设备只能解密批准后发给它的消息。
- 构建注意：`vue.config.js` 对 libsignal 的 curve25519 wasm 包设置 `resolve.fallback.fs=false`。
- 消息全部落在 MySQL，服务端只存逐设备密文。

已实测：Node 端两个真实 libsignal 设备通过该后端完成
`PreKeyWhisperMessage` 发送 → 对方解密 → `WhisperMessage` 回复 → 原设备解密。

## 10. 当前未实现 / 下一步

- WebSocket 实时推送（当前是 HTTP 增量同步）。
- 客户端 libsignal 加密/解密、设备本地密钥存储、历史重加密。
- 群主转让/解散群、消息撤回、附件对象存储。
- token 过期/刷新、接口级 RBAC、限流与审计。
