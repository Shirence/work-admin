import axios from 'axios'
import request from './request'

/**
 * 远程 stu 账号使用独立的 token：remoteToken。
 * 不复用本地 H2 登录态，避免两种身份混在一起。
 */
const remoteService = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 10000
})

remoteService.interceptors.request.use(config => {
  const token = localStorage.getItem('remoteToken')
  if (token) config.headers.token = token
  return config
})

remoteService.interceptors.response.use(
  response => {
    const result = response.data
    if (result && result.code !== 200) {
      if (result.code === 401) {
        localStorage.removeItem('remoteToken')
        localStorage.removeItem('remoteUserInfo')
        if (window.location.pathname !== '/remote-login') {
          window.location.href = '/remote-login'
        }
      }
      return Promise.reject(new Error(result.msg || '请求失败'))
    }
    return result
  },
  error => Promise.reject(error)
)

// 登录使用普通 request：远程登录接口本身不需要 token
export function remoteLogin(data) {
  return request({ url: '/remote-auth/login', method: 'post', data })
}

// 注册同样不需要 token；成功后会直接返回远程登录态
export function remoteRegister(data) {
  return request({ url: '/remote-auth/register', method: 'post', data, timeout: 15000 })
}

export function remoteMe() {
  return remoteService({ url: '/remote-auth/me', method: 'get' })
}

export function remoteLogout() {
  return remoteService({ url: '/remote-auth/logout', method: 'post' })
}

export function listRemoteGroups(params) {
  return remoteService({ url: '/remote-chat/groups', method: 'get', params })
}

export function createRemoteGroup(data) {
  return remoteService({ url: '/remote-chat/groups', method: 'post', data })
}

export function joinRemoteGroup(id) {
  return remoteService({ url: `/remote-chat/groups/${id}/join`, method: 'post' })
}

export function getRemoteChatMessages(id, params) {
  return remoteService({ url: `/remote-chat/groups/${id}/messages`, method: 'get', params })
}

export function sendRemoteChatMessage(id, data) {
  return remoteService({ url: `/remote-chat/groups/${id}/messages`, method: 'post', data })
}
