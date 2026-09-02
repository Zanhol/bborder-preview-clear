import { get, post } from './request'

export const getCurrentCook = () => get('/api/cook/current')
export const switchCook = (who) => post('/api/cook/switch', { who })
