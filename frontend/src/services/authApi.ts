import { request } from './apiClient'
import type { AuthResponse, AuthUser, LoginPayload, RegisterPayload, UpdateProfilePayload } from '@/types/chat'

export function register(payload: RegisterPayload): Promise<AuthResponse> {
  return request<AuthResponse>('/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export function login(payload: LoginPayload): Promise<AuthResponse> {
  return request<AuthResponse>('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export function logout(): Promise<{ loggedOut: boolean }> {
  return request<{ loggedOut: boolean }>('/auth/logout', { method: 'POST' })
}

export function getProfile(): Promise<AuthUser> {
  return request<AuthUser>('/users/me')
}

export function updateProfile(payload: UpdateProfilePayload): Promise<AuthUser> {
  return request<AuthUser>('/users/me', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}
