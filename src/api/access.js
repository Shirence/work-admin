import request from './request'

export function getAccess() {
  return request({ url: '/auth/access', method: 'get' })
}

export function getRoles() {
  return request({ url: '/roles', method: 'get' })
}

export function getRoleOptions() {
  return request({ url: '/roles/options', method: 'get' })
}

export function createRole(data) {
  return request({ url: '/roles', method: 'post', data })
}

export function updateRole(id, data) {
  return request({ url: `/roles/${id}`, method: 'put', data })
}

export function deleteRole(id) {
  return request({ url: `/roles/${id}`, method: 'delete' })
}

export function getRolePermissions(id) {
  return request({ url: `/roles/${id}/permissions`, method: 'get' })
}

export function saveRolePermissions(id, menuIds) {
  return request({ url: `/roles/${id}/permissions`, method: 'put', data: { menuIds } })
}

export function getPermissionTree() {
  return request({ url: '/menus/permission-tree', method: 'get' })
}

export function getMenus() {
  return request({ url: '/menus', method: 'get' })
}

export function createMenu(data) {
  return request({ url: '/menus', method: 'post', data })
}

export function updateMenu(id, data) {
  return request({ url: `/menus/${id}`, method: 'put', data })
}

export function deleteMenu(id) {
  return request({ url: `/menus/${id}`, method: 'delete' })
}

export function getAccounts() {
  return request({ url: '/users', method: 'get' })
}

export function createAccount(data) {
  return request({ url: '/users', method: 'post', data })
}

export function updateAccount(id, data) {
  return request({ url: `/users/${id}`, method: 'put', data })
}

export function deleteAccount(id) {
  return request({ url: `/users/${id}`, method: 'delete' })
}

export function bindUserRole(id, roleId) {
  return request({ url: `/users/${id}/role`, method: 'put', data: { roleId } })
}
