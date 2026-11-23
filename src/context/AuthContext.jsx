import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, ApiError, setUnauthorizedHandler, tokenStore } from '../api.js'

const AuthContext = createContext(null)
const KEY = 'odonto:auth'

/** Usuário criado pelo back-end na primeira subida (ADMIN_EMAIL / ADMIN_PASSWORD). */
export const DEMO_USER = { email: 'admin@odonto.com', senha: '123456' }

const readUser = () => {
  try {
    return tokenStore.get() ? JSON.parse(localStorage.getItem(KEY)) : null
  } catch {
    return null
  }
}

const saveUser = (u) => {
  try { localStorage.setItem(KEY, JSON.stringify(u)) } catch {}
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser)

  const logout = useCallback(() => {
    tokenStore.clear()
    try { localStorage.removeItem(KEY) } catch {}
    setUser(null)
  }, [])

  // token vencido em qualquer chamada → volta para o login
  useEffect(() => setUnauthorizedHandler(logout), [logout])

  /** true = entrou; false = e-mail/senha errados; lança erro se o servidor estiver fora. */
  const login = async (email, senha) => {
    try {
      const r = await api('/auth/login', { method: 'POST', body: { email: email.trim(), senha }, auth: false })
      tokenStore.set(r.token)
      saveUser(r.usuario)
      setUser(r.usuario)
      return true
    } catch (e) {
      if (e instanceof ApiError && (e.status === 401 || e.status === 400)) return false
      throw e
    }
  }

  const updateUser = async (data) => {
    const u = await api('/auth/me', { method: 'PUT', body: { nome: data.nome } })
    saveUser(u)
    setUser(u)
    return u
  }

  const isAdmin = user?.cargo === 'Administrador'

  return <AuthContext.Provider value={{ user, isAdmin, login, logout, updateUser }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
