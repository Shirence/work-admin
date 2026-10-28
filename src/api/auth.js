import request from './request'

export const loginApi = data => request.post('/api/auth/login', data, {
    skipAuth: true,
    silentError: true
})
export const registerApi = data => request.post('/api/auth/register', data, {
    skipAuth: true,
    silentError: true
})
export const getCurrentUser = () => request.get('/api/auth/me')
export const logoutApi = () => request.post('/api/auth/logout')
