const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const { transformSync } = require('@babel/core')
const { parse } = require('vue/compiler-sfc')
const source = file => fs.readFileSync(path.join(__dirname, '../src', file), 'utf8')
function load(text, overrides = {}, globals = {}, module = { exports: {} }) {
  const code = transformSync(text, { configFile: false, babelrc: false, plugins: ['@babel/plugin-transform-modules-commonjs', '@babel/plugin-transform-dynamic-import'] }).code
  new Function('require', 'module', 'exports', ...Object.keys(globals), code)(id => Object.hasOwn(overrides, id) ? overrides[id] : require(id), module, module.exports, ...Object.values(globals))
  return module.exports
}
const menu = load(source('utils/menu.js'))

test('menu index preserves sibling order and does not modify source rows', () => {
  const rows = [{ id: 1, parentId: null, sortOrder: 2 }, { id: 4, parentId: 1, sortOrder: 1 }, { id: 3, parentId: 1, sortOrder: 1 }, { id: 2, parentId: null, sortOrder: 1 }]
  const before = JSON.stringify(rows)
  assert.deepEqual(menu.toTree(rows).map(node => node.id), [2, 1])
  assert.deepEqual(menu.toTree(rows)[1].children.map(node => node.id), [3, 4])
  assert.equal(JSON.stringify(rows), before)
})

test('malformed menu cycles terminate without entering an infinite tree', () => {
  const rows = [{ id: 1, parentId: null, sortOrder: 1 }, { id: 2, parentId: 1, sortOrder: 1 }, { id: 1, parentId: 2, sortOrder: 1 }]
  const tree = menu.toTree(rows)
  assert.equal(tree[0].children[0].id, 2)
  assert.equal(tree[0].children[0].children.length, 0)
})

test('cross-tab synchronization handles logout, new login, cache restoration and cleanup', () => {
  const handlers = new Map()
  let saved = JSON.stringify({ token: 'current' }), reloads = 0
  const localStorage = { getItem: key => key === 'work-admin.session.v2' ? saved : null }
  const window = {
    addEventListener: (name, handler) => handlers.set(name, handler),
    removeEventListener: (name, handler) => { if (handlers.get(name) === handler) handlers.delete(name) },
    location: { reload: () => reloads++ }
  }
  const { installSessionSync } = load(source('store/index.js'), {}, { window, localStorage })
  const cleanup = installSessionSync({ state: { token: 'current' } })
  handlers.get('storage')({ storageArea: localStorage, key: 'work-admin.session.v2' })
  assert.equal(reloads, 0)
  saved = 'null'
  handlers.get('storage')({ storageArea: localStorage, key: 'unrelated' })
  assert.equal(reloads, 0)
  handlers.get('storage')({ storageArea: localStorage, key: 'work-admin.session.v2' })
  assert.equal(reloads, 1)
  saved = JSON.stringify({ token: 'new-account' })
  handlers.get('storage')({ storageArea: localStorage, key: null })
  assert.equal(reloads, 2)
  handlers.get('pageshow')({ persisted: false })
  assert.equal(reloads, 2)
  handlers.get('pageshow')({ persisted: true })
  assert.equal(reloads, 3)
  cleanup()
  assert.equal(handlers.size, 0)
})

test('application bootstrap installs session synchronization and registers only selected icons', () => {
  let installed = 0, cleaned = 0, hotDispose
  const registered = []
  const app = { component: name => registered.push(name), use() { return this }, mount() { return this } }
  const store = {}
  const module = { exports: {}, hot: { dispose: fn => { hotDispose = fn } } }
  load(source('main.js'), {
    vue: { createApp: () => app }, './App.vue': {}, './router': {},
    './store': { __esModule: true, default: store, installSessionSync: value => { assert.equal(value, store); installed++; return () => cleaned++ } },
    'element-plus': {}, 'element-plus/dist/index.css': {}, './styles/theme.css': {},
    './plugins/icons': { icons: { User: {}, House: {} } }, './api/request': { setUnauthorizedHandler() {} }
  }, {}, module)
  assert.equal(installed, 1)
  assert.deepEqual(registered, ['User', 'House'])
  hotDispose()
  assert.equal(cleaned, 1)
})

test('selected icon registry covers static templates and dashboard dynamic icons', () => {
  const { icons } = load(source('plugins/icons.js'))
  function files(dir) { return fs.readdirSync(dir, { withFileTypes: true }).flatMap(entry => entry.isDirectory() ? files(path.join(dir, entry.name)) : [path.join(dir, entry.name)]) }
  for (const file of files(path.join(__dirname, '../src')).filter(file => file.endsWith('.vue'))) {
    const content = fs.readFileSync(file, 'utf8')
    const names = [...content.matchAll(/<el-icon[^>]*>\s*<([A-Z]\w*)/g), ...content.matchAll(/icon:\s*'([A-Z]\w*)'/g)].map(match => match[1])
    for (const name of names) assert.ok(icons[name], `${name} missing for ${file}`)
  }
  assert.ok(Object.keys(icons).length < 40)
})

test('dashboard uses the real statistics API and invalidates pending loads on unmount', () => {
  assert.match(source('components/dashboard/DashboardWorkspace.vue'), /await getDashboard\(days.value\)/)
  assert.match(source('components/dashboard/DashboardWorkspace.vue'), /onBeforeUnmount\(\(\) => \+\+serial\)/)
  assert.doesNotMatch(source('views/Dashboard.vue'), /教学演示|totalUsers: 1/)
})

test('user table respects server search results instead of filtering them twice', () => {
  const component = load(parse(source('views/User.vue')).descriptor.script.content, {
    '../api/user': {}, '../api/access': {}, '../components/users/UserRoleDialog.vue': {}
  }).default
  const items = [{ id: 1, account: 'server-matched', name: '服务器匹配结果' }]
  assert.equal(component.computed.tableData.call({ items, appliedKeyword: '服务器扩展搜索条件' }), items)
})

test('skip-to-content focuses the main region without navigating the hash router', () => {
  const component = load(parse(source('views/Layout.vue')).descriptor.script.content, {
    '../api/auth': {}, '../utils/menu': menu, '../components/layout/AppNavigation.vue': {}
  }).default
  let focused = false
  component.methods.focusContent.call({ $refs: { contentRef: { $el: { focus: () => { focused = true } } } } })
  assert.equal(focused, true)
  assert.match(source('views/Layout.vue'), /@click\.prevent="focusContent"/)
})
