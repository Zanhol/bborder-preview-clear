// Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0
const BASE_URL = 'https://YOUR_BACKEND_DOMAIN_OR_HOST'
// 说明：小程序 `request` 需配置绝对 HTTPS 域名（微信「服务器域名」需合法备案域名）。
// 本地联调可指向局域网 IP（勾选开发者工具「不校验合法域名」）。
// 与服务端同域(nginx 反代)时可直接用 '/api' 前缀。

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': options.contentType || 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data)
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('user')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(new Error('登录已过期'))
        } else if (res.statusCode === 403) {
          uni.showToast({ title: res.data?.error || '无权限操作', icon: 'none' })
          reject(new Error(res.data?.error || '无权限操作'))
        } else {
          reject(new Error(res.data?.error || '请求失败'))
        }
      },
      fail: (err) => { reject(new Error('网络错误: ' + err.errMsg)) }
    })
  })
}

export const get = (url, data) => request({ url, method: 'GET', data })
export const post = (url, data) => request({ url, method: 'POST', data })
export const put = (url, data) => request({ url, method: 'PUT', data })
export const del = (url) => request({ url, method: 'DELETE' })
export default request
