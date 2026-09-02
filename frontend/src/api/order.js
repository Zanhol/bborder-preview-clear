import { get, post, put, del } from './request'

export const createOrder = (dishes) => post('/api/orders', { dishes })

export const getOrders = () => get('/api/orders')

export const markReceived = (orderId) => put(`/api/orders/${orderId}/received`)

export const deleteOrder = (orderId) => del(`/api/orders/${orderId}`)
