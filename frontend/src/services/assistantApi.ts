import { request } from './apiClient'
import type { InputType, SessionUsage, VisionChatResponse } from '@/types/chat'

export interface AskVisionParams {
  sessionId: string
  question: string
  image: Blob
  inputType: InputType
  enableHistory: boolean
  maxOutputTokens: number
  clientImageWidth: number
  clientImageHeight: number
}

export async function askVision(params: AskVisionParams): Promise<VisionChatResponse> {
  const formData = new FormData()
  formData.append('sessionId', params.sessionId)
  formData.append('question', params.question)
  formData.append('inputType', params.inputType)
  formData.append('enableHistory', String(params.enableHistory))
  formData.append('maxOutputTokens', String(params.maxOutputTokens))
  formData.append('clientImageWidth', String(params.clientImageWidth))
  formData.append('clientImageHeight', String(params.clientImageHeight))
  formData.append('image', params.image, 'frame.jpg')

  return request<VisionChatResponse>('/chat/vision', {
    method: 'POST',
    body: formData
  })
}

export async function getSessionUsage(sessionId: string): Promise<SessionUsage> {
  return request<SessionUsage>(`/usage/session/${encodeURIComponent(sessionId)}`)
}

export async function clearConversation(sessionId: string): Promise<{ sessionId: string; deleted: boolean }> {
  return request(`/conversation/${encodeURIComponent(sessionId)}`, { method: 'DELETE' })
}
