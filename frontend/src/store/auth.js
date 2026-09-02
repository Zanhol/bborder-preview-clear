import { defineStore } from 'pinia'
import { loginByCode, getMe, updateBackground } from '@/api/auth'
import { switchCook } from '@/api/cook'
import { APP } from '@/config'

// 角色称呼映射（白标配置：读 config.js 的 APP.cookLabel）
const ROLE_LABEL = APP.cookLabel || { husband: '老公', wife: '老婆', husband_test: '老公', wife_test: '老婆' }

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
    // 今日做饭人（husband/wife），登录时在登录页选择并写入
    cookWho: uni.getStorageSync('cookWho') || '',
    // 用户 DIY 背景（跨端同步）
    backgroundUrl: ''
  }),

  getters: {
    // 当前登录者是否为今日做饭人（做饭人=订单端/接单端，对方=点餐端）
    isCook: (state) => !!state.user && !!state.cookWho && baseRole(state.user.role) === state.cookWho,
    // 当前用户称呼（测试角色归一显示）
    myLabel: (state) => (state.user ? ROLE_LABEL[state.user.role] || ROLE_LABEL[baseRole(state.user.role)] : ''),
    cookLabel: (state) => ROLE_LABEL[state.cookWho] || '做饭人'
  },

  actions: {
    /**
     * 微信登录。cookChoice：登录页选出的今日做饭人（husband/wife）。
     * 登录成功自动分配角色，并按动态角色跳转：做饭人→订单端，点餐人→点餐端。
     */
    async loginAction(cookChoice) {
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

        // 登录即选做饭人
        if (cookChoice) {
          try { await switchCook(cookChoice) } catch (e) { /* 不阻塞 */ }
          this.cookWho = cookChoice
          uni.setStorageSync('cookWho', cookChoice)
        }

        // 拉取用户 DIY 背景
        this.refreshMe()

        if (this.isCook) {
          // 做饭人：引导开启通知后再进订单端
          uni.showModal({
            title: '开启通知',
            content: `${this.cookLabel}，对方下单后需要微信通知你\n点确定后请在弹窗中选"允许"`,
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.requestSubscribeMessage({
                  tmplIds: ['YOUR_WECHAT_TEMPLATE_ID'],
                  complete: () => {
                    uni.setStorageSync('msg_subscribed', '1')
                    uni.reLaunch({ url: '/pages/wife/dishes' })
                  }
                })
              } else {
                uni.reLaunch({ url: '/pages/wife/dishes' })
              }
            }
          })
        } else {
          uni.reLaunch({ url: '/pages/husband/menu' })
        }
      } catch (e) {
        uni.showToast({ title: '登录失败: ' + e.message, icon: 'none' })
      }
    },

    /** 从本地恢复登录态（含今日做饭人），并异步刷新背景 */
    checkLogin() {
      const token = uni.getStorageSync('token')
      const userStr = uni.getStorageSync('user')
      if (token && userStr) {
        try {
          this.token = token
          this.user = JSON.parse(userStr)
          this.isLoggedIn = true
          this.cookWho = uni.getStorageSync('cookWho') || this.cookWho
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
