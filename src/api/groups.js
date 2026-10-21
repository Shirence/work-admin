import request from './request'

/** 群列表：普通用户只返回自己加入的群，super_admin 返回全站群组 */
export function listGroups() {
  return request({ url: '/groups', method: 'get' })
}

/** 建群时可选的用户（不要求 user:read 权限） */
export function listCandidates() {
  return request({ url: '/groups/candidates', method: 'get' })
}

/** 群详情：群信息 + 成员 */
export function getGroup(id) {
  return request({ url: `/groups/${id}`, method: 'get' })
}

/** 历史消息，按 id 升序；beforeId 用于向上翻页 */
export function listMessages(id, params) {
  return request({ url: `/groups/${id}/messages`, method: 'get', params, timeout: 15000 })
}

/** 发送消息 */
export function sendMessage(id, data) {
  return request({ url: `/groups/${id}/messages`, method: 'post', data, timeout: 15000 })
}

/** 建群 */
export function createGroup(data) {
  return request({ url: '/groups', method: 'post', data })
}

/** 加入群聊 */
export function joinGroup(id) {
  return request({ url: `/groups/${id}/join`, method: 'post' })
}
