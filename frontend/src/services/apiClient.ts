import { clearAuthState, getAccessToken, getRefreshToken, updateTokens } from './authStorage'
import type { AuthResponse } from '@/types/chat'

const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
let refreshPromise: Promise<boolean> | null = null

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  return requestInternal<T>(path, init, true)
}

async function requestInternal<T>(path: string, init: RequestInit = {}, allowRefresh: boolean): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, withAuthHeader(init))

  if (response.status === 401 && allowRefresh && !path.startsWith('/auth/')) {
    const refreshed = await refreshAccessToken()
    if (refreshed) {
      return requestInternal<T>(path, init, false)
    }
  }

  if (!response.ok) {
    let message = `请求失败：HTTP ${response.status}`
    try {
      const body = await response.json()
      message = body.message || body.code || message
    } catch {
      // ignore JSON parse failure
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

function withAuthHeader(init: RequestInit): RequestInit {
  const headers = new Headers(init.headers)
  const token = getAccessToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  return { ...init, headers }
}

async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    clearAuthState()
    return false
  }

  if (!refreshPromise) {
    refreshPromise = fetch(`${baseUrl}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    })
      .then(async response => {
        if (!response.ok) {
          clearAuthState()
          return false
        }
        const auth = (await response.json()) as AuthResponse
        updateTokens(auth.accessToken, auth.refreshToken, auth.user)
        return true
      })
      .catch(() => {
        clearAuthState()
        return false
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}
