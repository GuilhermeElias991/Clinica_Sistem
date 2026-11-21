// Cliente HTTP do back-end (Spring Boot). Em desenvolvimento o Vite faz proxy de /api → http://localhost:8080.
// Para apontar para outro servidor: VITE_API_URL=https://minha-api/api npm run dev

const BASE = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '')
const TOKEN_KEY = 'odonto:token'

export const tokenStore = {
  get: () => {
    try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
  },
  set: (t) => {
    try { localStorage.setItem(TOKEN_KEY, t) } catch {}
  },
  clear: () => {
    try { localStorage.removeItem(TOKEN_KEY) } catch {}
  },
}

let onUnauthorized = null
/** Chamado quando a API responde 401 (token vencido/inválido). */
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn }

export class ApiError extends Error {
  constructor(status, message, errors = {}) {
    super(message)
    this.status = status
    this.errors = errors || {}
  }
}

/** Mensagem amigável para qualquer erro (de rede ou da API). */
export const errMsg = (e) => e?.message || 'Erro ao comunicar com o servidor'

export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = tokenStore.get()
  if (auth && token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch(BASE + path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined })
  } catch {
    throw new ApiError(0, 'Servidor indisponível. Verifique se o back-end está rodando.')
  }

  if (res.status === 204) return null
  const text = await res.text()
  let data = null
  try { data = text ? JSON.parse(text) : null } catch { data = null }

  if (!res.ok) {
    if (res.status === 401 && auth && onUnauthorized) onUnauthorized()
    const msg = data?.message || (res.status === 401 ? 'Sessão expirada. Entre novamente.' : `Erro ${res.status}`)
    throw new ApiError(res.status, msg, data?.errors)
  }
  return data
}
