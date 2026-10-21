import axios from 'axios'

// 接口前缀来自环境变量（.env.development / .env.production）
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
      // 401：登录态失效，清掉本地状态并回登录页。
      // 登录接口自身的 401（密码错误）不跳转，交给页面提示。
      const url = (response.config && response.config.url) || ''
      if (result.code === 401 && !url.includes('/login')) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
      return Promise.reject(new Error(result.msg || '请求失败'))
    }
    return result
  },
  error => Promise.reject(error)
)

export default service
