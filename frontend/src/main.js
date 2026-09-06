// Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0
import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())
  return { app }
}
