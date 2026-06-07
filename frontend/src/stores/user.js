import { defineStore } from 'pinia'

function getSavedUser() {
  return JSON.parse(localStorage.getItem('user') || 'null')
}

export const useUserStore = defineStore('user', {
  state: () => ({
    user: getSavedUser()
  }),
  getters: {
    isLoggedIn: (state) => !!state.user,
    userId: (state) => state.user?.userId || '',
    roleCode: (state) => state.user?.roleCode || '',
    username: (state) => state.user?.username || '',
    realName: (state) => state.user?.realName || '',
    refId: (state) => state.user?.refId || '',
    loginAt: (state) => state.user?.loginAt || '',
    isDefaultPassword: (state) => Boolean(state.user?.defaultPassword)
  },
  actions: {
    setUser(user) {
      this.user = user
      localStorage.setItem('user', JSON.stringify(user))
    },
    clearUser() {
      this.user = null
      localStorage.removeItem('user')
    }
  }
})
