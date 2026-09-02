import { get, post, put } from './request'

export const loginByRole = (role) => post('/api/auth/login', { role })
export const loginByCode = (code) => post('/api/auth/login', { code })
export const getMe = () => get('/api/auth/me')
export const updateBackground = (backgroundUrl) => put('/api/auth/me/background', { backgroundUrl })
