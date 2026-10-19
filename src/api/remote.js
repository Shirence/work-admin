import request from './request'

/** 远程 MySQL 只读观察台（仅 admin） */
export function getRemoteOverview() {
  return request({ url: '/remote/overview', method: 'get' })
}

export function getRemoteGroups(params) {
  return request({ url: '/remote/groups', method: 'get', params })
}

export function getRemoteGroupDetail(id) {
  return request({ url: `/remote/groups/${id}`, method: 'get' })
}

export function getRemoteGroupMessages(id, params) {
  return request({ url: `/remote/groups/${id}/messages`, method: 'get', params })
}

export function getRemoteMessages(params) {
  return request({ url: '/remote/messages', method: 'get', params })
}

export function getRemoteUsers(params) {
  return request({ url: '/remote/users', method: 'get', params })
}
