<template>
  <!-- 演示模式水印角标（P1-3）：防止把演示数据当真实交付 -->
  <view class="demo-watermark" v-if="show">演示数据</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const show = ref(false)

onMounted(() => {
  // 演示模式由存储标记控制（demo 构建/演示入口设置 'demo_mode'）
  show.value = uni.getStorageSync('demo_mode') === '1'
})
</script>

<style scoped>
.demo-watermark {
  position: fixed;
  top: 24rpx;
  right: 24rpx;
  z-index: 9999;
  background: rgba(212, 117, 107, 0.85);
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
  padding: 6rpx 18rpx;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(212,117,107,0.3);
  pointer-events: none;
}
</style>
