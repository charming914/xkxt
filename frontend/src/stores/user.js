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
    roleCode: (state) => state.user?.roleCode || '',
    username: (state) => state.user?.username || '',
    realName: (state) => state.user?.realName || '',
    refId: (state) => state.user?.refId || ''
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
