import { request } from './apiClient'
import type { InputType, QuestionMode, SessionUsage, VisionChatResponse } from '@/types/chat'

export interface AskVisionImage {
  blob: Blob
  width: number
  height: number
  capturedAt: number
  offsetMs: number
  sequence: number
  role: string
}

export interface AskVisionParams {
  sessionId: string
  question: string
  questionMode: QuestionMode
  visualSummary: string
  images: AskVisionImage[]
  inputType: InputType
  enableHistory: boolean
  maxOutputTokens: number
}

export async function askVision(params: AskVisionParams): Promise<VisionChatResponse> {
  const formData = new FormData()
  formData.append('sessionId', params.sessionId)
  formData.append('question', params.question)
  formData.append('questionMode', params.questionMode)
  formData.append('visualSummary', params.visualSummary)
  formData.append('inputType', params.inputType)
  formData.append('enableHistory', String(params.enableHistory))
  formData.append('maxOutputTokens', String(params.maxOutputTokens))

  formData.append(
    'frameMetadata',
    JSON.stringify(
      params.images.map(image => ({
        sequence: image.sequence,
        role: image.role,
        capturedAt: image.capturedAt,
        offsetMs: image.offsetMs,
        width: image.width,
        height: image.height,
        size: image.blob.size
      }))
    )
  )

  params.images.forEach(image => {
    const sequence = String(image.sequence).padStart(3, '0')
    const role = image.role || 'frame'
    formData.append('images', image.blob, `rolling-${sequence}-${role}.jpg`)
  })

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
