import request from './request'

export const registerRider = (data) => request.post('/riders', data)
export const loginRider = (data) => request.post('/rider/auth/login', data)
export const listAvailableOrders = () => request.get('/rider/available-orders')
export const claimOrder = (id) => request.post(`/rider/orders/${id}/claim`)
export const listRiderOrders = () => request.get('/rider/orders')
export const getRiderOrder = (id) => request.get(`/rider/orders/${id}`)
export const dispatchOrder = (id) => request.post(`/rider/orders/${id}/dispatch`)
export const completeOrder = (id) => request.post(`/rider/orders/${id}/complete`)
