const fs = require('node:fs')
const path = require('node:path')
const { spawn } = require('node:child_process')
const { chromium } = require('playwright')
const assert = require('node:assert/strict')
const root = path.resolve(__dirname, '..')
const envFile = path.join(root, '.env.local')
if (fs.existsSync(envFile)) process.loadEnvFile(envFile)
const port = Number(process.env.SMOKE_PORT || 8093)
const base = `http://127.0.0.1:${port}`
async function main() {
  // Refuse to attach to an unrelated service occupying the test port.
  const occupied = await fetch(base, { signal: AbortSignal.timeout(1000) }).then(() => true, () => false)
  if (occupied) throw new Error(`测试端口 ${port} 已被使用，请通过 SMOKE_PORT 选择空闲端口`)
  let output = '', browser
  const frontend = spawn(process.execPath, [require.resolve('@vue/cli-service/bin/vue-cli-service.js'), 'serve', '--host', '127.0.0.1', '--port', String(port)], { cwd: root, env: process.env, stdio: ['ignore', 'pipe', 'pipe'] })
  frontend.stdout.on('data', data => { output += data })
  frontend.stderr.on('data', data => { output += data })
  try {
    let ready = false
    for (let i = 0; i < 120; i++) {
      if (frontend.exitCode !== null) throw new Error('前端启动失败，请运行 pnpm serve 查看错误')
      ready = await fetch(base, { signal: AbortSignal.timeout(1000) }).then(r => r.ok, () => false)
      if (ready) break
      await new Promise(resolve => setTimeout(resolve, 500))
    }
    if (!ready) throw new Error('前端未能在 60 秒内启动')
    browser = await chromium.launch({ headless: true })
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const page = await context.newPage(), pageErrors = []
    page.setDefaultTimeout(15000)
    page.on('pageerror', error => pageErrors.push(error.message))
    await page.goto(base + '/#/tasks')
    await page.getByRole('heading', { name: '欢迎回来', exact: true }).waitFor()
    console.log('PASS 未登录访问受保护路由返回登录页')
    await page.goto(base + '/#/register')
    await page.getByRole('heading', { name: '注册普通用户', exact: true }).waitFor()
    console.log('PASS 注册入口可访问，没有自动提交共享后端')
    await page.goto(base + '/#/login')
    await page.getByRole('heading', { name: '欢迎回来', exact: true }).waitFor()
    const account = process.env.SMOKE_ACCOUNT, password = process.env.SMOKE_PASSWORD
    if (account && password) {
      await page.getByRole('textbox', { name: '登录账号' }).fill(account)
      await page.getByRole('textbox', { name: '登录密码' }).fill(password)
      await page.getByRole('button', { name: '登 录', exact: true }).click()
      await page.getByRole('heading', { name: '工作概览', exact: true }).waitFor()
      await page.waitForLoadState('networkidle')
      if (await page.locator('.dashboard .el-alert--error').count()) throw new Error('仪表盘接口失败，请核对教学后端契约')
      console.log('PASS 个人测试账号登录与仪表盘读取')
      for (const [route, title] of [['user','用户管理'], ['roles','角色管理'], ['menus','菜单管理'], ['tasks','任务管理'], ['announcements','公告中心'], ['reports','工作周报'], ['notifications','通知中心']]) {
        await page.goto(`${base}/#/${route}`)
        await page.getByRole('heading', { name: new RegExp(`^${title}|页面暂时无法访问`) }).first().waitFor()
        await page.waitForLoadState('networkidle')
        const denied = await page.getByText('页面暂时无法访问', { exact: true }).count()
        if (!denied && await page.locator('.el-alert--error').count()) throw new Error(`${title}读取失败，请检查角色和接口`)
        console.log(`PASS ${title}: ${denied ? '按权限拒绝访问' : '已加载页面'}`)
      }
      await page.getByRole('button', { name: '退出登录', exact: true }).click()
      await page.getByRole('heading', { name: '欢迎回来', exact: true }).waitFor()
    } else console.log('SKIP 已认证业务联调：未设置个人 SMOKE_ACCOUNT / SMOKE_PASSWORD；不能据此声称全部功能已验收')
    await page.setViewportSize({ width: 390, height: 844 })
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
    assert.deepEqual(pageErrors, [])
    console.log('PASS 登录页 390px 无整体横向溢出，未出现 JavaScript 页面异常')
    console.log('写入操作、群聊收发、AI 真实流式、会话恢复与导出请按课件人工验收。')
  } finally {
    await browser?.close()
    if (frontend.exitCode === null) {
      const stopped = new Promise(resolve => frontend.once('exit', resolve))
      frontend.kill('SIGTERM')
      await stopped
    }
  }
}
main().catch(error => { console.error(error.message); process.exitCode = 1 })
