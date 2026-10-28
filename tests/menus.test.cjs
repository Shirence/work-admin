const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const { transformSync } = require('@babel/core')
const { parse } = require('vue/compiler-sfc')

function load(file, overrides = {}) {
  const contents = readFileSync(path.join(__dirname, '..', 'src', file), 'utf8')
  const source = file.endsWith('.vue') ? parse(contents).descriptor.script.content : contents
  const { code } = transformSync(source, { configFile: false, babelrc: false, plugins: ['@babel/plugin-transform-modules-commonjs'] })
  const module = { exports: {} }
  new Function('require', 'module', 'exports', code)(id => {
    if (Object.hasOwn(overrides, id)) return overrides[id]
    if (id.endsWith('.vue')) return {}
    return require(id)
  }, module, module.exports)
  return module.exports
}
const model = load('components/menus/menuModel.js')
const menu = load('utils/menu.js')
const rows = [
  { id: 1, parentId: null, name: '系统', type: 'DIRECTORY', path: null, permission: null, sortOrder: 0, enabled: true, builtin: true },
  { id: 2, parentId: 1, name: '目录', type: 'DIRECTORY', sortOrder: 1, enabled: true },
  { id: 3, parentId: 2, name: '页面', type: 'MENU', path: '/example.html', permission: 'example:read', sortOrder: 0, enabled: true },
  { id: 4, parentId: 3, name: '按钮', type: 'BUTTON', permission: 'example:create', sortOrder: 0, enabled: true }
]
const draft = { name: '新节点', type: 'MENU', parentId: 1, path: '/new.html', permission: 'new:read', sortOrder: 0, enabled: true }

function page(overrides = {}, permissions = ['*']) {
  const calls = []
  const api = Object.fromEntries(['getMenus', 'createMenu', 'updateMenu', 'deleteMenu'].map(name => [name, async (...args) => {
    calls.push([name, ...args])
    return overrides[name] ? overrides[name](...args) : (name === 'getMenus' ? rows : undefined)
  }]))
  const view = load('components/menus/MenuManagement.vue', { '../../api/access': api, '../../utils/menu': menu, './menuModel': model }).default
  const vm = {
    ...view.data(), rows: [...rows],
    $store: {
      state: { token: 'session' }, getters: { can: p => permissions.includes('*') || permissions.includes(p) },
      dispatch: async name => { calls.push(['dispatch', name]); if (overrides.dispatch) await overrides.dispatch() },
      commit: (...args) => calls.push(['commit', ...args])
    },
    $router: { replace: async route => { calls.push(['replace', route]) } },
    $message: Object.fromEntries(['warning', 'success'].map(name => [name, message => calls.push([name, message])])),
    $confirm: async () => { calls.push(['confirm']) }
  }
  for (const [name, method] of Object.entries(view.methods)) vm[name] = method.bind(vm)
  for (const [name, getter] of Object.entries(view.computed)) Object.defineProperty(vm, name, { get: () => getter.call(vm) })
  return { vm, calls }
}

test('legal directory/page/button payloads use null for unused fields and strip server fields', () => {
  assert.deepEqual(model.menuPayload({ ...draft, id: 999, builtin: true }, null, rows), draft)
  const directory = model.menuPayload({ ...draft, type: 'DIRECTORY', parentId: null }, null, rows)
  assert.equal(directory.permission, null)
  assert.equal(directory.path, null)
  const button = model.menuPayload({ ...draft, type: 'BUTTON', parentId: 3, permission: 'new:create' }, null, rows)
  assert.equal(button.path, null)
  assert.equal(button.parentId, 3)
})

test('builtin nodes accept only name and sorting changes', () => {
  const builtin = { ...rows[2], builtin: true }
  assert.deepEqual(model.menuPayload({ ...draft, name: ' 改名 ', sortOrder: 12, type: 'BUTTON', enabled: false }, builtin, rows), {
    name: '改名', type: 'MENU', parentId: 2, path: '/example.html', permission: 'example:read', enabled: true, sortOrder: 12
  })
})

test('parent candidates exclude self and all descendants, including circular input', () => {
  assert.deepEqual(model.parentCandidates({ type: 'DIRECTORY' }, rows, 1), [])
  assert.deepEqual(model.parentCandidates({ type: 'MENU' }, rows, 2).map(row => row.id), [1])
  assert.deepEqual(model.parentCandidates({ type: 'BUTTON' }, rows, null).map(row => row.id), [3])
  assert.doesNotThrow(() => model.parentCandidates({ type: 'MENU' }, [{ id: 1, parentId: 2 }, { id: 2, parentId: 1 }], 1))
})

test('invalid hierarchy, missing parent and incompatible existing children are rejected', () => {
  assert.throws(() => model.menuPayload({ ...draft, type: 'BUTTON', parentId: null }, null, rows), /按钮/)
  assert.throws(() => model.menuPayload({ ...draft, type: 'BUTTON', parentId: 1 }, null, rows), /上级/)
  assert.throws(() => model.menuPayload({ ...draft, parentId: 3 }, null, rows), /上级/)
  assert.throws(() => model.menuPayload({ ...rows[1], parentId: 2 }, rows[1], rows), /上级/)
  assert.throws(() => model.menuPayload({ ...draft, parentId: 1 }, rows[1], rows), /子节点/)
})

test('page path, unique permission, node name and sorting are validated', () => {
  for (const path of ['', 'https://example.com', '//example.com', '/a?token=x', '/a\\b']) {
    assert.throws(() => model.menuPayload({ ...draft, path }, null, rows), /路径/)
  }
  assert.equal(model.menuPayload({ ...draft, path: '/', parentId: null }, null, rows).path, '/')
  assert.throws(() => model.menuPayload({ ...draft, permission: 'example:read' }, null, rows), /已存在/)
  assert.throws(() => model.menuPayload({ ...draft, permission: ' ' }, null, rows), /权限标识/)
  assert.throws(() => model.menuPayload({ ...draft, name: ' ' }, null, rows), /名称/)
  for (const sortOrder of [-1, 10000, 0.1, null]) assert.throws(() => model.menuPayload({ ...draft, sortOrder }, null, rows), /排序/)
})

test('disabled ancestors affect child status without changing the child node', () => {
  const disabled = rows.map(row => ({ ...row, enabled: row.id !== 1 }))
  assert.equal(model.hasDisabledAncestor(disabled[3], disabled), true)
  assert.equal(disabled[3].enabled, true)
  assert.equal(model.hasDisabledAncestor(rows[3], rows), false)
})

test('loading builds the full sorted menu tree and counts all node types', async () => {
  const { vm } = page()
  await vm.load()
  assert.equal(vm.tree[0].children[0].children[0].children[0].id, 4)
  assert.deepEqual(vm.counts, { DIRECTORY: 2, MENU: 1, BUTTON: 1 })
})

test('failed menu loading preserves existing tree and releases its lock', async () => {
  const { vm } = page({ getMenus: () => { throw { userMessage: '服务不可用' } } })
  assert.equal(await vm.load(), false)
  assert.equal(vm.rows.length, 4)
  assert.equal(vm.error, '服务不可用')
  assert.equal(vm.loading, false)
})

test('creating a node saves the normalized payload then refreshes access and list', async () => {
  const { vm, calls } = page()
  vm.open()
  await vm.save({ ...draft, name: ' 新节点 ' })
  assert.deepEqual(calls[0], ['createMenu', draft])
  assert.ok(calls.some(call => call[0] === 'dispatch' && call[1] === 'refreshAccess'))
  assert.ok(calls.some(call => call[0] === 'getMenus'))
  assert.equal(vm.visible, false)
})

test('editing uses the actual server ID and excludes child data from payload', async () => {
  const { vm, calls } = page()
  vm.open(rows[2])
  await vm.save({ ...rows[2], name: '改名', children: [{ id: 999 }] })
  assert.equal(calls[0][0], 'updateMenu')
  assert.equal(calls[0][1], 3)
  assert.equal(calls[0][2].name, '改名')
  assert.equal(Object.hasOwn(calls[0][2], 'children'), false)
})

test('invalid node data cannot reach mutation APIs', async () => {
  const { vm, calls } = page()
  await vm.save({ ...draft, parentId: 3 })
  assert.deepEqual(calls.map(call => call[0]), ['warning'])
})

test('mutation failure keeps the editor and current data for retry', async () => {
  const { vm } = page({ createMenu: () => { throw new Error('409') } })
  vm.open()
  await vm.save(draft)
  assert.equal(vm.visible, true)
  assert.equal(vm.busy, false)
  assert.equal(vm.rows.length, 4)
})

test('pending operations cannot submit another node or open another editor', async () => {
  let finish
  const { vm, calls } = page({ createMenu: () => new Promise(resolve => { finish = resolve }) })
  const pending = vm.save(draft)
  await vm.save(draft)
  vm.open(rows[2])
  assert.equal(vm.editingNode, null)
  assert.equal(calls.filter(call => call[0] === 'createMenu').length, 1)
  finish()
  await pending
})

test('read-only operators cannot bypass action permissions through methods', async () => {
  const { vm, calls } = page({}, ['menu:read'])
  vm.open(); vm.open(rows[2])
  await vm.save(draft)
  await vm.remove(rows[3])
  assert.deepEqual(calls, [])
})

test('built-in nodes and nodes with children never reach delete API', async () => {
  const { vm, calls } = page()
  await vm.remove(rows[0])
  await vm.remove(rows[2])
  assert.deepEqual(calls.map(call => call[0]), ['warning', 'warning'])
})

test('cancelled deletion has no side effects; confirmed deletion refreshes navigation', async () => {
  const { vm, calls } = page()
  vm.$confirm = async () => { throw 'cancel' }
  await vm.remove(rows[3])
  assert.deepEqual(calls, [])
  assert.equal(vm.deleting, false)
  vm.$confirm = async () => {}
  await vm.remove(rows[3])
  assert.equal(calls[0][0], 'deleteMenu')
  assert.equal(calls[0][1], 4)
  assert.ok(calls.some(call => call[0] === 'dispatch'))
})

test('deletion conflicts preserve the tree and release the lock', async () => {
  const { vm } = page({ deleteMenu: () => { throw new Error('409') } })
  await vm.remove(rows[3])
  assert.equal(vm.rows.length, 4)
  assert.equal(vm.deleting, false)
})

test('losing menu read after a branch change leaves menu management without another list call', async () => {
  const permissions = ['menu:read', 'menu:update']
  const { vm, calls } = page({ dispatch: () => permissions.splice(0) }, permissions)
  vm.open(rows[1])
  await vm.save({ ...rows[1], enabled: false })
  assert.ok(calls.some(call => call[0] === 'replace' && call[1] === '/forbidden'))
  assert.equal(calls.some(call => call[0] === 'getMenus'), false)
})

test('failed permission refresh after save clears stale access and shows a saved-state warning', async () => {
  const { vm, calls } = page({ dispatch: () => { throw new Error('offline') } })
  await vm.save(draft)
  assert.ok(calls.some(call => call[0] === 'commit' && call[1] === 'setAccess'))
  assert.ok(calls.some(call => call[0] === 'warning' && /已保存/.test(call[1])))
  assert.equal(vm.visible, false)
})

test('add-child initializes the correct parent and child type and editing does not mutate source data', async () => {
  const view = load('components/menus/MenuFormDialog.vue', { './menuModel': model }).default
  const vm = { ...view.data(), node: null, parent: rows[2], $nextTick: async () => {}, $refs: {} }
  await view.methods.initialize.call(vm)
  assert.equal(vm.form.type, 'BUTTON')
  assert.equal(vm.form.parentId, 3)
  vm.node = rows[2]
  await view.methods.initialize.call(vm)
  vm.form.name = '改名'
  assert.equal(rows[2].name, '页面')
  await view.methods.typeChanged.call(vm)
  assert.equal(vm.form.parentId, null)
  assert.equal(vm.form.path, null)
  assert.equal(vm.form.permission, null)
})
