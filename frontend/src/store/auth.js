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

        if (this.isWife) {
          // 老婆（做饭端）：引导开启通知后再进菜品管理
          uni.showModal({
            title: '开启通知',
            content: '对方下单后需要微信通知你\n点确定后请在弹窗中选"允许"',
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
          // 老公（点餐端）：订阅「已收到订单」+「订单完成（饭好了）」两个通知
          uni.showModal({
            title: '开启通知',
            content: '老婆收到订单、做完饭时微信通知你\n点确定后请在弹窗中选"允许"',
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.requestSubscribeMessage({
                  tmplIds: ['YOUR_WECHAT_TEMPLATE_ID', 'YOUR_WECHAT_COOK_DONE_TEMPLATE_ID'],
                  complete: () => {
                    uni.setStorageSync('msg_subscribed', '1')
                    uni.reLaunch({ url: '/pages/husband/menu' })
                  }
                })
              } else {
                uni.reLaunch({ url: '/pages/husband/menu' })
              }
            }
          })
        }
      } catch (e) {
        uni.showToast({ title: '登录失败: ' + e.message, icon: 'none' })
      }
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
