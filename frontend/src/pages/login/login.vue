<template>
  <view class="login-page" :style="pageBg">
    <badge-demo />
    <view class="hero">
      <view class="food-icons">
        <text class="food-icon">🥘</text><text class="food-icon">🍜</text><text class="food-icon">🥬</text><text class="food-icon">🥩</text><text class="food-icon">🍳</text>
      </view>
      <text class="app-name">{{ app.name }}</text>
      <text class="app-desc">{{ tagline }}</text>
    </view>

    <view class="login-panel">
      <view class="cook-picker">
        <text class="cook-title">🍳 今天谁做饭？</text>
        <view class="cook-cards">
          <view class="cook-card" :class="{ active: cookChoice === 'husband' }" @click="cookChoice='husband'">
            <view class="cook-avatar">👨</view><text class="cook-name">老公做饭</text>
            <text class="cook-sub">{{ cookChoice==='husband' ? '✓ 我接单' : '老婆点菜' }}</text>
          </view>
          <view class="cook-card" :class="{ active: cookChoice === 'wife' }" @click="cookChoice='wife'">
            <view class="cook-avatar">👩‍🍳</view><text class="cook-name">老婆做饭</text>
            <text class="cook-sub">{{ cookChoice==='wife' ? '✓ 我接单' : '老公点菜' }}</text>
          </view>
        </view>
      </view>

      <button class="wx-login-btn" :disabled="loading" @click="handleLogin">
        <text class="wx-icon">💬</text>
        <text v-if="loading">登录中...</text>
        <text v-else>微信一键登录</text>
      </button>

      <!-- 动态角色卡：不固定谁想吃饭/谁厨神，按今天做饭人动态 -->
      <view class="brand-cards">
        <view class="brand-card" :class="{ active: cookChoice==='wife' }">
          <view class="brand-avatar">👨</view>
          <text class="brand-label">{{ cookChoice==='wife' ? '老公 · 点菜端' : '今天不做饭' }}</text>
          <text class="brand-sublabel">{{ cookChoice==='wife' ? '看老婆的拿手菜 · 下单' : '等你选谁做饭' }}</text>
        </view>
        <view class="brand-card" :class="{ active: cookChoice==='husband' }">
          <view class="brand-avatar">👩‍🍳</view>
          <text class="brand-label">{{ cookChoice==='husband' ? '老婆 · 点菜端' : '今天做饭' }}</text>
          <text class="brand-sublabel">{{ cookChoice==='husband' ? '看老公的拿手菜 · 下单' : '做饭人管菜接管订单' }}</text>
        </view>
      </view>
    </view>

    <text class="footer-text">扫码自动识别 · 夫妻档专属</text>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/store/auth'
import { getCurrentCook } from '@/api/cook'
import { APP, DEFAULT_BACKGROUND } from '@/config'
import badgeDemo from '@/components/badge-demo.vue'

const app = APP
const loading = ref(false)
const cookChoice = ref('wife')
const authStore = useAuthStore()

const pageBg = computed(() => ({ background: DEFAULT_BACKGROUND }))
const tagline = computed(() => {
  const cook = cookChoice.value === 'husband' ? '老公' : '老婆'
  return `今天 ${cook} 做饭 · ${authStore.myLabel || (cookChoice.value==='husband'?'老婆':'老公')} 点菜`
})

onMounted(async () => {
  try {
    const res = await getCurrentCook()
    if (res && res.cookWho) cookChoice.value = res.cookWho
  } catch (e) { /* 用默认值 */ }
})

const handleLogin = async () => {
  loading.value = true
  await authStore.loginAction(cookChoice.value)
  loading.value = false
}
</script>

<style scoped>
.login-page { min-height:100vh; display:flex; flex-direction:column; align-items:center; justify-content:center; padding:80rpx 40rpx; position:relative; overflow:hidden; }
.hero { text-align:center; margin-bottom:48rpx; z-index:1; }
.food-icons { display:flex; justify-content:center; gap:16rpx; margin-bottom:24rpx; }
.food-icon { font-size:48rpx; animation:float 2s ease-in-out infinite; }
.food-icon:nth-child(2){animation-delay:.4s}.food-icon:nth-child(3){animation-delay:.8s}.food-icon:nth-child(4){animation-delay:1.2s}.food-icon:nth-child(5){animation-delay:1.6s}
@keyframes float{0%,100%{transform:translateY(0)}50%{transform:translateY(-12rpx)}}
.app-name { display:block; font-size:52rpx; font-weight:800; color:#fff; text-shadow:0 4rpx 16rpx rgba(0,0,0,.1); letter-spacing:6rpx; margin-bottom:10rpx; }
.app-desc { font-size:26rpx; color:rgba(255,255,255,.85); letter-spacing:2rpx; }
.login-panel { width:100%; background:#FDF5EF; border-radius:28rpx; padding:40rpx 36rpx 36rpx; box-shadow:0 12rpx 40rpx rgba(0,0,0,.1); z-index:1; }
.cook-picker { margin-bottom:32rpx; }
.cook-title { display:block; text-align:center; font-size:30rpx; font-weight:700; color:#4E3D35; margin-bottom:20rpx; }
.cook-cards { display:flex; gap:16rpx; }
.cook-card { flex:1; display:flex; flex-direction:column; align-items:center; padding:24rpx 12rpx 20rpx; border-radius:18rpx; background:#FAF2EA; border:3rpx solid transparent; transition:all .2s; }
.cook-card.active { border-color:#E8805A; background:#FCEDE6; box-shadow:0 4rpx 16rpx rgba(232,128,90,.15); }
.cook-avatar { font-size:48rpx; margin-bottom:8rpx; }
.cook-name { font-size:28rpx; font-weight:700; color:#4E3D35; margin-bottom:4rpx; }
.cook-sub { font-size:20rpx; color:#A89080; }
.cook-card.active .cook-sub { color:#E8805A; font-weight:600; }
.wx-login-btn { width:100%; height:96rpx; line-height:96rpx; border-radius:48rpx; font-size:32rpx; font-weight:600; border:none; color:#fff; background:linear-gradient(135deg,#5CB85C,#6DC06D); box-shadow:0 6rpx 24rpx rgba(92,184,92,.25); text-align:center; margin-bottom:36rpx; display:flex; align-items:center; justify-content:center; }
.wx-icon { font-size:36rpx; margin-right:12rpx; }
.wx-login-btn:active{transform:scale(.97)}.wx-login-btn[disabled]{opacity:.6}
.brand-cards { display:flex; gap:16rpx; }
.brand-card { flex:1; display:flex; flex-direction:column; align-items:center; padding:28rpx 12rpx 20rpx; border-radius:18rpx; background:#FAF2EA; border:1rpx solid #F0E2D2; }
.brand-card.active { background:#FCEDE6; border-color:#E8805A; }
.brand-avatar { width:72rpx; height:72rpx; line-height:72rpx; text-align:center; font-size:40rpx; border-radius:50%; margin-bottom:10rpx; background:#E8F0FA; }
.brand-label { font-size:26rpx; font-weight:700; color:#4E3D35; margin-bottom:4rpx; }
.brand-sublabel { font-size:20rpx; color:#A89080; text-align:center; }
.footer-text { margin-top:40rpx; font-size:22rpx; color:rgba(255,255,255,.6); letter-spacing:2rpx; z-index:1; }
</style>
