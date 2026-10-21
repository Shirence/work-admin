import request from './request'

export function getRemoteUsers(params) {
  return request({ url: '/remote-admin/users', method: 'get', params })
}

export function getRemoteRoles() {
  return request({ url: '/remote-admin/roles', method: 'get' })
}

export function createRemoteUser(data) {
  return request({ url: '/remote-admin/users', method: 'post', data })
}

export function updateRemoteUser(id, data) {
  return request({ url: `/remote-admin/users/${id}`, method: 'put', data })
}

export function bindRemoteRole(id, roleId) {
  return request({ url: `/remote-admin/users/${id}/role`, method: 'put', data: { roleId } })
}

export function deleteRemoteUser(id) {
  return request({ url: `/remote-admin/users/${id}`, method: 'delete' })
}
