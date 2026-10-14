import axios from 'axios'

// 第 10 课：接口前缀来自环境变量（.env.development / .env.production）
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 5000
})

// 请求拦截器：自动带上 token
service.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.token = token
  return config
})

// 响应拦截器：剥掉一层 res.data，并按业务码判断成败
service.interceptors.response.use(
  response => {
    const result = response.data
    if (result && result.code !== 200) {
      return Promise.reject(new Error(result.msg || '请求失败'))
    }
    return result
  },
  error => Promise.reject(error)
)

export default service
