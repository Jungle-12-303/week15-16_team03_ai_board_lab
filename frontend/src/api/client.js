import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 20000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function unwrap(promise) {
  return promise.then((response) => response.data)
}

export const authApi = {
  signup: (payload) => unwrap(api.post('/auth/signup', payload)),
  login: (payload) => unwrap(api.post('/auth/login', payload))
}

export const postApi = {
  list: (params) => unwrap(api.get('/posts', { params })),
  get: (id) => unwrap(api.get(`/posts/${id}`)),
  create: (payload) => unwrap(api.post('/posts', payload)),
  update: (id, payload) => unwrap(api.put(`/posts/${id}`, payload)),
  remove: (id) => api.delete(`/posts/${id}`),
  comment: (postId, payload) => unwrap(api.post(`/posts/${postId}/comments`, payload)),
  removeComment: (commentId) => api.delete(`/comments/${commentId}`)
}

export const aiApi = {
  similar: (payload) => unwrap(api.post('/ai/rag/similar', payload)),
  chat: (payload) => unwrap(api.post('/ai/rag/chat', payload)),
  mcp: (payload) => unwrap(api.post('/ai/mcp/call', payload)),
  agent: (payload) => unwrap(api.post('/ai/agent/write-helper', payload))
}

export function getErrorMessage(error) {
  if (error?.response?.data?.message) {
    return error.response.data.message
  }
  if (error?.response?.status) {
    return `API 요청이 실패했습니다. 상태 코드: ${error.response.status}`
  }
  return '백엔드 API에 연결할 수 없습니다. 서버 실행 상태를 확인해 주세요.'
}
