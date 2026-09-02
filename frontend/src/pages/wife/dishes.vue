<template>
  <view class="dishes-page" :style="pageBg">
    <badge-demo />
    <view class="nav-bar">
      <view class="nav-tabs">
        <view class="nav-tab active">菜品</view>
        <view class="nav-tab" @click="goOrders">订单</view>
      </view>
      <view class="nav-logout" @click="handleLogout">退出</view>
    </view>

    <!-- 今日做饭人提示 -->
    <view class="cook-banner">
      <text class="cook-banner-icon">👩‍🍳</text>
      <text class="cook-banner-text">今天是你做饭 · 菜品库在这里管理，对方看你的拿手菜</text>
    </view>

    <button class="fab-btn" @click="openAddForm">+</button>

    <!-- 新增/编辑弹窗 -->
    <view class="modal-mask" v-if="showForm" @click="cancelForm">
      <view class="modal-panel" @click.stop>
        <text class="modal-title">{{ editingDish ? '编辑菜品' : '新菜品' }}</text>
        <view class="form-group">
          <text class="form-label">菜名</text>
          <input class="form-input" v-model="form.name" placeholder="比如：红烧排骨" maxlength="20" />
        </view>
        <view class="form-group">
          <text class="form-label">描述（选填）</text>
          <textarea class="form-textarea" v-model="form.description" placeholder="口味、做法..." maxlength="100" />
        </view>
        <view class="form-group">
          <text class="form-label">分类</text>
          <view class="cat-row">
            <view v-for="c in cats" :key="c.catKey" class="cat-tag"
              :class="{ active: form.category === c.catKey }" @click="formSwitchCat(c.catKey)">{{ c.catKey === 'cooking' ? '🥬' : '🛵' }} {{ c.label }}</view>
          </view>
        </view>
        <view class="form-group" v-if="form.category === 'cooking'">
          <text class="form-label">子分类</text>
          <view class="subcat-row">
            <view v-for="s in currentSubs" :key="s.value" class="subcat-tag" :class="{ active: form.subcategory === (s.value === '全部' ? '' : s.value) }"
              @click="form.subcategory = (s.value === '全部' ? '' : s.value)">{{ s.label }}</view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">归属菜单（对方看谁的菜单）</text>
          <view class="cat-row">
            <view class="cat-tag" :class="{ active: form.owner === 'own' }" @click="form.owner = 'own'">👤 我的拿手菜</view>
            <view class="cat-tag" :class="{ active: form.owner === 'both' }" @click="form.owner = 'both'">👫 公用菜单</view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">辣度</text>
          <view class="spice-row">
            <view class="spice-tag" :class="{ active: form.spiciness === 'none' }" @click="form.spiciness = 'none'">不辣</view>
            <view class="spice-tag mild" :class="{ active: form.spiciness === 'male_baby' }" @click="form.spiciness = 'male_baby'">🌶️ 男宝辣</view>
            <view class="spice-tag hot" :class="{ active: form.spiciness === 'female_baby' }" @click="form.spiciness = 'female_baby'">🌶️🌶️ 女宝辣</view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">图片</text>
          <view class="image-area" @click="pickImage">
            <image v-if="form.imagePreview" :src="form.imagePreview" class="preview-image" mode="aspectFill" />
            <view v-else class="image-empty">
              <text class="image-icon">📸</text>
              <text class="image-hint">拍照或选图</text>
            </view>
          </view>
        </view>
        <view class="modal-btns">
          <button class="btn-cancel" @click="cancelForm">取消</button>
          <button class="btn-confirm" @click="submitForm" :disabled="!form.name || submitting">
            {{ submitting ? '提交中...' : (editingDish ? '保存修改' : '确认添加') }}
          </button>
        </view>
      </view>
    </view>

    <!-- 搜索 -->
    <view class="search-bar">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input class="search-input" v-model="searchText" placeholder="搜菜名，看哪些已上传..." placeholder-style="color:#BFAB98" />
        <text class="search-clear" v-if="searchText" @click="searchText = ''">✕</text>
      </view>
    </view>

    <!-- 分类（后端驱动） -->
    <view class="cat-bar">
      <view v-for="c in cats" :key="c.catKey" class="cat-item"
        :class="{ active: dishCat === c.catKey }" @click="switchCat(c.catKey)">{{ c.catKey === 'cooking' ? '🥬' : '🛵' }} {{ c.label }}</view>
    </view>

    <!-- 子分类（仅买菜做饭显示） -->
    <view class="subcat-bar" v-if="dishCat === 'cooking'">
      <view v-for="s in currentSubs" :key="s.value" class="subcat-item"
        :class="{ active: subCat === (s.value === '全部' ? '' : s.value) }" @click="subCat = (s.value === '全部' ? '' : s.value)">{{ s.label }}</view>
    </view>

    <!-- 菜品列表 -->
    <view class="dish-list" v-if="filteredDishList.length > 0">
      <view class="dish-card" v-for="dish in filteredDishList" :key="dish.id" @click="openEditForm(dish)">
        <view class="dish-img-wrap">
          <image class="dish-img" :src="dishImg(dish)" mode="aspectFill" />
        </view>
        <view class="dish-body">
          <text class="dish-name">{{ dish.name }}</text>
          <view class="dish-tags-row">
            <view class="owner-badge" :class="'owner-' + dish.owner">{{ ownerLabel(dish.owner) }}</view>
            <view class="spice-badge" :class="spiceClass(dish.spiciness)">{{ spiceLabel(dish.spiciness) }}</view>
          </view>
          <text class="dish-desc" v-if="dish.description">{{ dish.description }}</text>
        </view>
        <view class="dish-del" @click.stop="handleDelete(dish)">✕</view>
      </view>
      <view class="load-more">
        <text v-if="loadingMore">加载中...</text>
        <text v-else-if="hasMore">上拉加载更多</text>
        <text v-else>—— 已显示全部菜品 ——</text>
      </view>
    </view>

    <view class="empty-state" v-else-if="!searchText">
      <text class="empty-icon">📋</text>
      <text class="empty-title">还没有{{ dishCat === 'cooking' ? '做饭' : '外卖' }}菜品</text>
      <text class="empty-desc">点击右下角 + 添加</text>
    </view>

    <view class="empty-state" v-else>
      <text class="empty-icon">🔍</text>
      <text class="empty-title">没找到 "{{ searchText }}"</text>
      <text class="empty-desc">换个关键词试试</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh, onReachBottom as uniOnReachBottom } from '@dcloudio/uni-app'
import { getDishes, addDish, updateDish, deleteDish, getCategories } from '@/api/dish'
import { useAuthStore } from '@/store/auth'
import { dishImg } from '@/utils/image'
import badgeDemo from '@/components/badge-demo.vue'
import { DEFAULT_BACKGROUND, BG_IMAGE_URL } from '@/config'

const authStore = useAuthStore()
const dishes = ref([])

// 页面背景：全局 DIY 背景图（后台“背景设置”上传，重启后生效），兜底浅色
const pageBg = computed(() => ({
  backgroundImage: `url(${BG_IMAGE_URL})`,
  backgroundSize: 'cover',
  backgroundPosition: 'center',
  backgroundColor: '#FAF7F2'
}))
const dishCat = ref('cooking')
const subCat = ref('')
const searchText = ref('')
const page = ref(1)
const total = ref(0)
const pageSize = 20
const loading = ref(false)
const loadingMore = ref(false)
const reloadSeq = ref(0)

const cats = ref([])

const loadCategories = async () => {
  try { cats.value = await getCategories() } catch (e) { /* 静默 */ }
}

const currentSubs = computed(() => {
  const c = cats.value.find(x => x.catKey === dishCat.value)
  return c ? (c.subcategories || []) : []
})

const switchCat = (cat) => {
  dishCat.value = cat
  subCat.value = ''   // 切换大分类时重置子分类
}

const formSwitchCat = (cat) => {
  form.value.category = cat
  if (cat !== 'cooking') form.value.subcategory = ''   // 切到懒人外卖时清空子分类
}

// 新到旧展示（后端已按 created_at DESC, id DESC 返回，拼接即新→旧）
const filteredDishList = computed(() => {
  let list = dishes.value.filter(d => d.category === dishCat.value)
  if (dishCat.value === 'cooking' && subCat.value) {
    list = list.filter(d => d.subcategory === subCat.value)
  }
  if (!searchText.value) return list
  const kw = searchText.value.toLowerCase()
  return list.filter(
    d => (d.name && d.name.toLowerCase().includes(kw)) ||
         (d.description && d.description.toLowerCase().includes(kw))
  )
})

const goOrders = () => uni.reLaunch({ url: '/pages/wife/orders' })
const handleLogout = () => authStore.logout()

const loadDishes = async () => {
  reloadSeq.value++   // 使在途 loadMore 的响应失效
  loading.value = true
  try {
    const data = await getDishes(1, pageSize)
    dishes.value = (data && data.items) ? data.items : []
    total.value = (data && data.total) ? Number(data.total) : dishes.value.length
    page.value = 1
  } catch (e) { /* */ } finally {
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
  } catch (e) { /* */ } finally {
    loadingMore.value = false
  }
}

// 表单
const showForm = ref(false)
const submitting = ref(false)
const editingDish = ref(null)
const form = ref({ name: '', description: '', spiciness: 'none', category: 'cooking', subcategory: '', owner: 'own', imagePreview: '', imageFile: null })

const openAddForm = () => {
  editingDish.value = null
  form.value = { name: '', description: '', spiciness: 'none', category: 'cooking', subcategory: '', owner: 'own', imagePreview: '', imageFile: null }
  showForm.value = true
}

const openEditForm = (dish) => {
  editingDish.value = dish
  form.value = {
    name: dish.name, description: dish.description || '',
    spiciness: dish.spiciness || 'none', category: dish.category || 'cooking',
    subcategory: dish.subcategory || '',
    owner: dish.owner === 'both' ? 'both' : 'own',
    imagePreview: dish.imagePath ? '/api/dishes/' + dish.id + '/image' : '', imageFile: null
  }
  showForm.value = true
}

// 归属标签：husband/wife/both
const ownerLabel = (o) => {
  if (o === 'both') return '公用'
  if (o === 'husband') return '老公的菜'
  return '老婆的菜'
}

const cancelForm = () => { showForm.value = false; editingDish.value = null }

// 拍照/选图
const pickImage = () => {
  uni.chooseImage({
    count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'],
    success: (res) => {
      const path = res.tempFilePaths[0]
      wx.cropImage({
        src: path, cropScale: '1:1',
        success: (cropRes) => {
          form.value.imagePreview = cropRes.tempFilePath
          form.value.imageFile = cropRes.tempFilePath
        },
        fail: () => {
          form.value.imagePreview = path
          form.value.imageFile = path
        }
      })
    }
  })
}

// 提交
const submitForm = async () => {
  if (!form.value.name.trim()) return
  submitting.value = true
  try {
    // 'own' → 自己的角色归属；'both' → 公用菜单
    const owner = form.value.owner === 'both' ? 'both' : (authStore.user?.role || 'wife')
    if (editingDish.value) {
      await updateDish(editingDish.value.id, form.value.name.trim(), form.value.description.trim(), form.value.spiciness, form.value.category, form.value.subcategory, owner, form.value.imageFile)
      uni.showToast({ title: '已更新', icon: 'success' })
    } else {
      await addDish(form.value.name.trim(), form.value.description.trim(), form.value.spiciness, form.value.category, form.value.subcategory, form.value.imageFile)
      uni.showToast({ title: '已添加', icon: 'success' })
    }
    showForm.value = false; editingDish.value = null
    await loadDishes()
  } catch (e) {
    uni.showToast({ title: editingDish.value ? '更新失败' : '添加失败', icon: 'none' })
  } finally { submitting.value = false }
}

const handleDelete = (dish) => {
  uni.showModal({
    title: '删除菜品', content: `确定删除「${dish.name}」吗？`, confirmColor: '#D4756B',
    success: async (res) => {
      if (res.confirm) { await deleteDish(dish.id); await loadDishes() }
    }
  })
}

const spiceClass = (s) => {
  if (s === 'female_baby') return 'hot'
  if (s === 'male_baby') return 'mild'
  return 'none'
}
const spiceLabel = (s) => {
  if (s === 'female_baby') return '女宝辣'
  if (s === 'male_baby') return '男宝辣'
  return '不辣'
}

onMounted(async () => {
  await loadCategories()
  if (cats.value.length) dishCat.value = cats.value[0].catKey
  loadDishes()
})
onPullDownRefresh(async () => { await loadDishes(); uni.stopPullDownRefresh() })
uniOnReachBottom(() => { loadMore() })
</script>

<style scoped>
.dishes-page { padding: 20rpx 30rpx 120rpx; min-height: 100vh; background: #FAF7F2; }
.nav-bar { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 0 24rpx; }

/* 今日做饭人提示条 */
.cook-banner {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
  background: linear-gradient(135deg, #6A8347, #7D9A5A);
  border-radius: 16rpx;
  padding: 18rpx 24rpx;
}
.cook-banner-icon { font-size: 32rpx; flex-shrink: 0; }
.cook-banner-text { font-size: 26rpx; color: #fff; font-weight: 500; }
.nav-tabs { display: flex; background: #F5EFE8; border-radius: 20rpx; padding: 4rpx; }
.nav-tab { padding: 14rpx 32rpx; font-size: 26rpx; color: #BFAB98; border-radius: 18rpx; font-weight: 500; }
.nav-tab.active { background: #FDF5EF; color: #4E3D35; font-weight: 700; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.06); }
.nav-logout { font-size: 26rpx; color: #BFAB98; padding: 8rpx 16rpx; }
.fab-btn { position: fixed; bottom: 40rpx; right: 30rpx; width: 100rpx; height: 100rpx; border-radius: 50%; background: linear-gradient(135deg, #E8805A, #EC9A7A); color: #fff; font-size: 48rpx; font-weight: 300; border: none; display: flex; align-items: center; justify-content: center; box-shadow: 0 8rpx 32rpx rgba(232,128,90,0.35); z-index: 100; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: flex-end; justify-content: center; z-index: 200; animation: fadeIn .2s ease; }
@keyframes fadeIn { from{opacity:0} to{opacity:1} }
.modal-panel { background: #FDF5EF; border-radius: 32rpx 32rpx 0 0; padding: 40rpx 36rpx 50rpx; width: 100%; max-height: 88vh; overflow-y: auto; animation: slideUp .3s ease; }
@keyframes slideUp { from{transform:translateY(100%)} to{transform:translateY(0)} }
.modal-title { font-size: 36rpx; font-weight: 800; text-align: center; display: block; margin-bottom: 36rpx; color: #4E3D35; }
.form-group { margin-bottom: 20rpx; }
.form-label { font-size: 26rpx; font-weight: 600; color: #8B7355; margin-bottom: 10rpx; display: block; }
.form-input { background: #F5EFE8; height: 88rpx; border-radius: 16rpx; padding: 0 24rpx; font-size: 30rpx; width: 100%; box-sizing: border-box; }
.form-textarea { background: #F5EFE8; border-radius: 16rpx; padding: 20rpx 24rpx; font-size: 28rpx; width: 100%; box-sizing: border-box; min-height: 100rpx; }
.cat-row { display: flex; gap: 14rpx; }
.cat-tag { flex: 1; height: 72rpx; display: flex; align-items: center; justify-content: center; border-radius: 18rpx; font-size: 24rpx; font-weight: 500; background: #F5EFE8; color: #BFAB98; border: 2rpx solid transparent; transition: all .2s; }
.cat-tag.active { border-color: #6A8347; background: #EDF3E6; color: #6A8347; font-weight: 700; }
.spice-row { display: flex; gap: 14rpx; }
.spice-tag { flex: 1; height: 72rpx; display: flex; align-items: center; justify-content: center; border-radius: 18rpx; font-size: 24rpx; font-weight: 500; background: #F5EFE8; color: #BFAB98; border: 2rpx solid transparent; transition: all .2s; }
.spice-tag.active { border-color: #E8805A; background: #FCEDE6; color: #E8805A; font-weight: 700; }
.spice-tag.mild.active { border-color: #E8A850; background: #FDF3E0; color: #E8A850; }
.spice-tag.hot.active { border-color: #D4756B; background: #FCE8E6; color: #D4756B; }
.image-area { width: 100%; height: 300rpx; border-radius: 16rpx; overflow: hidden; background: #F5EFE8; }
.preview-image { width: 100%; height: 100%; }
.image-empty { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; border: 2rpx dashed #E8D5C4; border-radius: 16rpx; }
.image-icon { font-size: 56rpx; margin-bottom: 10rpx; }
.image-hint { font-size: 26rpx; color: #BFAB98; }
.modal-btns { display: flex; gap: 20rpx; margin-top: 12rpx; }
.btn-cancel { flex: 1; height: 88rpx; line-height: 88rpx; background: #F5EFE8; color: #8B7355; border-radius: 44rpx; border: none; font-size: 30rpx; font-weight: 500; }
.btn-confirm { flex: 1; height: 88rpx; line-height: 88rpx; background: linear-gradient(135deg, #E8805A, #EC9A7A); color: #fff; border-radius: 44rpx; border: none; font-size: 30rpx; font-weight: 600; box-shadow: 0 6rpx 20rpx rgba(232,128,90,0.25); }
.btn-confirm[disabled] { background: #E8D5C4; box-shadow: none; }
.search-bar { padding: 8rpx 0 16rpx; }
.search-box { display: flex; align-items: center; background: #FDF5EF; height: 80rpx; border-radius: 40rpx; padding: 0 24rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04); }
.search-icon { font-size: 28rpx; opacity: 0.5; flex-shrink: 0; margin-right: 12rpx; }
.search-input { flex: 1; font-size: 28rpx; height: 80rpx; color: #4E3D35; }
.search-clear { font-size: 28rpx; color: #BFAB98; padding: 8rpx 8rpx; flex-shrink: 0; }
.cat-bar { display: flex; gap: 12rpx; padding: 0 0 20rpx; }
.cat-item { padding: 14rpx 28rpx; font-size: 26rpx; background: #FDF5EF; border-radius: 20rpx; color: #BFAB98; font-weight: 500; transition: all .2s; }
.cat-item.active { background: #E8805A; color: #fff; font-weight: 600; }
.subcat-bar { display: flex; gap: 10rpx; flex-wrap: wrap; padding: 0 0 20rpx; }
.subcat-item { padding: 8rpx 20rpx; font-size: 24rpx; background: #FBF3EA; border-radius: 28rpx; color: #8B7355; font-weight: 500; }
.subcat-item.active { background: #E8805A; color: #fff; font-weight: 600; }
.subcat-row { display: flex; flex-wrap: wrap; gap: 12rpx; }
.subcat-tag { padding: 10rpx 20rpx; font-size: 24rpx; border-radius: 14rpx; background: #F5EFE8; color: #BFAB98; border: 2rpx solid transparent; font-weight: 500; }
.subcat-tag.active { border-color: #6A8347; background: #EDF3E6; color: #6A8347; font-weight: 700; }
.dish-list { margin-top: 8rpx; }
.load-more { text-align: center; padding: 24rpx 0 40rpx; font-size: 24rpx; color: #BFAB98; }
.dish-card { background: #FDF5EF; border-radius: 20rpx; padding: 20rpx 24rpx; margin-bottom: 18rpx; display: flex; align-items: center; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03); position: relative; }
.dish-img-wrap { width: 110rpx; height: 110rpx; border-radius: 14rpx; overflow: hidden; flex-shrink: 0; background: #F5EFE8; }
.dish-img { width: 100%; height: 100%; }
.dish-body { flex: 1; margin-left: 18rpx; overflow: hidden; }
.dish-name { font-size: 30rpx; font-weight: 700; color: #4E3D35; display: block; margin-bottom: 4rpx; }
.dish-tags-row { display: flex; gap: 10rpx; align-items: center; margin-bottom: 4rpx; flex-wrap: wrap; }
.owner-badge { font-size: 20rpx; padding: 4rpx 14rpx; border-radius: 12rpx; font-weight: 500; }
.owner-badge.owner-both { background: #F0EAF8; color: #7A5AA8; }
.owner-badge.owner-husband { background: #E8F0FA; color: #4A7BA6; }
.owner-badge.owner-wife { background: #FAE8E0; color: #C0705A; }
.dish-desc { font-size: 24rpx; color: #8B7355; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.dish-tags { margin-right: 12rpx; }
.spice-badge { font-size: 20rpx; padding: 4rpx 14rpx; border-radius: 12rpx; font-weight: 500; }
.spice-badge.none { background: #EDF3E6; color: #6A8347; }
.spice-badge.mild { background: #FDF3E0; color: #E8A850; }
.spice-badge.hot { background: #FCE8E6; color: #D4756B; }
.dish-del { width: 48rpx; height: 48rpx; line-height: 48rpx; text-align: center; font-size: 24rpx; color: #BFAB98; border-radius: 50%; flex-shrink: 0; margin-left: 8rpx; }
.dish-del:active { background: #FCE8E6; color: #D4756B; }
.empty-state { text-align: center; padding-top: 180rpx; }
.empty-icon { font-size: 100rpx; display: block; margin-bottom: 20rpx; }
.empty-title { font-size: 30rpx; font-weight: 600; color: #8B7355; display: block; margin-bottom: 8rpx; }
.empty-desc { font-size: 26rpx; color: #BFAB98; }
</style>
