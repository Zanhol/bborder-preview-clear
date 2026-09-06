// Copyright 2026 深圳市甜梦屋科技有限公司 · Licensed under Apache-2.0
import { get, post, put, del } from './request'

export const createOrder = (dishes) => post('/api/orders', { dishes })

export const getOrders = () => get('/api/orders')

export const markReceived = (orderId) => put(`/api/orders/${orderId}/received`)

export const deleteOrder = (orderId) => del(`/api/orders/${orderId}`)
