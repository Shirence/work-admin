import request from './request'
export const listWork = (resource, params) => request.get(`/api/${resource}`, { params })
export const saveWork = (resource, id, body) => id ? request.put(`/api/${resource}/${id}`, body) : request.post(`/api/${resource}`, body)
export const removeWork = (resource, id) => request.delete(`/api/${resource}/${id}`)
export const getPeople = () => request.get('/api/work/people')
export const getDashboard = days => request.get('/api/dashboard', { params: { days }, silentError: true })
export const markRead = id => request.put(`/api/notifications/${id}/read`)
export const markAllRead = () => request.put('/api/notifications/read-all')
