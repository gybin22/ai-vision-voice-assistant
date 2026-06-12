const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, init)

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

  return response.json() as Promise<T>
}
