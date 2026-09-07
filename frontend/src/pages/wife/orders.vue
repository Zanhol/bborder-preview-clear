<!-- Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0 -->
<template>
  <view class="orders-page">
    <view class="nav-bar">
      <view class="nav-tabs">
        <view class="nav-tab" @click="goDishes">菜品</view>
        <view class="nav-tab active">订单</view>
      </view>
      <view class="nav-logout" @click="handleLogout">退出</view>
    </view>

    <!-- #ifdef MP-WEIXIN -->
    <view class="sub-banner" v-if="!subscribed" @click="requestSubscribe">
      <text class="sub-icon">🔔</text>
      <text class="sub-text">开启通知，对方下单立刻提醒</text>
      <text class="sub-arrow">›</text>
    </view>
    <!-- #endif -->

    <view class="page-header">
      <view class="header-badge" v-if="pendingCount">{{ pendingCount }}条待处理</view>
    </view>

    <view class="order-card" v-for="order in orders" :key="order.id" :class="{ pending: order.status === 'pending' }">
      <view class="order-ribbon" v-if="order.status === 'pending'">NEW</view>

      <view class="order-meta">
        <view class="order-id">#{{ order.orderNumber || order.id }}</view>
        <text class="order-time">{{ formatTime(order.createdAt) }}</text>
      </view>

      <view class="order-dishes">
        <view class="dish-chip" v-for="item in order.items" :key="item.dishId">
          <image class="chip-img" :src="dishImgById(item.dishId, item.dishId)" mode="aspectFill" />
          <text class="chip-name">{{ item.dishName }}</text>
          <text class="chip-spice" v-if="item.spiciness && item.spiciness !== 'none'">
            {{ item.spiciness === 'female_baby' ? '🌶️🌶️' : '🌶️' }}
          </text>
        </view>
      </view>

      <view class="order-footer">
        <view class="order-status" :class="order.status">
          {{ order.status === 'pending' ? '⏳ 待买菜' : '✅ 已收到' }}
        </view>
        <button
          class="done-btn"
          v-if="order.status === 'pending'"
          @click="handleReceive(order.id)"
        >
          标记已收到
        </button>
          <button class="del-btn" @click="handleDelete(order)">删除</button>
        </view>
      </view>

    <view class="empty-state" v-if="orders.length === 0">
      <text class="empty-icon">📭</text>
      <text class="empty-title">暂无订单</text>
      <text class="empty-desc">对方还没点菜</text>
    </view>

    <view class="legal">
      <text class="icp-text">粤ICP备2026129595号-1</text>
      <text class="copyright-text">开发主体及版权归属：深圳市甜梦屋科技有限公司</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { getOrders, markReceived, deleteOrder } from '@/api/order'
import { useAuthStore } from '@/store/auth'
import { dishImgById } from '@/utils/image'

const authStore = useAuthStore()
const orders = ref([])
const subscribed = ref(uni.getStorageSync('msg_subscribed') === '1')
const pendingCount = computed(() => Array.isArray(orders.value) ? orders.value.filter(o => o.status === 'pending').length : 0)

const requestSubscribe = () => {
  // #ifdef MP-WEIXIN
  wx.requestSubscribeMessage({
    tmplIds: ['YOUR_WECHAT_TEMPLATE_ID'],
    success: (res) => {
      if (res['YOUR_WECHAT_TEMPLATE_ID'] === 'accept') {
        subscribed.value = true
        uni.setStorageSync('msg_subscribed', '1')
        uni.showToast({ title: '已开启通知', icon: 'success' })
      }
    },
    fail: () => {
      uni.showToast({ title: '授权失败，稍后重试', icon: 'none' })
    }
  })
  // #endif
}

const goDishes = () => {
  uni.reLaunch({ url: '/pages/wife/dishes' })
}

const handleLogout = () => {
  authStore.logout()
}

const loadOrders = async () => {
  try {
    const res = await getOrders()
    orders.value = Array.isArray(res) ? res : []
    if (!Array.isArray(res)) console.warn('orders API returned non-array:', typeof res, res)
  } catch (e) { console.warn('orders load failed:', e.message) }
}

const handleDelete = (order) => {
  uni.showModal({
    title: '删除订单',
    content: `确定删除订单 #${order.orderNumber || order.id} 吗？`,
    confirmColor: '#D4756B',
    success: async (res) => {
      if (res.confirm) {
        await deleteOrder(order.id)
        uni.showToast({ title: '已删除', icon: 'success' })
        await loadOrders()
      }
    }
  })
}

const handleReceive = async (orderId) => {
  const order = orders.value.find(o => o.id === orderId)
  if (order) order.status = 'received'
  try {
    await markReceived(orderId)
    uni.showToast({ title: '已标记', icon: 'success' })
  } catch (e) {
    if (order) order.status = 'pending'
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}月${d.getDate()}日 ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(loadOrders)

onPullDownRefresh(async () => {
  await loadOrders()
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.orders-page {
  padding: 20rpx 30rpx 60rpx;
  min-height: 100vh;
  background: #FAF7F2;
}

.sub-banner {
  display: flex; align-items: center;
  background: linear-gradient(135deg, #E8805A, #EC9A7A);
  border-radius: 16rpx; padding: 20rpx 24rpx; margin-bottom: 20rpx;
}
.sub-icon { font-size: 36rpx; margin-right: 16rpx; flex-shrink: 0; }
.sub-text { flex: 1; font-size: 26rpx; color: #fff; font-weight: 500; }
.sub-arrow { color: #fff; font-size: 36rpx; flex-shrink: 0; }

.page-header {
  padding: 0 0 20rpx;
  display: flex;
  align-items: center;
}

.header-title {
  font-size: 40rpx;
  font-weight: 800;
  color: #4E3D35;
}

/* 顶部导航 */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0 24rpx;
}

.nav-tabs {
  display: flex;
  gap: 0;
  background: #F5EFE8;
  border-radius: 20rpx;
  padding: 4rpx;
}

.nav-tab {
  padding: 14rpx 32rpx;
  font-size: 26rpx;
  color: #BFAB98;
  border-radius: 18rpx;
  font-weight: 500;
  transition: all 0.2s;
}

.nav-tab.active {
  background: #FDF5EF;
  color: #4E3D35;
  font-weight: 700;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.06);
}

.nav-logout {
  font-size: 26rpx;
  color: #BFAB98;
  padding: 8rpx 16rpx;
}

.header-badge {
  font-size: 24rpx;
  color: #E8805A;
  background: #FCEDE6;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-weight: 600;
}

/* 订单卡片 */
.order-card {
  background: #FDF5EF;
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.04);
  position: relative;
  overflow: hidden;
  transition: transform 0.2s;
}

.order-card.pending {
  border-left: 6rpx solid #E8805A;
}

.order-card:active { transform: scale(0.985); }

.order-ribbon {
  position: absolute;
  top: 16rpx;
  right: -28rpx;
  background: #E8805A;
  color: #fff;
  font-size: 20rpx;
  font-weight: 700;
  padding: 6rpx 40rpx;
  transform: rotate(45deg);
  letter-spacing: 2rpx;
}

.order-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.order-id {
  font-size: 28rpx;
  font-weight: 700;
  color: #E8805A;
  background: #FCEDE6;
  padding: 6rpx 18rpx;
  border-radius: 10rpx;
}

.order-time {
  font-size: 24rpx;
  color: #BFAB98;
}

/* 菜品列表 */
.order-dishes {
  background: #F5EFE8;
  border-radius: 14rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
}

.dish-chip {
  display: flex;
  align-items: center;
  padding: 10rpx 0;
}

.chip-img {
  width: 60rpx; height: 60rpx;
  border-radius: 10rpx; flex-shrink: 0;
  margin-right: 16rpx; background: #F5EFE8;
}

.chip-name {
  font-size: 28rpx; color: #4E3D35; font-weight: 500;
}

.chip-dot {
  color: #E8805A;
  margin-right: 12rpx;
  font-size: 32rpx;
}

/* 底部 */
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-status {
  font-size: 24rpx;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-weight: 500;
}

.order-status.pending {
  background: #FDF3E0;
  color: #E8A850;
}

.order-status.received {
  background: #EDF3E6;
  color: #6A8347;
}

.done-btn {
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 32rpx;
  background: linear-gradient(135deg, #6A8347, #7D9A5A);
  color: #fff;
  font-size: 26rpx;
  font-weight: 600;
  border-radius: 32rpx;
  border: none;
  box-shadow: 0 4rpx 16rpx rgba(106,131,71,0.2);
  transition: all 0.2s;
}

.done-btn:active {
  transform: scale(0.95);
  opacity: 0.9;
}

.order-actions { display: flex; gap: 16rpx; align-items: center; }

.del-btn {
  height: 64rpx; line-height: 64rpx;
  padding: 0 28rpx;
  background: none; color: #BFAB98;
  font-size: 24rpx; border: 1rpx solid #E8D5C4;
  border-radius: 32rpx;
}
.del-btn:active { background: #FCE8E6; color: #D4756B; border-color: #D4756B; }

/* 空状态 */
.empty-state {
  text-align: center;
  padding-top: 200rpx;
}

.empty-icon { font-size: 100rpx; display: block; margin-bottom: 20rpx; }
.empty-title { font-size: 30rpx; font-weight: 600; color: #8B7355; display: block; margin-bottom: 8rpx; }
.empty-desc { font-size: 26rpx; color: #BFAB98; }

.legal { text-align: center; padding: 20rpx 0 30rpx; }
.icp-text { font-size: 22rpx; color: #BFAB98; display: block; }
.copyright-text { font-size: 22rpx; color: #BFAB98; display: block; margin-top: 6rpx; }
</style>
