// Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0
import { get, del } from './request'

const BASE = 'https://YOUR_BACKEND_DOMAIN_OR_HOST'

export const getDishes = (page = 1, size = 20, ownerFilter = '') => get('/api/dishes', { page, size, ownerFilter })

export const getCategories = () => get('/api/categories')

export const addDish = (name, description, spiciness, category, subcategory, imagePath) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: BASE + '/api/dishes',
      name: 'image',
      filePath: imagePath || undefined,
      formData: { name, description: description || '', spiciness, category, subcategory: subcategory || '' },
      header: { 'Authorization': `Bearer ${token}` },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (res.statusCode >= 200 && res.statusCode < 300) resolve(data)
          else reject(new Error(data.error || '上传失败'))
        } catch (e) { reject(new Error('解析响应失败')) }
      },
      fail: () => reject(new Error('网络错误'))
    })
  })
}

export const updateDish = (id, name, description, spiciness, category, subcategory, owner, imageBlob) => {
  const fields = { name, description: description || '', spiciness, category: category || 'cooking', subcategory: subcategory || '', owner: owner || '' }
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    const header = { 'Authorization': `Bearer ${token}` }
    if (imageBlob) {
      // 换图：multipart 上传 + PUT
      uni.uploadFile({
        url: BASE + `/api/dishes/${id}`,
        method: 'PUT',
        name: 'image',
        filePath: imageBlob,
        formData: fields,
        header,
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (res.statusCode >= 200 && res.statusCode < 300) resolve(data)
            else reject(new Error(data.error || '更新失败'))
          } catch (e) { reject(new Error('解析响应失败')) }
        },
        fail: () => reject(new Error('网络错误'))
      })
    } else {
      // 不换图：表单编码 PUT（不传 filePath，避免 uploadFile 因空文件失败）。
      // 依赖 Spring Boot 默认 FormContentFilter 让后端 @RequestParam 能解析 x-www-form-urlencoded。
      uni.request({
        url: BASE + `/api/dishes/${id}`,
        method: 'PUT',
        data: fields,
        header: { 'Content-Type': 'application/x-www-form-urlencoded', ...header },
        success: (res) => {
          if (res.statusCode >= 200 && res.statusCode < 300) resolve(res.data)
          else reject(new Error(res.data?.error || '更新失败'))
        },
        fail: () => reject(new Error('网络错误'))
      })
    }
  })
}

export const deleteDish = (id) => del(`/api/dishes/${id}`)

export const getDishImageUrl = (id) => BASE + `/api/dishes/${id}/image`
