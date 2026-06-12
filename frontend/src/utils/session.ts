const SESSION_KEY = 'ai-vision-voice-session-id'

export function getOrCreateSessionId(): string {
  const existing = localStorage.getItem(SESSION_KEY)
  if (existing) return existing

  const sessionId = crypto.randomUUID ? crypto.randomUUID() : fallbackUuid()
  localStorage.setItem(SESSION_KEY, sessionId)
  return sessionId
}

function fallbackUuid(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}
