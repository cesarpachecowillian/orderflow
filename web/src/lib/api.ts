import axios, { AxiosError } from 'axios'
import { authStorage } from './auth'
import type { ApiError } from '../types'

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = authStorage.getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (resp) => resp,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401 && authStorage.getToken()) {
      authStorage.clear()
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export function extractApiMessage(err: unknown, fallback = 'Erro inesperado'): string {
  const ax = err as AxiosError<ApiError>
  return ax.response?.data?.message ?? ax.message ?? fallback
}
