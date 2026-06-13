import type { AuthResponse, AuthUser } from '@/types/chat'

const STORAGE_KEY = 'ai-vision-auth-state'

export interface StoredAuthState {
  accessToken: string
  refreshToken: string
  user: AuthUser
}

export function getStoredAuthState(): StoredAuthState | null {
  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as StoredAuthState
    if (!parsed.accessToken || !parsed.refreshToken || !parsed.user) return null
    return parsed
  } catch {
    clearAuthState()
    return null
  }
}

export function saveAuthResponse(response: AuthResponse) {
  const state: StoredAuthState = {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    user: response.user
  }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

export function saveUser(user: AuthUser) {
  const state = getStoredAuthState()
  if (!state) return
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...state, user }))
}

export function updateTokens(accessToken: string, refreshToken: string, user: AuthUser) {
  window.localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({ accessToken, refreshToken, user } satisfies StoredAuthState)
  )
}

export function getAccessToken(): string | null {
  return getStoredAuthState()?.accessToken ?? null
}

export function getRefreshToken(): string | null {
  return getStoredAuthState()?.refreshToken ?? null
}

export function clearAuthState() {
  window.localStorage.removeItem(STORAGE_KEY)
}
