import { createStore } from 'vuex'

const emptyAccess = () => ({ role: null, permissions: [], menus: [] })
let refreshing = null

export default createStore({
  state: {
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo')) || null,
    access: emptyAccess()
  },
  getters: {
    can: state => permission =>
      state.access.permissions.includes('*') || state.access.permissions.includes(permission)
  },
  mutations: {
    setToken(state, val) {
      state.token = val
      localStorage.setItem('token', val)
    },
    setUserInfo(state, val) {
      state.userInfo = val
      localStorage.setItem('userInfo', JSON.stringify(val))
    },
    setAccess(state, access) {
      state.access = access || emptyAccess()
    },
    clearUserInfo(state) {
      state.token = ''
      state.userInfo = null
      state.access = emptyAccess()
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  },
  actions: {
    async refreshAccess({ state, commit }) {
      if (!state.token) throw new Error('未登录')
      if (refreshing) return refreshing
      const token = state.token
      refreshing = (async () => {
        const [{ getAccess }, { getMe }] = await Promise.all([
          import('../api/access'),
          import('../api/auth')
        ])
        const [accessRes, meRes] = await Promise.all([getAccess(), getMe()])
        if (token !== state.token) throw new Error('登录账号已变更')
        commit('setAccess', accessRes.data || emptyAccess())
        if (meRes.data) commit('setUserInfo', meRes.data)
        return accessRes.data
      })().finally(() => { refreshing = null })
      return refreshing
    }
  }
})
