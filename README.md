# Work Admin 课堂前端

只包含 Vue 前端，不包含业务后端、数据库或模型密钥。

统一教学后端：http://118.25.25.164:8080，通过开发代理访问。

## 运行

需要 Node.js 22.13+、pnpm 10.32.1。

```bash
pnpm install --frozen-lockfile
```

将 `.env.example` 复制为 `.env.local` 后：

```bash
pnpm serve
```

以终端实际前端地址为准，默认 http://localhost:8083/#/login。

## 检查

```bash
pnpm test
pnpm build
pnpm check:backend
pnpm exec playwright install chromium
pnpm test:e2e
```

默认浏览器检查只测试未登录流程。个人测试账号通过私有环境文件中的 SMOKE_ACCOUNT / SMOKE_PASSWORD 配置，不能提交密码。
完整业务验收需要教学后端提供所有接口、权限和 AI SSE。不要用前端假数据掩盖接口缺失。
构建得到静态 dist/；生产环境需要另行转发 /api/ 与 /ws/chat。
