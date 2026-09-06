<!-- Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0 -->
<template>
  <view class="login-page" :style="pageBg">
    <badge-demo />
    <view class="hero">
      <view class="food-icons">
        <text class="food-icon">🥘</text><text class="food-icon">🍜</text><text class="food-icon">🥬</text><text class="food-icon">🥩</text><text class="food-icon">🍅</text>
      </view>
      <text class="app-name">{{ app.name }}</text>
      <text class="app-desc">{{ app.desc }}</text>
    </view>

    <view class="login-panel">
      <button class="wx-login-btn" :disabled="loading" @click="handleLogin">
        <text class="wx-icon">💬</text>
        <text v-if="loading">登录中...</text>
        <text v-else>微信一键登录</text>
      </button>

      <!-- 固定角色卡：老公点菜端 / 老婆做饭端 -->
      <view class="brand-cards">
        <view class="brand-card">
          <view class="brand-avatar">👨</view>
          <text class="brand-label">老公要吃饭</text>
          <text class="brand-sublabel">浏览菜单 · 下单</text>
        </view>
        <view class="brand-card">
          <view class="brand-avatar">👩‍🍳</view>
          <text class="brand-label">老婆厨神到</text>
          <text class="brand-sublabel">管菜品 · 接订单</text>
        </view>
      </view>
    </view>

    <text class="footer-text">燕公主的食堂 · © 2026 深圳市甜梦屋科技有限公司</text>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/store/auth'
import { APP, DEFAULT_BACKGROUND } from '@/config'
import badgeDemo from '@/components/badge-demo.vue'

const app = APP
const loading = ref(false)
const authStore = useAuthStore()

const pageBg = computed(() => ({ background: DEFAULT_BACKGROUND }))

const handleLogin = async () => {
  loading.value = true
  await authStore.loginAction()
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
.wx-login-btn { width:100%; height:96rpx; line-height:96rpx; border-radius:48rpx; font-size:32rpx; font-weight:600; border:none; color:#fff; background:linear-gradient(135deg,#5CB85C,#6DC06D); box-shadow:0 6rpx 24rpx rgba(92,184,92,.25); text-align:center; margin-bottom:36rpx; display:flex; align-items:center; justify-content:center; }
.wx-icon { font-size:36rpx; margin-right:12rpx; }
.wx-login-btn:active{transform:scale(.97)}.wx-login-btn[disabled]{opacity:.6}
.brand-cards { display:flex; gap:16rpx; }
.brand-card { flex:1; display:flex; flex-direction:column; align-items:center; padding:28rpx 12rpx 20rpx; border-radius:18rpx; background:#FAF2EA; border:1rpx solid #F0E2D2; }
.brand-avatar { width:72rpx; height:72rpx; line-height:72rpx; text-align:center; font-size:40rpx; border-radius:50%; margin-bottom:10rpx; background:#E8F0FA; }
.brand-label { font-size:26rpx; font-weight:700; color:#4E3D35; margin-bottom:4rpx; }
.brand-sublabel { font-size:20rpx; color:#A89080; text-align:center; }
.footer-text { margin-top:40rpx; font-size:22rpx; color:rgba(255,255,255,.6); letter-spacing:2rpx; z-index:1; }
</style>
