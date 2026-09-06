<!-- Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0 -->
<script setup>
import { onLaunch } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'

onLaunch(() => {
  const authStore = useAuthStore()
  authStore.checkLogin()
})
</script>

<style>
page {
  background-color: #FAF7F2;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 28rpx;
  color: #4E3D35;
  -webkit-font-smoothing: antialiased;
}

button { padding: 0; margin: 0; background: none; border: none; font-size: inherit; line-height: inherit; }
button::after { border: none; }
input, textarea { outline: none; border: none; background: none; font-family: inherit; }
</style>
