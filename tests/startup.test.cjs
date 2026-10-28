const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const { transformSync } = require('@babel/core')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const { createSSRApp } = require('vue')
const { renderToString } = require('@vue/server-renderer')
const vueRouter = require('vue-router')

const source = file => readFileSync(path.join(__dirname, '..', 'src', file), 'utf8')

function loadModule(code, overrides = {}) {
  const compiled = transformSync(code, {
    configFile: false,
    babelrc: false,
    plugins: ['@babel/plugin-transform-modules-commonjs', '@babel/plugin-transform-dynamic-import']
  }).code
  const module = { exports: {} }
  const load = id => {
    if (Object.hasOwn(overrides, id)) return overrides[id]
    if (id.endsWith('.vue')) return { template: '<div />' }
    return require(id)
  }
  new Function('require', 'module', 'exports', compiled)(load, module, module.exports)
  return module.exports
}

function makeRouter(token, options = {}) {
  const store = {
    state: { token },
    getters: { can: permission => (options.permissions || ['*']).some(value => value === '*' || value === permission) },
    dispatch: async action => {
      assert.equal(action, 'refreshAccess')
      if (options.refresh) await options.refresh(store)
    }
  }
  return loadModule(source('router/index.js'), {
    '../store': store,
    'vue-router': {
      ...vueRouter,
      createWebHashHistory: vueRouter.createMemoryHistory
    }
  }).default
}

for (const target of ['/', '/dashboard', '/user', '/roles', '/menus', '/chat', '/messages']) {
  test(`without a session, ${target} opens the login page`, async () => {
    const router = makeRouter('')
    await router.push(target)
    assert.equal(router.currentRoute.value.path, '/login')
  })
}

test('the login page stays accessible without a redirect loop', async () => {
  const router = makeRouter('')
  await router.push('/login')
  assert.equal(router.currentRoute.value.name, 'Login')
})

test('a saved session can open the dashboard', async () => {
  const router = makeRouter('test-session')
  await router.push('/dashboard')
  assert.equal(router.currentRoute.value.path, '/dashboard')
})

test('registration is accessible before logging in', async () => {
  const router = makeRouter('')
  await router.push('/register')
  assert.equal(router.currentRoute.value.name, 'Register')
})

test('all four placeholder pages are registered inside the admin layout', async () => {
  const router = makeRouter('test-session')
  for (const target of ['/roles', '/menus', '/chat', '/messages']) {
    await router.push(target)
    assert.equal(router.currentRoute.value.path, target)
    assert.equal(router.currentRoute.value.matched.length, 2)
  }
})

test('ordinary users can access chat and AI but cannot open administration routes directly', async () => {
  const router = makeRouter('test-session', { permissions: ['chat:access', 'message:access'] })
  for (const target of ['/chat', '/messages']) {
    await router.push(target)
    assert.equal(router.currentRoute.value.path, target)
  }
  for (const target of ['/user', '/roles', '/menus']) {
    await router.push(target)
    assert.equal(router.currentRoute.value.path, '/forbidden')
  }
})

test('permissions are refreshed before protected navigation is allowed', async () => {
  let refreshed = 0
  const permissions = []
  const router = makeRouter('test-session', {
    permissions,
    refresh: async () => { refreshed++; permissions.push('role:read') }
  })
  await router.push('/roles')
  assert.equal(refreshed, 1)
  assert.equal(router.currentRoute.value.path, '/roles')
})

test('access API failure ends on the forbidden page without a redirect loop', async () => {
  const router = makeRouter('test-session', { refresh: async () => { throw new Error('offline') } })
  await router.push('/dashboard')
  assert.equal(router.currentRoute.value.path, '/forbidden')
})

test('expired sessions return to login when refreshing access', async () => {
  const router = makeRouter('expired-session', {
    refresh: async store => { store.state.token = ''; throw new Error('unauthorized') }
  })
  await router.push('/roles')
  assert.equal(router.currentRoute.value.path, '/login')
})

test('unknown routes fall back to the dashboard', async () => {
  const router = makeRouter('test-session')
  await router.push('/missing-page')
  assert.equal(router.currentRoute.value.path, '/dashboard')
})

function registration(api) {
  const { descriptor } = parse(source('views/Register.vue'))
  const component = loadModule(descriptor.script.content, {
    '../api/auth': { registerApi: api }
  }).default
  const calls = []
  const vm = {
    ...component.data(),
    $message: Object.fromEntries(['warning', 'success', 'error'].map(kind => [
      kind, message => calls.push([kind, message])
    ])),
    $store: { commit: (...args) => calls.push(['commit', ...args]) },
    $router: { replace: async path => calls.push(['replace', path]) }
  }
  return { vm, calls, submit: () => component.methods.register.call(vm) }
}

test('registration submits only public account fields and saves the returned session', async () => {
  let payload
  const result = { tokenValue: 'test-session', user: { name: '测试用户' } }
  const form = registration(async data => { payload = data; return result })
  form.vm.form = { name: ' 测试用户 ', account: ' tester ', passwd: ' pass word ', role: 'admin' }
  await form.submit()
  assert.deepEqual(payload, { name: '测试用户', account: 'tester', passwd: ' pass word ' })
  assert.deepEqual(form.calls, [
    ['commit', 'setSession', result], ['success', '注册成功'], ['replace', '/dashboard']
  ])
  assert.equal(form.vm.busy, false)
})

test('blank account or password prevents registration', async () => {
  let requests = 0
  const form = registration(async () => { requests++ })
  for (const values of [{ account: ' ', passwd: 'password' }, { account: 'tester', passwd: ' ' }]) {
    Object.assign(form.vm.form, values)
    await form.submit()
  }
  assert.equal(requests, 0)
  assert.deepEqual(form.calls.map(call => call[0]), ['warning', 'warning'])
})

test('a pending registration cannot be submitted twice', async () => {
  let finish
  let requests = 0
  const form = registration(() => {
    requests++
    return new Promise(resolve => { finish = resolve })
  })
  Object.assign(form.vm.form, { account: 'tester', passwd: 'password' })
  const pending = form.submit()
  assert.equal(form.vm.busy, true)
  await form.submit()
  assert.equal(requests, 1)
  finish({ tokenValue: 'test-session', user: {} })
  await pending
  assert.equal(form.vm.busy, false)
})

test('failed registration keeps the form usable and does not create a session', async () => {
  const form = registration(async () => { throw { userMessage: '账号已存在' } })
  Object.assign(form.vm.form, { account: 'tester', passwd: 'password' })
  await form.submit()
  assert.deepEqual(form.calls, [['error', '账号已存在']])
  assert.equal(form.vm.busy, false)
})

const { descriptor } = parse(source('views/Layout.vue'))
const { code, errors } = compileTemplate({
  source: descriptor.template.content,
  filename: 'Layout.vue',
  id: 'startup-regression'
})
assert.deepEqual(errors, [])
const menu = loadModule(source('utils/menu.js'))
function makeLayout(logoutApi = async () => {}) {
  return {
    ...loadModule(descriptor.script.content, {
      '../api/auth': { logoutApi },
      '../utils/menu': menu
    }).default,
    render: loadModule(code).render
  }
}
const layout = makeLayout()

async function renderLayout(userInfo) {
  const app = createSSRApp(layout)
  app.config.globalProperties.$store = {
    state: { userInfo, token: '', access: { role: null, permissions: [], menus: [] } }
  }
  app.config.globalProperties.$route = { path: '/dashboard' }
  for (const name of ['ElContainer', 'ElAside', 'ElMenu', 'ElMenuItem', 'ElIcon',
    'House', 'User', 'ElHeader', 'ElButton', 'ElMain', 'RouterView']) {
    app.component(name, { template: '<div><slot /></div>' })
  }
  return renderToString(app)
}

test('missing user information does not crash the layout', async () => {
  assert.match(await renderLayout(null), /欢迎，用户/)
})

test('the layout displays the signed-in user name', async () => {
  assert.match(await renderLayout({ name: '测试用户' }), /欢迎，测试用户/)
})

test('clearing the session during logout leaves a renderable layout', async () => {
  const state = { token: 'test-session', userInfo: { name: '测试用户' } }
  let destination
  await layout.methods.logout.call({
    leaving: false,
    $store: { state, commit: () => { state.token = ''; state.userInfo = null } },
    $router: { replace: async target => { destination = target } }
  })
  assert.equal(destination, '/login')
  assert.match(await renderLayout(state.userInfo), /欢迎，用户/)
})

test('logout clears the local session even when the API fails', async () => {
  const component = makeLayout(async () => { throw new Error('offline') })
  const commits = []
  const destinations = []
  const vm = {
    leaving: false,
    $store: { state: { token: 'test-session' }, commit: value => commits.push(value) },
    $router: { replace: async value => destinations.push(value) }
  }
  await component.methods.logout.call(vm)
  assert.deepEqual(commits, ['clearUserInfo'])
  assert.deepEqual(destinations, ['/login'])
  assert.equal(vm.leaving, false)
})

test('a slow logout cannot clear a newer login session', async () => {
  let finish
  const component = makeLayout(() => new Promise(resolve => { finish = resolve }))
  const vm = {
    leaving: false,
    $store: { state: { token: 'old-session' }, commit: () => assert.fail('new session must be preserved') },
    $router: { replace: () => assert.fail('new session must not be redirected') }
  }
  const pending = component.methods.logout.call(vm)
  vm.$store.state.token = 'new-session'
  finish()
  await pending
  assert.equal(vm.leaving, false)
})

test('refreshing revoked permissions leaves the current protected page', async () => {
  const destinations = []
  const vm = {
    refreshing: false,
    $route: { meta: { permission: 'role:read' } },
    $store: {
      dispatch: async action => assert.equal(action, 'refreshAccess'),
      getters: { can: () => false }
    },
    $router: { replace: async value => destinations.push(value) }
  }
  await layout.methods.refresh.call(vm)
  assert.deepEqual(destinations, ['/forbidden'])
  assert.equal(vm.refreshing, false)
})

test('navigation hides buttons, unknown pages and empty directories', () => {
  const rows = [
    { id: 1, parentId: null, sortOrder: 1, type: 'DIRECTORY', name: '协作' },
    { id: 2, parentId: 1, sortOrder: 2, type: 'MENU', path: '/chat.html' },
    { id: 3, parentId: 1, sortOrder: 1, type: 'MENU', path: '/' },
    { id: 4, parentId: 1, sortOrder: 3, type: 'BUTTON', path: '/roles.html' },
    { id: 5, parentId: null, sortOrder: 2, type: 'DIRECTORY', name: '空目录' },
    { id: 6, parentId: 5, sortOrder: 1, type: 'MENU', path: '/unknown.html' }
  ]
  const result = menu.navigation(rows)
  assert.equal(result.length, 1)
  assert.deepEqual(result[0].children.map(node => node.route), ['/messages', '/chat'])
})

test('network errors retain a readable message instead of throwing on missing response', async () => {
  let rejectResponse
  const messages = []
  const client = {
    interceptors: {
      request: { use: () => {} },
      response: { use: (_success, failure) => { rejectResponse = failure } }
    }
  }
  loadModule(source('api/request.js'), {
    axios: { create: () => client },
    'element-plus': { ElMessage: { error: value => messages.push(value) } },
    '../store': { state: { token: 'test-session' } }
  })
  const error = Object.assign(new Error('Network Error'), { config: { sessionToken: 'test-session' } })
  await assert.rejects(rejectResponse(error), value => value === error)
  assert.match(error.userMessage, /网络连接失败/)
  assert.deepEqual(messages, [error.userMessage])
})
