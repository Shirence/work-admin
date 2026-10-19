import request from './request'

/** 设备 */
export function getChatDevices() {
  return request({ url: '/chat/devices', method: 'get' })
}

export function registerChatDevice(data) {
  return request({ url: '/chat/devices/register', method: 'post', data, timeout: 20000 })
}

export function getPendingDeviceLinks(deviceKey) {
  return request({ url: '/chat/devices/pending', method: 'get', params: { deviceKey } })
}

export function approveChatDevice(data) {
  return request({ url: '/chat/devices/approve', method: 'post', data })
}

export function rejectChatDevice(data) {
  return request({ url: '/chat/devices/reject', method: 'post', data })
}

export function revokeChatDevice(data) {
  return request({ url: '/chat/devices/revoke', method: 'post', data })
}

export function getDeviceBundles(username) {
  return request({ url: `/chat/users/${encodeURIComponent(username)}/device-bundles`, method: 'get' })
}

/** 建群权限 */
export function getGroupPermission() {
  return request({ url: '/chat/group-permission', method: 'get' })
}

/** 会话 */
export function listChatConversations(params) {
  return request({ url: '/chat/conversations', method: 'get', params })
}

export function createPrivateConversation(data) {
  return request({ url: '/chat/conversations/private', method: 'post', data })
}

export function createGroupConversation(data) {
  return request({ url: '/chat/conversations/group', method: 'post', data })
}

export function getChatConversation(id, params) {
  return request({ url: `/chat/conversations/${id}`, method: 'get', params })
}

export function markConversationRead(id, data) {
  return request({ url: `/chat/conversations/${id}/read`, method: 'post', data })
}

/** 消息 */
export function syncChatMessages(params) {
  return request({ url: '/chat/messages', method: 'get', params, timeout: 15000 })
}

export function sendChatMessage(data) {
  return request({ url: '/chat/messages', method: 'post', data, timeout: 20000 })
}
