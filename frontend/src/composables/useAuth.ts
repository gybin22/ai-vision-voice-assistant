import { computed, ref } from 'vue'
import * as authApi from '@/services/authApi'
import { clearAuthState, getStoredAuthState, saveAuthResponse, saveUser } from '@/services/authStorage'
import type { AuthUser, LoginPayload, RegisterPayload, UpdateProfilePayload } from '@/types/chat'

const stored = typeof window !== 'undefined' ? getStoredAuthState() : null
const user = ref<AuthUser | null>(stored?.user ?? null)
const loading = ref(false)
const error = ref('')

export function useAuth() {
  const isAuthenticated = computed(() => Boolean(user.value))

  async function register(payload: RegisterPayload) {
    error.value = ''
    loading.value = true
    try {
      const response = await authApi.register(payload)
      saveAuthResponse(response)
      user.value = response.user
    } catch (e) {
      error.value = e instanceof Error ? e.message : '注册失败。'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function login(payload: LoginPayload) {
    error.value = ''
    loading.value = true
    try {
      const response = await authApi.login(payload)
      saveAuthResponse(response)
      user.value = response.user
    } catch (e) {
      error.value = e instanceof Error ? e.message : '登录失败。'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // 本地仍然退出，避免用户卡在失效 token 状态。
    } finally {
      clearAuthState()
      user.value = null
    }
  }

  async function refreshProfile() {
    if (!getStoredAuthState()) return
    try {
      const profile = await authApi.getProfile()
      user.value = profile
      saveUser(profile)
    } catch {
      clearAuthState()
      user.value = null
    }
  }

  async function updateProfile(payload: UpdateProfilePayload) {
    error.value = ''
    loading.value = true
    try {
      const profile = await authApi.updateProfile(payload)
      user.value = profile
      saveUser(profile)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '保存失败。'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    user,
    loading,
    error,
    isAuthenticated,
    register,
    login,
    logout,
    refreshProfile,
    updateProfile
  }
}
