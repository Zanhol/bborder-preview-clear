// Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0
import { defineStore } from 'pinia'
import { loginByCode, getMe, updateBackground } from '@/api/auth'

// 固定角色称呼映射
const ROLE_LABEL = { husband: '老公', wife: '老婆', husband_test: '老公', wife_test: '老婆' }

// 测试角色归一到正式侧的称呼
function baseRole(role) {
  if (role === 'husband_test') return 'husband'
  if (role === 'wife_test') return 'wife'
  return role
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    user: null,
    isLoggedIn: false,
    // 用户 DIY 背景（跨端同步）
    backgroundUrl: ''
  }),

  getters: {
    // 固定角色：老公=点餐端，老婆=做饭端（管菜品·接订单）
    isHusband: (state) => !!state.user && baseRole(state.user.role) === 'husband',
    isWife: (state) => !!state.user && baseRole(state.user.role) === 'wife',
    // 当前用户称呼（测试角色归一显示）
    myLabel: (state) => (state.user ? ROLE_LABEL[state.user.role] || ROLE_LABEL[baseRole(state.user.role)] : '')
  },

  actions: {
    /**
     * 微信登录。按固定角色跳转：老公→点餐端菜单，老婆→做饭端（先引导开启通知）。
     */
    async loginAction() {
      try {
        const codeRes = await new Promise((resolve, reject) => {
          wx.login({ success: (r) => resolve(r.code), fail: reject })
        })
        const res = await loginByCode(codeRes)
        this.token = res.token
        this.user = res.user
        this.isLoggedIn = true
        uni.setStorageSync('token', res.token)
        uni.setStorageSync('user', JSON.stringify(res.user))

        // 拉取用户 DIY 背景
        this.refreshMe()

        // 进端分流（订阅授权交给各端首页 onMounted 每次进端请求——微信一次性订阅）
        if (this.isWife) {
          uni.reLaunch({ url: '/pages/wife/dishes' })
        } else {
          uni.reLaunch({ url: '/pages/husband/menu' })
        }
      } catch (e) {
        uni.showToast({ title: '登录失败: ' + e.message, icon: 'none' })
      }
    },

    /**
     * 每次进入点餐/做饭端请求订阅（微信订阅为一次性：允许一次=可收一条通知，多授权多收）
     */
    requestSubscribe() {
      // #ifdef MP-WEIXIN
      if (this.isWife) {
        wx.requestSubscribeMessage({
          tmplIds: ['YOUR_WECHAT_TEMPLATE_ID'],
          complete: () => { uni.setStorageSync('msg_subscribed', '1') }
        })
      } else {
        wx.requestSubscribeMessage({
          tmplIds: ['YOUR_WECHAT_TEMPLATE_ID', 'YOUR_WECHAT_COOK_DONE_TEMPLATE_ID'],
          complete: () => { uni.setStorageSync('msg_subscribed', '1') }
        })
      }
      // #endif
    },

    /** 从本地恢复登录态，并异步刷新背景 */
    checkLogin() {
      const token = uni.getStorageSync('token')
      const userStr = uni.getStorageSync('user')
      if (token && userStr) {
        try {
          this.token = token
          this.user = JSON.parse(userStr)
          this.isLoggedIn = true
          this.refreshMe()
        } catch (e) { this.logout() }
      }
    },

    /** 从后端拉取当前用户信息（含 DIY 背景），失败静默 */
    async refreshMe() {
      try {
        const me = await getMe()
        if (me && me.backgroundUrl) { this.backgroundUrl = me.backgroundUrl }
      } catch (e) { /* 后台未就绪时静默 */ }
    },

    /** 保存用户 DIY 背景 */
    async setBackground(bg) {
      try {
        await updateBackground(bg)
        this.backgroundUrl = bg
        return true
      } catch (e) {
        uni.showToast({ title: '保存背景失败', icon: 'none' })
        return false
      }
    },

    logout() {
      this.token = ''
      this.user = null
      this.isLoggedIn = false
      uni.removeStorageSync('token')
      uni.removeStorageSync('user')
      uni.reLaunch({ url: '/pages/login/login' })
    }
  }
})
