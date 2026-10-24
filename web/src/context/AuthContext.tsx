import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api } from '../lib/api'
import { authStorage, decodeJwtPayload } from '../lib/auth'
import type { AuthResponse } from '../types'

interface AuthState {
  token: string | null
  userName: string | null
  userEmail: string | null
  role: string | null
}

interface AuthCtx extends AuthState {
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthCtx | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(() => {
    const token = authStorage.getToken()
    const claims = token ? decodeJwtPayload(token) : null
    return {
      token,
      userName: authStorage.getUserName(),
      userEmail: authStorage.getUserEmail(),
      role: claims?.role ?? null,
    }
  })

  useEffect(() => {
    const token = state.token
    if (!token) return
    const claims = decodeJwtPayload(token)
    if (claims?.exp && claims.exp * 1000 < Date.now()) {
      authStorage.clear()
      setState({ token: null, userName: null, userEmail: null, role: null })
    }
  }, [state.token])

  const login = async (email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/auth/login', { email, password })
    const claims = decodeJwtPayload(data.token)
    const name = claims?.name ?? email
    const role = claims?.role ?? 'USER'
    authStorage.save(data.token, name, email)
    setState({ token: data.token, userName: name, userEmail: email, role })
  }

  const register = async (name: string, email: string, password: string) => {
    await api.post('/auth/register', { name, email, password })
    await login(email, password)
  }

  const logout = () => {
    authStorage.clear()
    setState({ token: null, userName: null, userEmail: null, role: null })
  }

  return (
    <AuthContext.Provider value={{ ...state, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth deve estar dentro de AuthProvider')
  return ctx
}
