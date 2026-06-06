import api from './client'

export const login = (username, password) =>
  api.post('/api/auth/login', { username, password }).then((r) => r.data)

export const listCustomers = () =>
  api.get('/api/customers').then((r) => r.data)

export const createCustomer = (payload) =>
  api.post('/api/customers', payload).then((r) => r.data)

export const updateCustomer = (id, payload) =>
  api.put(`/api/customers/${id}`, payload).then((r) => r.data)

export const deleteCustomer = (id) =>
  api.delete(`/api/customers/${id}`).then((r) => r.data)
