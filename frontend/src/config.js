// Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0
// 白标参数化配置（P2-3）：改此处即可一键改店名 / 夫妻称呼 / 品牌色 / 默认背景。
export const APP = {
  name: '燕公主的食堂',
  hostName: '老公',
  desc: '你点菜 · 我做饭',
  primary: '#E8805A'
}

// 角色→菜单可用的归属侧（点餐端看做饭人的菜单库）
export const baseRole = (role) => (role === 'husband_test' ? 'husband' : role === 'wife_test' ? 'wife' : role)

// DIY 背景预设（用户在小程序可选；存 users.background_url 跨端同步）
export const BACKGROUNDS = [
  { key: 'default', label: '暖橙', value: 'linear-gradient(170deg,#E8805A,#EC9A7A,#F5C4A8,#FAF0E8)' },
  { key: 'peach', label: '蜜桃', value: 'linear-gradient(170deg,#FD8A8A,#F1A7C2,#F7D9E0)' },
  { key: 'mint', label: '薄荷', value: 'linear-gradient(170deg,#8ED1C0,#B8E3D4,#E7F6F0)' },
  { key: 'sky', label: '晴空', value: 'linear-gradient(170deg,#74B7EE,#A6D0F5,#E2F0FC)' },
  { key: 'lavender', label: '淡紫', value: 'linear-gradient(170deg,#A98BD8,#CBB7EC,#EDE4F8)' },
  { key: 'lemon', label: '柠檬', value: 'linear-gradient(170deg,#F2C94C,#F7E7A8,#FEF9E7)' },
  { key: 'choco', label: '可可', value: 'linear-gradient(170deg,#8D6E63,#BCAAA4,#EFEBE9)' }
]

// 默认背景（未选时）
export const DEFAULT_BACKGROUND = BACKGROUNDS[0].value

// 全局 DIY 背景图地址（后台在"背景设置"上传，存 uploads/global-background.jpg）
// 替换为你的后端 API 起始地址（与 src/api/request.js 的 BASE_URL 同源）。
export const BG_IMAGE_URL = 'https://YOUR_BACKEND_DOMAIN_OR_HOST/api/background'
