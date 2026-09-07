<!-- Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0 -->
<template>
  <view class="menu-page" :style="pageBg">
    <badge-demo />
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <view class="nav-tabs">
        <view class="nav-tab active">菜单</view>
        <view class="nav-tab" @click="goOrders">我的订单</view>
      </view>
      <view class="nav-logout" @click="handleLogout">退出</view>
    </view>

    <!-- 做饭状态 -->
    <view class="cook-status-banner" :class="cookStatus">
      <text>{{ cookStatus === 'done' ? '🍽️ 饭好啦，快来吃饭！' : '👩‍🍳 老婆做饭中...' }}</text>
    </view>

    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="searchText"
          placeholder="搜一下想吃的..."
          placeholder-style="color:#BFAB98"
        />
        <text class="search-clear" v-if="searchText" @click="searchText = ''">✕</text>
      </view>
    </view>

    <!-- 分类 Tab（后端驱动） -->
    <view class="cat-bar">
      <view v-for="c in cats" :key="c.catKey" class="cat-item"
        :class="{ active: activeCat === c.catKey }" @click="switchCat(c.catKey)">{{ catIcon(c.catKey) }} {{ c.label }}</view>
    </view>

    <!-- 子分类（后端驱动） -->
    <view class="subcat-bar" v-if="currentSubs.length > 0">
      <view v-for="s in currentSubs" :key="s.value" class="subcat-item"
        :class="{ active: subCat === (s.value === '全部' ? '' : s.value) }" @click="subCat = (s.value === '全部' ? '' : s.value)">{{ s.label }}</view>
    </view>

    <!-- 辣度选择弹窗 -->
    <view class="spice-modal-mask" v-if="showSpice" @click="showSpice = false">
      <view class="spice-modal" @click.stop>
        <text class="spice-modal-title">选择辣度 · {{ spiceDish?.name }}</text>
        <view class="spice-options">
          <view class="spice-opt" :class="{ active: pickSpice === 'none' }" @click="pickSpice = 'none'">不辣</view>
          <view class="spice-opt mild" :class="{ active: pickSpice === 'male_baby' }" @click="pickSpice = 'male_baby'">🌶️ 男宝辣</view>
          <view class="spice-opt hot" :class="{ active: pickSpice === 'female_baby' }" @click="pickSpice = 'female_baby'">🌶️🌶️ 女宝辣</view>
        </view>
        <button class="spice-confirm" @click="confirmAddSpice">确认加入</button>
      </view>
    </view>

    <!-- 加载中 -->
    <view class="loading-box" v-if="loading">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 菜品列表 -->
    <view class="dish-list" v-else-if="filteredDishes.length > 0">
      <view class="dish-card" v-for="(dish, idx) in filteredDishes" :key="dish.id" :style="{ animationDelay: (idx * 0.05) + 's' }">
        <view class="dish-img-wrap" @click="previewImage(dish)">
          <image
            class="dish-image"
            :src="dishImg(dish)"
            mode="aspectFill"
          />
          <view class="img-overlay" v-if="!dish.imagePath">
            <text class="img-placeholder">📷</text>
          </view>
        </view>
        <view class="dish-body">
          <view class="dish-name-row">
            <text class="dish-name">{{ dish.name }}</text>
            <text class="dish-cat" :class="dish.category === 'delivery' ? 'cat-delivery' : 'cat-cooking'">{{ dish.category === 'delivery' ? '外卖' : '做饭' }}</text>
          </view>
          <text class="dish-desc" v-if="dish.description">{{ dish.description }}</text>
        </view>
        <button class="add-btn" @click="openSpicePicker(dish)">
          <text class="add-icon">+</text>
        </button>
      </view>
    </view>

    <!-- 上拉加载更多 -->
    <view class="load-more" v-if="filteredDishes.length > 0">
      <text v-if="loadingMore">加载中...</text>
      <text v-else-if="hasMore">上拉加载更多</text>
      <text v-else>—— 已显示全部菜品 ——</text>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else-if="!loading && !searchText">
      <view class="empty-illustration">🍳</view>
      <text class="empty-title">还没有菜品</text>
      <text class="empty-desc">催老婆赶紧上传几道菜~</text>
    </view>

    <view class="empty-state" v-else>
      <view class="empty-illustration">🔍</view>
      <text class="empty-title">没找到 "{{ searchText }}"</text>
      <text class="empty-desc">试试别的关键词</text>
    </view>

    <!-- 底部浮条 -->
    <view class="cart-float" v-if="cartStore.count > 0" @click="showCartPanel = true">
      <view class="cart-badge">{{ cartStore.count }}</view>
      <text class="cart-label">购物车</text>
      <view class="cart-dishes">
        <text class="cart-dish-name" v-for="item in cartStore.items.slice(0, 3)" :key="item.dishId">
          {{ item.dishName }}
        </text>
        <text class="cart-more" v-if="cartStore.items.length > 3">等</text>
      </view>
      <text class="cart-arrow">→</text>
    </view>

    <!-- 购物车浮层面板（就地预览/改口味/删除/下单，不再跳转） -->
    <view class="cart-panel-mask" v-if="showCartPanel" @click="showCartPanel = false">
      <view class="cart-panel" @click.stop>
        <view class="panel-head">
          <text class="panel-title">🛒 已选 {{ cartStore.count }} 道菜</text>
          <text class="panel-clear" @click="cartStore.clear()">清空</text>
        </view>
        <scroll-view scroll-y class="panel-list">
          <view class="panel-item" v-for="item in cartStore.items" :key="item.dishId">
            <image class="panel-img" :src="dishImg({ id: item.dishId, imagePath: item.imagePath })" mode="aspectFill" />
            <view class="panel-info">
              <text class="panel-name">{{ item.dishName }}</text>
              <view class="panel-spice" @click="changeSpice(item)">
                <text>{{ item.spiciness === 'none' ? '不辣' : item.spiciness === 'male_baby' ? '🌶️ 男宝辣' : '🌶️🌶️ 女宝辣' }}</text>
                <text class="panel-switch">点按改口味</text>
              </view>
            </view>
            <view class="panel-del" @click="removeCartItem(item.dishId)">✕</view>
          </view>
          <view class="panel-empty" v-if="cartStore.items.length === 0">
            <text>购物车已空 🛒</text>
          </view>
        </scroll-view>
        <button class="panel-submit" @click="submitCart" :disabled="submitting || cartStore.items.length === 0">
          <text v-if="submitting">提交中...</text>
          <text v-else>确认下单 · 共 {{ cartStore.count }} 道</text>
        </button>
      </view>
    </view>

    <!-- 版权 -->
    <view class="legal">
      <text class="icp-text">粤ICP备2026129595号-1</text>
      <text class="copyright-text">开发主体及版权归属：深圳市甜梦屋科技有限公司</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh, onReachBottom as uniOnReachBottom } from '@dcloudio/uni-app'
import { useCartStore } from '@/store/cart'
import { getDishes, getCategories } from '@/api/dish'
import { createOrder } from '@/api/order'
import { useAuthStore } from '@/store/auth'
import { getCurrentCook } from '@/api/cook'
import { dishImg, dishImgFull } from '@/utils/image'
import badgeDemo from '@/components/badge-demo.vue'
import { DEFAULT_BACKGROUND, BG_IMAGE_URL } from '@/config'

const authStore = useAuthStore()
const cartStore = useCartStore()

// 页面背景：全局 DIY 背景图（后台“背景设置”上传，重启后生效），兜底浅色
const pageBg = computed(() => ({
  backgroundImage: `url(${BG_IMAGE_URL})`,
  backgroundSize: 'cover',
  backgroundPosition: 'center',
  backgroundColor: '#FAF7F2'
}))
const dishes = ref([])
const loading = ref(true)
const page = ref(1)
const total = ref(0)
const pageSize = 20
const loadingMore = ref(false)
const reloadSeq = ref(0)
const searchText = ref('')
const activeCat = ref('cooking')
const subCat = ref('')
const showSpice = ref(false)
const spiceDish = ref(null)
const pickSpice = ref('none')

const cats = ref([])

const loadCategories = async () => {
  try { cats.value = await getCategories() } catch (e) { /* 拉取失败用空，分类栏空 */ }
}

const catIcon = (key) => key === 'cooking' ? '🥬' : '🛵'
const currentSubs = computed(() => {
  const c = cats.value.find(x => x.catKey === activeCat.value)
  return c ? (c.subcategories || []) : []
})

const openSpicePicker = (dish) => {
  spiceDish.value = dish
  pickSpice.value = 'none'
  showSpice.value = true
}

const confirmAddSpice = () => {
  cartStore.addItem({ ...spiceDish.value, spiciness: pickSpice.value })
  showSpice.value = false
}

const switchCat = (cat) => {
  activeCat.value = cat
  subCat.value = ''   // 切换大分类时重置子分类
}

const filteredDishes = computed(() => {
  let list = dishes.value
  if (activeCat.value !== 'all') list = list.filter(d => d.category === activeCat.value)
  if (activeCat.value === 'cooking' && subCat.value) {
    list = list.filter(d => d.subcategory === subCat.value)
  }
  if (!searchText.value) return list
  const kw = searchText.value.toLowerCase()
  return list.filter(
    d => (d.name && d.name.toLowerCase().includes(kw)) ||
         (d.description && d.description.toLowerCase().includes(kw))
  )
})

const loadDishes = async () => {
  reloadSeq.value++   // 使在途 loadMore 的响应失效
  loading.value = true
  try {
    const data = await getDishes(1, pageSize)
    dishes.value = (data && data.items) ? data.items : []
    total.value = (data && data.total) ? Number(data.total) : dishes.value.length
    page.value = 1
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const hasMore = computed(() => dishes.value.length < total.value)

const loadMore = async () => {
  if (loading.value || loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  const seq = reloadSeq.value   // 记录调用时的版本
  try {
    const next = page.value + 1
    const data = await getDishes(next, pageSize)
    const items = (data && data.items) ? data.items : []
    if (seq !== reloadSeq.value) return   // 期间列表被重载，丢弃本次结果
    dishes.value = dishes.value.concat(items)
    page.value = next
  } catch (e) {
    /* 静默，下次上拉再试 */
  } finally {
    loadingMore.value = false
  }
}

uniOnReachBottom(() => { loadMore() })

const previewImage = (dish) => {
  if (!dish.imagePath) return
  uni.previewImage({
    urls: [dishImgFull(dish)],
    current: 0
  })
}

const goToMine = () => {
  uni.reLaunch({ url: '/pages/husband/mine' })
}

// 购物车浮层：改口味（点击循环）/删除/就地确认下单
const showCartPanel = ref(false)
const submitting = ref(false)
const changeSpice = (item) => {
  const order = ['none', 'male_baby', 'female_baby']
  const idx = order.indexOf(item.spiciness)
  item.spiciness = order[(idx + 1) % order.length]
}
const removeCartItem = (dishId) => cartStore.removeItem(dishId)
const submitCart = async () => {
  if (cartStore.items.length === 0) return
  submitting.value = true
  try {
    await createOrder(cartStore.orderItems)
    uni.showToast({ title: '下单成功，已通知做饭人 🍳', icon: 'success' })
    cartStore.clear()
    showCartPanel.value = false
  } catch (e) {
    uni.showToast({ title: (e && e.message) || '下单失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

const goOrders = () => {
  uni.reLaunch({ url: '/pages/husband/mine' })
}

const cookStatus = ref('cooking')
const loadCookStatus = async () => {
  try {
    const c = await getCurrentCook()
    cookStatus.value = c.cookStatus || 'cooking'
  } catch (e) { /* 静默 */ }
}

const handleLogout = () => {
  authStore.logout()
}

onMounted(async () => {
  authStore.requestSubscribe()
  await loadCategories()
  if (cats.value.length) activeCat.value = cats.value[0].catKey
  loadDishes()
  loadCookStatus()
})

onPullDownRefresh(async () => {
  await loadDishes()
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.menu-page {
  padding-bottom: 140rpx;
  background: #FAF7F2;
  min-height: 100vh;
}

.page-header {
  padding: 32rpx 30rpx 8rpx;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.header-title {
  font-size: 40rpx;
  font-weight: 800;
  color: #4E3D35;
}

.header-count {
  font-size: 24rpx;
  color: #999;
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0 16rpx;
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
.cook-status-banner { margin: 0 30rpx 16rpx; padding: 18rpx 24rpx; border-radius: 16rpx; font-size: 26rpx; font-weight: 600; background: #EDF3E6; color: #6A8347; }
.cook-status-banner.done { background: #FFF1E6; color: #E8805A; }

.search-bar {
  padding: 16rpx 30rpx 24rpx;
}

.search-box {
  background: #FDF5EF;
  height: 80rpx;
  border-radius: 40rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.search-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
  flex-shrink: 0;
  opacity: 0.5;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  height: 80rpx;
  color: #4E3D35;
}

.search-clear {
  font-size: 28rpx;
  color: #BFAB98;
  padding: 8rpx 12rpx;
  flex-shrink: 0;
}

/* 分类栏 */
.cat-bar { display: flex; gap: 12rpx; padding: 0 30rpx 20rpx; }
.cat-item { padding: 14rpx 28rpx; font-size: 26rpx; background: #FDF5EF; border-radius: 20rpx; color: #BFAB98; font-weight: 500; transition: all .2s; }
.cat-item.active { background: #E8805A; color: #fff; font-weight: 600; }
.subcat-bar { display: flex; gap: 10rpx; flex-wrap: wrap; padding: 0 30rpx 20rpx; }
.subcat-item { padding: 8rpx 20rpx; font-size: 24rpx; background: #FBF3EA; border-radius: 28rpx; color: #8B7355; font-weight: 500; }
.subcat-item.active { background: #E8805A; color: #fff; font-weight: 600; }
.loading-box { text-align: center; padding: 100rpx 0; }
.loading-text { font-size: 28rpx; color: #BFAB98; }
.spice-modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 300; }
.spice-modal { background: #FDF5EF; border-radius: 24rpx; padding: 40rpx; width: 560rpx; }
.spice-modal-title { font-size: 32rpx; font-weight: 700; text-align: center; display: block; margin-bottom: 30rpx; }
.spice-options { display: flex; flex-direction: column; gap: 16rpx; margin-bottom: 30rpx; }
.spice-opt { padding: 24rpx; border-radius: 16rpx; border: 2rpx solid #E8D5C4; text-align: center; font-size: 30rpx; font-weight: 600; color: #8B7355; background: #FFF8F0; transition: all 0.2s; }
.spice-opt.active { border-color: #E8805A; background: #FCEDE6; color: #E8805A; }
.spice-opt.mild.active { border-color: #E8A850; background: #FDF3E0; color: #E8A850; }
.spice-opt.hot.active { border-color: #D4756B; background: #FCE8E6; color: #D4756B; }
.spice-confirm { width: 100%; height: 80rpx; line-height: 80rpx; background: #E8805A; color: #fff; border-radius: 40rpx; font-size: 30rpx; font-weight: 600; border: none; }

/* 菜品列表 */
.dish-list {
  padding: 0 30rpx;
}

.dish-card {
  background: #FDF5EF;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.04);
  transition: transform 0.2s ease;
  animation: cardIn 0.4s ease-out both;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(16rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.dish-card:active {
  transform: scale(0.985);
  background: #FAF2EA;
}

.dish-img-wrap {
  width: 140rpx;
  height: 140rpx;
  border-radius: 16rpx;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  background: #F5EFE8;
}

.dish-image {
  width: 100%;
  height: 100%;
}

.img-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F5EFE8;
}

.img-placeholder {
  font-size: 40rpx;
  opacity: 0.4;
}

.dish-body {
  flex: 1;
  margin-left: 20rpx;
  margin-right: 12rpx;
  overflow: hidden;
}

.dish-name-row { display: flex; align-items: center; gap: 12rpx; margin-bottom: 6rpx; }
.dish-name {
  font-size: 32rpx;
  font-weight: 700;
  color: #4E3D35;
}
.dish-cat { font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 10rpx; flex-shrink: 0; }
.cat-cooking { background: #EDF3E6; color: #6A8347; }
.cat-delivery { background: #FCEDE6; color: #E8805A; }

.dish-desc {
  font-size: 24rpx;
  color: #8B7355;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.add-btn {
  flex-shrink: 0;
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #E8805A;
  border: none;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 14rpx rgba(232,128,90,0.3);
  transition: all 0.2s ease;
}

.add-btn:active {
  transform: scale(0.9);
  box-shadow: 0 2rpx 8rpx rgba(232,128,90,0.2);
}

.add-icon {
  color: #fff;
  font-size: 36rpx;
  font-weight: 300;
  line-height: 1;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding-top: 180rpx;
  padding-left: 40rpx;
  padding-right: 40rpx;
}

.load-more {
  text-align: center;
  padding: 24rpx 0 40rpx;
  font-size: 24rpx;
  color: #BFAB98;
}

.empty-illustration {
  font-size: 100rpx;
  display: block;
  margin-bottom: 20rpx;
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
}

/* 底部浮条 */
.cart-float {
  position: fixed;
  bottom: 24rpx;
  left: 30rpx;
  right: 30rpx;
  height: 96rpx;
  background: #4E3D35;
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  padding: 0 28rpx;
  z-index: 100;
  box-shadow: 0 8rpx 32rpx rgba(78,61,53,0.25);
  transition: transform 0.2s ease;
}

.cart-float:active {
  transform: scale(0.98);
}

.cart-badge {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  background: #E8805A;
  color: #fff;
  border-radius: 50%;
  font-size: 24rpx;
  font-weight: 700;
  flex-shrink: 0;
  margin-right: 16rpx;
}

.cart-label {
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.cart-dishes {
  flex: 1;
  display: flex;
  gap: 8rpx;
  overflow: hidden;
  align-items: center;
}

.cart-dish-name {
  font-size: 22rpx;
  color: rgba(255,255,255,0.7);
  background: rgba(255,255,255,0.12);
  padding: 4rpx 12rpx;
  border-radius: 10rpx;
  white-space: nowrap;
}

.cart-more {
  font-size: 22rpx;
  color: rgba(255,255,255,0.5);
}

.cart-arrow {
  color: #fff;
  font-size: 32rpx;
  flex-shrink: 0;
  margin-left: 8rpx;
}

/* 购物车浮层 */
.cart-panel-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); display: flex; align-items: flex-end; justify-content: center; z-index: 200; }
.cart-panel { width: 100%; background: #FDF8F3; border-radius: 32rpx 32rpx 0 0; padding: 32rpx 30rpx 40rpx; box-sizing: border-box; }
.panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.panel-title { font-size: 32rpx; font-weight: 700; color: #4E3D35; }
.panel-clear { font-size: 26rpx; color: #BFAB98; }
.panel-list { max-height: 50vh; }
.panel-item { display: flex; align-items: center; gap: 18rpx; padding: 16rpx 0; border-bottom: 1rpx solid #F0E2D2; }
.panel-img { width: 96rpx; height: 96rpx; border-radius: 14rpx; background: #F5EFE8; flex-shrink: 0; }
.panel-info { flex: 1; min-width: 0; }
.panel-name { font-size: 28rpx; font-weight: 600; color: #4E3D35; display: block; }
.panel-spice { display: flex; align-items: center; gap: 10rpx; margin-top: 6rpx; font-size: 24rpx; color: #D4756B; }
.panel-switch { font-size: 20rpx; color: #BFAB98; background: #F5EFE8; padding: 2rpx 12rpx; border-radius: 10rpx; }
.panel-del { width: 48rpx; height: 48rpx; line-height: 48rpx; text-align: center; font-size: 26rpx; color: #BFAB98; flex-shrink: 0; }
.panel-del:active { background: #FCE8E6; color: #D4756B; }
.panel-empty { text-align: center; color: #BFAB98; padding: 30rpx 0; font-size: 26rpx; }
.panel-submit { width: 100%; height: 88rpx; line-height: 88rpx; background: linear-gradient(135deg, #E8805A, #EC9A7A); color: #fff; font-size: 32rpx; font-weight: 700; border-radius: 44rpx; border: none; margin-top: 24rpx; box-shadow: 0 6rpx 24rpx rgba(232,128,90,.25); }
.panel-submit:active { transform: scale(.97); }
.panel-submit[disabled] { background: #E8D5C4; box-shadow: none; }

/* 版权 */
.legal { text-align: center; padding: 20rpx 30rpx 30rpx; }
.icp-text { font-size: 22rpx; color: #BFAB98; display: block; }
.copyright-text { font-size: 22rpx; color: #BFAB98; display: block; margin-top: 6rpx; }
</style>
