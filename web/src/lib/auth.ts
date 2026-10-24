const TOKEN_KEY = 'orderflow.token'
const NAME_KEY = 'orderflow.userName'
const EMAIL_KEY = 'orderflow.userEmail'

export const authStorage = {
  getToken: () => localStorage.getItem(TOKEN_KEY),
  getUserName: () => localStorage.getItem(NAME_KEY),
  getUserEmail: () => localStorage.getItem(EMAIL_KEY),
  save: (token: string, name: string, email: string) => {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(NAME_KEY, name)
    localStorage.setItem(EMAIL_KEY, email)
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(NAME_KEY)
    localStorage.removeItem(EMAIL_KEY)
  },
}

export function decodeJwtPayload(token: string): { sub?: string; name?: string; role?: string; exp?: number } | null {
  try {
    const part = token.split('.')[1]
    const json = atob(part.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decodeURIComponent(escape(json)))
  } catch {
    return null
  }
}
