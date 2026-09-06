// Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0
const IMG_BASE = 'https://YOUR_BACKEND_DOMAIN_OR_HOST'

// 无图菜品返回空字符串，避免请求不存在的 /static/default-dish.png 导致 500
const NO_IMAGE = ''

export const dishImg = (dish) => {
  if (!dish || !dish.imagePath) return NO_IMAGE
  return IMG_BASE + '/api/dishes/' + dish.id + '/thumb'
}

export const dishImgById = (id, imagePath) => {
  if (!imagePath) return NO_IMAGE
  return IMG_BASE + '/api/dishes/' + id + '/thumb'
}

export const dishImgFull = (dish) => {
  if (!dish || !dish.imagePath) return NO_IMAGE
  return IMG_BASE + '/api/dishes/' + dish.id + '/image'
}
