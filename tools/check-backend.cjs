const fs = require('node:fs')
const path = require('node:path')
const file = path.resolve(__dirname, '../.env.local')
if (fs.existsSync(file)) process.loadEnvFile(file)
const base = (process.env.VUE_APP_BACKEND_TARGET || 'http://118.25.25.164:8080').replace(/\/$/, '')
const token = process.env.BACKEND_CHECK_TOKEN || ''
const checks = [
  ['/api/auth/me', v => v && v.id != null],
  ['/api/auth/access', v => v && Array.isArray(v.permissions) && Array.isArray(v.menus)],
  ['/api/roles', Array.isArray],
  ['/api/roles/options', Array.isArray],
  ['/api/menus', Array.isArray],
  ['/api/menus/permission-tree', Array.isArray],
  ['/api/users?page=1', v => v && Array.isArray(v.items) && Number.isInteger(v.total)],
  ['/api/groups?afterId=0', Array.isArray],
  ['/api/tasks?page=1', v => v && Array.isArray(v.items)],
  ['/api/announcements?page=1', v => v && Array.isArray(v.items)],
  ['/api/reports?page=1', v => v && Array.isArray(v.items)],
  ['/api/notifications?page=1', v => v && Array.isArray(v.items) && typeof v.unread === 'number'],
  ['/api/dashboard?days=7', v => v && v.overview && Array.isArray(v.trend)],
  ['/api/conversations', Array.isArray]
]
async function main() {
  console.log(`教学后端: ${base}`)
  let failures = 0
  for (const [url, valid] of token ? checks : checks.slice(0, 1)) {
    try {
      const response = await fetch(base + url, {
        headers: token ? { satoken: token } : {}, signal: AbortSignal.timeout(10000)
      })
      const body = await response.json().catch(() => null)
      if (!token) {
        if (response.status !== 401) throw new Error(`未登录接口期望 401，实际 ${response.status}`)
        console.log('PASS 服务可连通，未登录鉴权正常；未验证受保护功能')
      } else if (!response.ok || !valid(body)) {
        failures++
        console.error(`FAIL ${url}: HTTP ${response.status}，权限、接口存在性或响应结构不匹配`)
      } else console.log(`PASS ${url}`)
    } catch (error) { failures++; console.error(`FAIL ${url}: ${error.message}`) }
  }
  if (!token) console.log('未设置 BACKEND_CHECK_TOKEN；教师使用有相应权限的测试账号才能检查全部读取接口。')
  console.log('WS、业务写入与真实 AI 生成需按课件人工验收；本工具没有执行这些操作。')
  if (failures) process.exitCode = 1
}
main().catch(error => { console.error(error.message); process.exitCode = 1 })
