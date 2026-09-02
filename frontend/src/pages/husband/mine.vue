<template>
  <view class="mine-page" :style="pageBg">
    <view class="nav-bar">
      <view class="nav-tabs">
        <view class="nav-tab" @click="uni.reLaunch({ url: '/pages/husband/menu' })">菜单</view>
        <view class="nav-tab active">订单</view>
      </view>
      <view class="nav-logout" @click="authStore.logout()">退出</view>
    </view>

    <!-- 购物车 -->
    <view class="section cart-section" v-if="cartStore.items.length > 0">
      <view class="section-header">
        <text class="section-title">🛒 待下单</text>
        <text class="action-link" @click="cartStore.clear()">清空</text>
      </view>

      <view class="cart-items">
        <view class="cart-item" v-for="item in cartStore.items" :key="item.dishId">
          <view class="cart-item-left">
            <image
              class="cart-item-img"
              :src="dishImgById(item.dishId, item.imagePath)"
              mode="aspectFill"
            />
            <view class="cart-item-info">
              <view class="cart-item-title">
                <text class="cart-item-name">{{ item.dishName }}</text>
                <text class="cart-item-spice" v-if="item.spiciness !== 'none'" :class="'spice-' + item.spiciness">
                  {{ item.spiciness === 'female_baby' ? '🌶️🌶️' : '🌶️' }}
                </text>
              </view>
              <text class="cart-item-desc" v-if="item.dishDescription">{{ item.dishDescription }}</text>
            </view>
          </view>
          <view class="cart-item-del" @click="cartStore.removeItem(item.dishId)">✕</view>
        </view>
      </view>

      <view class="cart-summary">
        <text class="summary-text">共 <text class="highlight">{{ cartStore.count }}</text> 个菜</text>
      </view>

      <button class="submit-btn" @click="submitOrder" :disabled="submitting">
        <text v-if="submitting" class="btn-loading">提交中...</text>
        <text v-else>确认下单</text>
      </button>
    </view>

    <!-- 购物车空 -->
    <view class="empty-area" v-else>
      <text class="empty-illustration">🛒</text>
      <text class="empty-title">购物车是空的</text>
      <text class="empty-desc">去菜单页选几个想吃的菜</text>
      <button class="link-btn" @click="uni.reLaunch({ url: '/pages/husband/menu' })">← 返回菜单</button>
    </view>

    <!-- 订单记录 -->
    <view class="section order-section" v-if="orders.length > 0">
      <view class="section-header">
        <text class="section-title">📋 我的订单</text>
      </view>

      <view class="order-card" v-for="order in orders" :key="order.id">
        <view class="order-meta">
          <view class="order-id-badge">No.{{ order.orderNumber || order.id }}</view>
          <view class="order-status" :class="order.status">
            {{ order.status === 'pending' ? '等待中' : '已收到' }}
          </view>
        </view>
        <view class="order-dish-list">
          <text class="order-dish-tag" v-for="item in order.items" :key="item.dishId">
            {{ item.dishName }}
            <text v-if="item.spiciness && item.spiciness !== 'none'">
              {{ item.spiciness === 'female_baby' ? ' 🌶️🌶️' : ' 🌶️' }}
            </text>
          </text>
        </view>
        <text class="order-time">{{ formatTime(order.createdAt) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { useCartStore } from '@/store/cart'
import { useAuthStore } from '@/store/auth'
import { createOrder, getOrders } from '@/api/order'
import { dishImgById } from '@/utils/image'
import { DEFAULT_BACKGROUND, BG_IMAGE_URL } from '@/config'

const authStore = useAuthStore()
const cartStore = useCartStore()
const orders = ref([])
const submitting = ref(false)

// 页面背景：全局 DIY 背景图（后台“背景设置”上传，重启后生效），兜底浅色
const pageBg = computed(() => ({
  backgroundImage: `url(${BG_IMAGE_URL})`,
  backgroundSize: 'cover',
  backgroundPosition: 'center',
  backgroundColor: '#FAF7F2'
}))

const loadOrders = async () => {
  try { orders.value = await getOrders() } catch (e) { /* silent */ }
}

const submitOrder = async () => {
  if (cartStore.items.length === 0) return
  submitting.value = true
  try {
    await createOrder(cartStore.orderItems)
    uni.showToast({ title: '下单成功！', icon: 'success' })
    cartStore.clear()
    await loadOrders()
  } catch (e) {
    uni.showToast({ title: '下单失败', icon: 'none' })
  } finally {
    submitting.value = false
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
.mine-page {
  padding: 0 30rpx 60rpx;
  min-height: 100vh;
  background: #FAF7F2;
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0 24rpx;
}

.nav-tabs {
  display: flex;
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

.section {
  background: #FDF5EF;
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #F5EFE8;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #4E3D35;
}

.action-link {
  font-size: 26rpx;
  color: #BFAB98;
}

/* 购物车项 */
.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
}

.cart-item + .cart-item { border-top: 1rpx solid #F0E2D2; }

.cart-item-left {
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
}

.cart-item-img {
  width: 100rpx;
  height: 100rpx;
  border-radius: 14rpx;
  background: #F5EFE8;
  flex-shrink: 0;
}

.cart-item-info {
  flex: 1;
  margin-left: 18rpx;
  overflow: hidden;
}

.cart-item-title { display: flex; align-items: center; gap: 10rpx; }
.cart-item-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #4E3D35;
}
.cart-item-spice { font-size: 24rpx; }
.cart-item-spice.spice-male_baby { opacity: 0.7; }
.cart-item-spice.spice-female_baby { opacity: 1; }

.cart-item-desc {
  font-size: 24rpx;
  color: #8B7355;
  display: block;
  margin-top: 4rpx;
}

.cart-item-del {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  font-size: 26rpx;
  color: #BFAB98;
  border-radius: 50%;
  flex-shrink: 0;
  transition: all 0.2s;
}
.cart-item-del:active { background: #FCE8E6; color: #D4756B; }

.cart-summary {
  text-align: center;
  padding: 24rpx 0 8rpx;
}

.summary-text {
  font-size: 26rpx;
  color: #8B7355;
}

.highlight {
  color: #E8805A;
  font-weight: 700;
  font-size: 30rpx;
}

/* 下单按钮 */
.submit-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #E8805A, #EC9A7A);
  color: #fff;
  font-size: 32rpx;
  font-weight: 700;
  border-radius: 44rpx;
  border: none;
  margin-top: 16rpx;
  box-shadow: 0 6rpx 24rpx rgba(232,128,90,0.25);
  transition: all 0.2s;
}

.submit-btn:active { transform: scale(0.97); opacity: 0.9; }
.submit-btn[disabled] { background: #E8D5C4; box-shadow: none; }

.btn-loading { opacity: 0.7; }

/* 空购物车 */
.empty-area {
  text-align: center;
  padding: 120rpx 0;
}

.empty-illustration {
  font-size: 100rpx;
  display: block;
  margin-bottom: 24rpx;
  filter: grayscale(0.3);
}

.empty-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #8B7355;
  display: block;
  margin-bottom: 10rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: #BFAB98;
  display: block;
  margin-bottom: 40rpx;
}

.link-btn {
  display: inline-block;
  padding: 18rpx 48rpx;
  background: #4E3D35;
  color: #fff;
  font-size: 28rpx;
  border-radius: 40rpx;
  font-weight: 500;
}

/* 订单卡片 */
.order-card {
  padding: 24rpx 0;
}
.order-card + .order-card { border-top: 1rpx solid #F5EFE8; }

.order-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.order-id-badge {
  font-size: 26rpx;
  font-weight: 600;
  color: #E8805A;
  background: #FCEDE6;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}

.order-status {
  font-size: 24rpx;
  padding: 6rpx 18rpx;
  border-radius: 20rpx;
  font-weight: 500;
}

.order-status.pending { background: #FDF3E0; color: #E8A850; }
.order-status.received { background: #EDF3E6; color: #6A8347; }

.order-dish-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-bottom: 12rpx;
}

.order-dish-tag {
  font-size: 26rpx;
  color: #4E3D35;
  background: #F5EFE8;
  padding: 8rpx 18rpx;
  border-radius: 20rpx;
}

.order-time {
  font-size: 22rpx;
  color: #BFAB98;
}
</style>
