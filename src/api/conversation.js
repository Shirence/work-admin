import request from './request'
export const getConversations = () => request.get('/api/conversations')
export const getConversation = id => request.get(`/api/conversations/${id}`)
export const createConversation = title => request.post('/api/conversations', { title })
export const saveConversation = (id, body) => request.put(`/api/conversations/${id}`, body, { timeout: 20000 })
export const deleteConversation = id => request.delete(`/api/conversations/${id}`)
export const exportConversation = id => request.get(`/api/conversations/${id}/export`, { responseType: 'text' })
