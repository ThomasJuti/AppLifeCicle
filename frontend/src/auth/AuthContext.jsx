import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { login as loginApi } from '../api/customers'
import { getToken, setToken } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setTokenState] = useState(() => getToken())
  const [username, setUsername] = useState(null)

  const logout = useCallback(() => {
    setToken(null)
    setTokenState(null)
    setUsername(null)
  }, [])

  useEffect(() => {
    window.addEventListener('auth:unauthorized', logout)
    return () => window.removeEventListener('auth:unauthorized', logout)
  }, [logout])

  const login = useCallback(async (user, password) => {
    const data = await loginApi(user, password)
    setToken(data.token)
    setTokenState(data.token)
    setUsername(data.username)
    return data
  }, [])

  const value = {
    token,
    username,
    isAuthenticated: Boolean(token),
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
