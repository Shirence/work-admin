import request from './request'

// 登录：{ username, password } -> { token, userInfo }
export function login(data) {
  return request({
    url: '/login',
    method: 'post',
    data
  })
}

// 注册：{ username, password, confirmPassword, email }
export function register(data) {
  return request({
    url: '/register',
    method: 'post',
    data
  })
}

// 退出登录
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 当前登录用户（可选）
export function getMe() {
  return request({
    url: '/me',
    method: 'get'
  })
}
