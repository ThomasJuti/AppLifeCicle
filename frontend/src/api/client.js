import axios from 'axios'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8081'

export const TOKEN_KEY = 'customers.jwt'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && getToken()) {
      setToken(null)
      window.dispatchEvent(new Event('auth:unauthorized'))
    }
    return Promise.reject(error)
  },
)

export function apiErrorMessage(error, fallback = 'Ocurrió un error inesperado') {
  const data = error?.response?.data
  if (!data) return error?.message || fallback
  if (data.errors && typeof data.errors === 'object') {
    return Object.values(data.errors).join(' · ')
  }
  return data.detail || data.title || fallback
}

export default api
