// Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0
import { get, post } from './request'

export const getCurrentCook = () => get('/api/cook/current')
export const switchCook = (who) => post('/api/cook/switch', { who })
export const setCookStatus = (status) => post('/api/cook/status', { status })
