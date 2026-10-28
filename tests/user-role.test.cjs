const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const { transformSync } = require('@babel/core')
const { parse } = require('vue/compiler-sfc')

function component(file, overrides = {}) {
  const { descriptor } = parse(readFileSync(path.join(__dirname, '..', 'src', file), 'utf8'))
  const { code } = transformSync(descriptor.script.content, {
    configFile: false, babelrc: false, plugins: ['@babel/plugin-transform-modules-commonjs']
  })
  const module = { exports: {} }
  new Function('require', 'module', 'exports', code)(id => {
    if (Object.hasOwn(overrides, id)) return overrides[id]
    if (id.endsWith('.vue')) return {}
    return require(id)
  }, module, module.exports)
  return module.exports.default
}

const target = { id: 9, name: '测试用户', account: 'test_member', roleId: 2, roleName: '普通用户' }
const roles = [
  { id: 2, name: '普通用户', enabled: true },
  { id: 3, name: '只读角色', enabled: true },
  { id: 4, name: '停用角色', enabled: false }
]

function page(overrides = {}, permissions = ['*']) {
  const calls = []
  const wrap = (name, fallback) => async (...args) => {
    calls.push([name, ...args])
    return overrides[name] ? overrides[name](...args) : fallback
  }
  const api = {
    getRoleOptions: wrap('getRoleOptions', roles),
    bindUserRole: wrap('bindUserRole', undefined)
  }
  const users = {
    getUser: wrap('getUser', { ...target }),
    getUserList: wrap('getUserList', { items: [target], page: 2, total: 30, totalPages: 2 }),
    addUser: wrap('addUser', target), editUser: wrap('editUser', target), deleteUser: wrap('deleteUser')
  }
  const view = component('views/User.vue', { '../api/access': api, '../api/user': users })
  const vm = {
    ...view.data(),
    $store: {
      state: { token: 'session', userInfo: { id: 1 }, access: { permissions } },
      getters: { can: p => permissions.includes('*') || permissions.includes(p) },
      dispatch: wrap('dispatch', undefined),
      commit: (...args) => calls.push(['commit', ...args])
    },
    $router: { replace: wrap('replace', undefined) },
    $message: Object.fromEntries(['warning', 'success'].map(name => [name, text => calls.push([name, text])])),
    $nextTick: async () => {},
    $refs: { userFormRef: { clearValidate() {}, validate: async () => true } }
  }
  for (const [name, method] of Object.entries(view.methods)) vm[name] = method.bind(vm)
  for (const [name, getter] of Object.entries(view.computed)) Object.defineProperty(vm, name, { get: () => getter.call(vm) })
  return { vm, calls }
}

test('role dialog uses fresh user detail, includes only enabled roles and restores current role', async () => {
  const { vm, calls } = page({ getUser: () => ({ ...target, roleId: 3, roleName: '只读角色' }) })
  await vm.openRole({ ...target, roleId: 2 })
  assert.deepEqual(calls.slice(0, 2), [['getRoleOptions'], ['getUser', 9]])
  assert.equal(vm.roleId, 3)
  assert.deepEqual(vm.roleOptions.map(role => role.id), [2, 3])
  assert.equal(vm.roleVisible, true)
  assert.equal(vm.busy, false)
})

test('disabled current role is not silently replaced by the first option', async () => {
  const { vm } = page({ getUser: () => ({ ...target, roleId: 4 }) })
  await vm.openRole(target)
  assert.equal(vm.roleId, null)
  assert.equal(vm.roleVisible, true)
})

test('empty role options keep save blocked', async () => {
  const { vm, calls } = page({ getRoleOptions: () => [] })
  await vm.openRole(target)
  await vm.saveRole()
  assert.equal(vm.roleId, null)
  assert.equal(calls.some(call => call[0] === 'bindUserRole'), false)
})

test('role option or user-detail failure never opens an incomplete dialog', async () => {
  for (const failing of ['getRoleOptions', 'getUser']) {
    const { vm } = page({ [failing]: () => { throw new Error('offline') } })
    await vm.openRole(target)
    assert.equal(vm.roleVisible, false)
    assert.equal(vm.roleLoading, false)
  }
})

test('missing assign permission prevents reading role options and binding users', async () => {
  const { vm, calls } = page({}, ['user:read'])
  await vm.openRole(target)
  vm.roleUser = target; vm.roleId = 3; vm.roleOptions = roles
  await vm.saveRole()
  assert.deepEqual(calls, [])
})

test('saving another user binds actual IDs and refreshes the current list page', async () => {
  const { vm, calls } = page()
  vm.page = 2
  vm.appliedKeyword = 'test'
  await vm.openRole(target)
  vm.roleId = 3
  await vm.saveRole()
  assert.ok(calls.some(call => call[0] === 'bindUserRole' && call[1] === 9 && call[2] === 3))
  assert.ok(calls.some(call => call[0] === 'getUserList' && call[1] === 2 && call[2] === 'test'))
  assert.equal(calls.some(call => call[0] === 'dispatch'), false)
  assert.equal(vm.roleVisible, false)
})

test('invalid, disabled or missing selections cannot reach binding API', async () => {
  const { vm, calls } = page()
  await vm.openRole(target)
  for (const id of [null, '', 99, 4]) { vm.roleId = id; await vm.saveRole() }
  assert.equal(calls.some(call => call[0] === 'bindUserRole'), false)
  assert.equal(calls.filter(call => call[0] === 'warning').length, 4)
})

test('binding failure preserves the dialog and selection without a success message', async () => {
  const { vm, calls } = page({ bindUserRole: () => { throw new Error('409') } })
  await vm.openRole(target)
  vm.roleId = 3
  await vm.saveRole()
  assert.equal(vm.roleVisible, true)
  assert.equal(vm.roleId, 3)
  assert.equal(vm.roleSaving, false)
  assert.equal(calls.some(call => call[0] === 'success'), false)
})

test('pending bind requests block duplicate submits and all other user operations', async () => {
  let finish
  const { vm, calls } = page({ bindUserRole: () => new Promise(resolve => { finish = resolve }) })
  await vm.openRole(target)
  vm.roleId = 3
  const pending = vm.saveRole()
  assert.equal(vm.busy, true)
  await vm.saveRole()
  await vm.openAddDialog()
  assert.equal(vm.dialogVisible, false)
  assert.equal(calls.filter(call => call[0] === 'bindUserRole').length, 1)
  finish()
  await pending
  assert.equal(vm.busy, false)
})

test('self-role change refreshes permissions and leaves user management if read is revoked', async () => {
  const permissions = ['user:read', 'user:assign-role']
  const { vm, calls } = page({ dispatch: () => { permissions.splice(0) } }, permissions)
  vm.$store.state.userInfo.id = 9
  await vm.openRole(target)
  vm.roleId = 3
  await vm.saveRole()
  const bind = calls.findIndex(call => call[0] === 'bindUserRole')
  const clear = calls.findIndex(call => call[0] === 'commit' && call[1] === 'setAccess')
  const refresh = calls.findIndex(call => call[0] === 'dispatch' && call[1] === 'refreshAccess')
  assert.ok(bind < clear && clear < refresh)
  assert.deepEqual(calls[clear][2], { role: null, permissions: [], menus: [] })
  assert.ok(calls.some(call => call[0] === 'replace' && call[1] === '/forbidden'))
  assert.equal(calls.some(call => call[0] === 'getUserList'), false)
})

test('self-role change retains user list access when the new role still has user:read', async () => {
  const { vm, calls } = page()
  vm.$store.state.userInfo.id = 9
  await vm.openRole(target)
  vm.roleId = 3
  await vm.saveRole()
  assert.ok(calls.some(call => call[0] === 'dispatch'))
  assert.ok(calls.some(call => call[0] === 'getUserList'))
  assert.equal(calls.some(call => call[0] === 'replace'), false)
})

test('failed self-role permission refresh cannot leave stale privileges active', async () => {
  const { vm, calls } = page({ dispatch: () => { throw new Error('offline') } })
  vm.$store.state.userInfo.id = 9
  await vm.openRole(target)
  vm.roleId = 3
  await vm.saveRole()
  assert.equal(vm.roleVisible, false)
  assert.ok(calls.some(call => call[0] === 'commit' && call[1] === 'setAccess'))
  assert.ok(calls.some(call => call[0] === 'replace' && call[1] === '/forbidden'))
  assert.ok(calls.some(call => call[0] === 'warning' && /角色已保存/.test(call[1])))
  assert.equal(vm.roleSaving, false)
})

test('a response from an old session cannot refresh or navigate a new account', async () => {
  const { vm, calls } = page({ bindUserRole: () => { vm.$store.state.token = 'new-session' } })
  await vm.openRole(target)
  vm.roleId = 3
  await vm.saveRole()
  assert.equal(calls.some(call => ['success', 'getUserList', 'dispatch', 'replace'].includes(call[0])), false)
})

test('read-only users cannot invoke existing create, edit or delete methods directly', async () => {
  const { vm, calls } = page({}, ['user:read'])
  await vm.openAddDialog()
  await vm.openEditDialog(target)
  await vm.submitForm()
  await vm.handleDelete(target)
  assert.deepEqual(calls, [])
})

test('editing a user preserves the existing role by omitting roleId from the payload', async () => {
  const { vm, calls } = page()
  vm.form = { id: target.id, name: target.name, account: target.account, passwd: '', roleId: 3 }
  await vm.submitForm()
  const update = calls.find(call => call[0] === 'editUser')
  assert.deepEqual(update, ['editUser', target.id, { name: target.name, account: target.account }])
})

test('role dialog cannot close, select or submit while saving', () => {
  const view = component('components/users/UserRoleDialog.vue')
  const events = []
  const vm = { saving: true, selectedRole: roles[0], $emit: (...args) => events.push(args) }
  view.methods.close.call(vm)
  view.methods.select.call(vm, 3)
  view.methods.save.call(vm)
  assert.deepEqual(events, [])
  vm.saving = false
  view.methods.select.call(vm, 3)
  view.methods.save.call(vm)
  assert.deepEqual(events, [['update:roleId', 3], ['save']])
})
