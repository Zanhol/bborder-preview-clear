// 管理后台专用请求封装（独立 token，与微信登录态隔离）
const BASE_URL = 'https://YOUR_BACKEND_DOMAIN_OR_HOST'

const adminRequest = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('admin_token')
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data)
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('admin_token')
          uni.removeStorageSync('admin_user')
          uni.reLaunch({ url: '/pages/admin/login' })
          reject(new Error('登录已过期'))
        } else {
          reject(new Error((res.data && res.data.error) || '请求失败'))
        }
      },
      fail: (err) => { reject(new Error('网络错误: ' + err.errMsg)) }
    })
  })
}

export const adminLogin = (password) => adminRequest({ url: '/api/auth/admin/login', method: 'POST', data: { password } })
export const getUsers = () => adminRequest({ url: '/api/admin/users', method: 'GET' })
export const assignRole = (id, role) => adminRequest({ url: `/api/admin/users/${id}/role`, method: 'PUT', data: { role } })
export const unbindUser = (id) => adminRequest({ url: `/api/admin/users/${id}/unbind`, method: 'POST' })

export default adminRequest
