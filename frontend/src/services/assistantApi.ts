import { request } from './apiClient'
import type { InputType, SessionUsage, VisionChatResponse } from '@/types/chat'

export interface AskVisionImage {
  blob: Blob
  width: number
  height: number
  capturedAt: number
  diffScore: number
  sequence: number
}

export interface AskVisionParams {
  sessionId: string
  question: string
  images: AskVisionImage[]
  inputType: InputType
  enableHistory: boolean
  maxOutputTokens: number
}

export async function askVision(params: AskVisionParams): Promise<VisionChatResponse> {
  const formData = new FormData()
  formData.append('sessionId', params.sessionId)
  formData.append('question', params.question)
  formData.append('inputType', params.inputType)
  formData.append('enableHistory', String(params.enableHistory))
  formData.append('maxOutputTokens', String(params.maxOutputTokens))
  formData.append(
    'frameMetadata',
    JSON.stringify(
      params.images.map(image => ({
        sequence: image.sequence,
        capturedAt: image.capturedAt,
        width: image.width,
        height: image.height,
        diffScore: Number(image.diffScore.toFixed(4)),
        size: image.blob.size
      }))
    )
  )

  params.images.forEach((image, index) => {
    const sequence = String(image.sequence).padStart(3, '0')
    formData.append('images', image.blob, `keyframe-${sequence}-${index + 1}.jpg`)
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
