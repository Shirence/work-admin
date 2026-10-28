const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const { transformSync } = require('@babel/core')
const { parse } = require('vue/compiler-sfc')

function load(file, overrides = {}) {
  const contents = readFileSync(path.join(__dirname, '..', 'src', file), 'utf8')
  const source = file.endsWith('.vue') ? parse(contents).descriptor.script.content : contents
  const { code } = transformSync(source, {
    configFile: false, babelrc: false, plugins: ['@babel/plugin-transform-modules-commonjs']
  })
  const module = { exports: {} }
  new Function('require', 'module', 'exports', code)(id => {
    if (Object.hasOwn(overrides, id)) return overrides[id]
    if (id.endsWith('.vue')) return {}
    return require(id)
  }, module, module.exports)
  return module.exports
}
const model = load('components/roles/roleModel.js')
const menu = load('utils/menu.js')
const role = { id: 10, name: '用户只读', code: 'student_reader', description: '', enabled: true, builtin: false, userCount: 0 }
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

function management(overrides = {}, permissions = ['*']) {
  const calls = []
  const api = Object.fromEntries(['getRoles', 'createRole', 'updateRole', 'deleteRole', 'getRolePermissions', 'saveRolePermissions', 'getPermissionTree'].map(name => [name, async (...args) => {
    calls.push([name, ...args])
    return overrides[name] ? overrides[name](...args) : (name === 'getRoles' ? [role] : [])
  }]))
  const component = load('components/roles/RoleManagement.vue', {
    '../../api/access': api, '../../utils/menu': menu, './roleModel': model
  }).default
  const vm = {
    ...component.data(),
    $store: {
      state: { token: 'same-session' },
      getters: { can: permission => permissions.includes('*') || permissions.includes(permission) },
      dispatch: async action => { calls.push(['dispatch', action]) }
    },
    $router: { replace: async route => { calls.push(['replace', route]) } },
    $message: Object.fromEntries(['warning', 'success'].map(type => [type, message => calls.push([type, message])])),
    $confirm: async () => { calls.push(['confirm']) }
  }
  for (const [name, method] of Object.entries(component.methods)) vm[name] = method.bind(vm)
  for (const [name, getter] of Object.entries(component.computed)) Object.defineProperty(vm, name, { get: () => getter.call(vm) })
  return { vm, calls }
}

test('role payload strips untrusted fields, trims input and preserves immutable builtin fields', () => {
  const form = { ...role, name: '  只读  ', description: '  说明  ', code: 'changed', enabled: false, userCount: 99 }
  assert.deepEqual(model.rolePayload(form, { ...role, builtin: true }), {
    name: '只读', code: 'student_reader', description: '说明', enabled: true
  })
  assert.throws(() => model.rolePayload({ ...role, code: '1invalid' }), /编码/)
  assert.throws(() => model.rolePayload({ ...role, name: ' ' }), /角色名/)
  assert.throws(() => model.rolePayload({ ...role, description: 'x'.repeat(256) }), /说明/)
})

test('role list loads the complete array and filters name or code locally', async () => {
  const { vm, calls } = management({ getRoles: () => [role, { ...role, id: 11, name: '运营', code: 'OPERATOR', enabled: false, builtin: true }] })
  await vm.load()
  vm.keyword = ' operator '
  assert.equal(vm.filtered.length, 1)
  assert.equal(vm.filtered[0].id, 11)
  assert.equal(vm.enabledCount, 1)
  assert.equal(vm.builtinCount, 1)
  assert.deepEqual(calls, [['getRoles']])
})

test('list failure preserves existing rows and permits retry', async () => {
  const { vm } = management({ getRoles: () => { throw { userMessage: '服务不可用' } } })
  vm.rows = [role]
  assert.equal(await vm.load(), false)
  assert.deepEqual(vm.rows, [role])
  assert.equal(vm.error, '服务不可用')
  assert.equal(vm.loading, false)
})

test('create sends only the four role fields, reloads the list and refreshes access', async () => {
  const { vm, calls } = management()
  vm.open()
  await vm.save({ ...role, name: ' 新角色 ', code: 'classroom_new' })
  assert.deepEqual(calls[0], ['createRole', { name: '新角色', code: 'classroom_new', description: '', enabled: true }])
  assert.equal(vm.formVisible, false)
  assert.ok(calls.some(call => call[0] === 'getRoles'))
  assert.ok(calls.some(call => call[0] === 'dispatch' && call[1] === 'refreshAccess'))
})

test('editing copies the row and sends an update using its actual ID', async () => {
  const { vm, calls } = management()
  vm.open(role)
  assert.notEqual(vm.editingRole, role)
  await vm.save({ ...role, name: '改名' })
  assert.equal(calls[0][0], 'updateRole')
  assert.equal(calls[0][1], role.id)
  assert.equal(calls[0][2].name, '改名')
  assert.equal(role.name, '用户只读')
})

test('invalid role code prevents all mutation requests', async () => {
  const { vm, calls } = management()
  await vm.save({ ...role, code: '0invalid' })
  assert.equal(calls[0][0], 'warning')
  assert.equal(calls.length, 1)
})

test('save failure preserves the dialog, form target and existing list', async () => {
  const { vm } = management({ updateRole: () => { throw new Error('conflict') } })
  vm.rows = [role]
  vm.open(role)
  await vm.save({ ...role, name: '改名' })
  assert.equal(vm.formVisible, true)
  assert.deepEqual(vm.rows, [role])
  assert.equal(vm.saving, false)
})

test('pending saves cannot submit a second create request', async () => {
  const pending = deferred()
  const { vm, calls } = management({ createRole: () => pending.promise })
  const save = vm.save(role)
  await vm.save(role)
  assert.equal(calls.filter(call => call[0] === 'createRole').length, 1)
  pending.resolve(role)
  await save
  assert.equal(vm.busy, false)
})

test('no mutation method can bypass action-level permissions', async () => {
  const { vm, calls } = management({}, ['role:read'])
  vm.open()
  vm.open(role)
  await vm.save(role)
  await vm.remove(role)
  await vm.openGrant(role)
  vm.grantRole = role
  await vm.saveGrant([1])
  assert.equal(vm.formVisible, false)
  assert.deepEqual(calls, [])
})

test('built-in and user-bound roles cannot reach deletion API', async () => {
  const { vm, calls } = management()
  await vm.remove({ ...role, builtin: true })
  await vm.remove({ ...role, userCount: 3 })
  assert.deepEqual(calls.map(call => call[0]), ['warning', 'warning'])
})

test('cancelled deletion never sends a delete request', async () => {
  const { vm, calls } = management()
  vm.$confirm = async () => { throw 'cancel' }
  await vm.remove(role)
  assert.deepEqual(calls, [])
  assert.equal(vm.busy, false)
})

test('deletion refreshes the list only after confirmation and success', async () => {
  const { vm, calls } = management()
  await vm.remove(role)
  assert.deepEqual(calls.map(call => call[0]), ['confirm', 'deleteRole', 'success', 'getRoles'])
  assert.equal(vm.deletingId, null)
})

test('a delete conflict leaves the record and releases the operation lock', async () => {
  const { vm, calls } = management({ deleteRole: () => { throw new Error('409: bound user') } })
  vm.rows = [role]
  await vm.remove(role)
  assert.deepEqual(vm.rows, [role])
  assert.equal(vm.busy, false)
  assert.equal(calls.some(call => call[0] === 'getRoles'), false)
})

test('permission data and existing IDs must both load before opening the grant dialog', async () => {
  const nodes = deferred(), ids = deferred()
  const { vm, calls } = management({ getPermissionTree: () => nodes.promise, getRolePermissions: () => ids.promise })
  const opening = vm.openGrant(role)
  assert.deepEqual(calls.map(call => call[0]), ['getPermissionTree', 'getRolePermissions'])
  nodes.resolve([{ id: 1, parentId: null, name: '页面', sortOrder: 1 }])
  await Promise.resolve()
  assert.equal(vm.grantVisible, false)
  ids.resolve([1])
  await opening
  assert.equal(vm.grantVisible, true)
  assert.deepEqual(vm.checkedKeys, [1])
  assert.equal(vm.tree[0].id, 1)
})

test('failed grant loading does not open or reuse old permissions', async () => {
  const { vm } = management({ getRolePermissions: () => { throw new Error('403') } })
  await vm.openGrant(role)
  assert.equal(vm.grantVisible, false)
  assert.equal(vm.busy, false)
})

test('super administrator permission editing cannot reach grant APIs', async () => {
  const { vm, calls } = management()
  const superRole = { ...role, code: 'super_admin' }
  await vm.openGrant(superRole)
  vm.grantRole = superRole
  await vm.saveGrant([1])
  assert.deepEqual(calls, [])
})

test('grant sends only checked node IDs, including an intentional empty selection', async () => {
  const { vm, calls } = management()
  vm.grantRole = role
  vm.grantVisible = true
  await vm.saveGrant([9])
  await vm.saveGrant([])
  assert.deepEqual(calls.filter(call => call[0] === 'saveRolePermissions'), [
    ['saveRolePermissions', role.id, [9]], ['saveRolePermissions', role.id, []]
  ])
  assert.equal(vm.grantVisible, false)
})

test('revoking the current role read permission redirects after a successful grant', async () => {
  const permissions = ['role:read', 'role:grant']
  const { vm, calls } = management({}, permissions)
  vm.grantRole = role
  vm.$store.dispatch = async () => { permissions.splice(permissions.indexOf('role:read'), 1) }
  await vm.saveGrant([])
  assert.ok(calls.some(call => call[0] === 'replace' && call[1] === '/forbidden'))
})

test('grant failure preserves the dialog for retry', async () => {
  const { vm } = management({ saveRolePermissions: () => { throw new Error('offline') } })
  vm.grantRole = role
  vm.grantVisible = true
  await vm.saveGrant([1])
  assert.equal(vm.grantVisible, true)
  assert.equal(vm.saving, false)
})

test('form initialization and validation preserve source role data', async () => {
  const component = load('components/roles/RoleFormDialog.vue').default
  const events = []
  const vm = {
    ...component.data(), role, busy: false,
    $nextTick: async () => {}, $refs: { formRef: { clearValidate() {}, validate: async () => false } },
    $emit: (...args) => events.push(args)
  }
  await component.methods.initialize.call(vm)
  vm.form.name = 'edited'
  await component.methods.submit.call(vm)
  assert.equal(events.length, 0)
  assert.equal(role.name, '用户只读')
  vm.$refs.formRef.validate = async () => true
  await component.methods.submit.call(vm)
  assert.equal(events[0][0], 'save')
  assert.equal(events[0][1].name, 'edited')
})

test('permission dialog restores exact independent checks and emits exact selection', async () => {
  const component = load('components/roles/RolePermissionDialog.vue', { './roleModel': model }).default
  let checked = []
  const events = []
  const vm = {
    ...component.data(), busy: false, checkedKeys: [2],
    tree: [{ id: 1, children: [{ id: 2, children: [] }, { id: 3, children: [] }] }],
    $nextTick: async () => {},
    $refs: { treeRef: { setCheckedKeys: keys => { checked = [...keys] }, getCheckedKeys: () => checked } },
    $emit: (...args) => events.push(args)
  }
  for (const [name, method] of Object.entries(component.methods)) vm[name] = method.bind(vm)
  await vm.initialize()
  assert.deepEqual(checked, [2])
  vm.submit()
  assert.deepEqual(events[0], ['save', [2]])
  vm.selectAll()
  assert.deepEqual(checked, [1, 2, 3])
  vm.selectAll(true)
  vm.submit()
  assert.deepEqual(events[1], ['save', []])
})
