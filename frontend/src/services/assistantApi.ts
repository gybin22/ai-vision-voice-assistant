import { request } from './apiClient'
import type { InputType, QuestionMode, SessionUsage, VisionChatResponse } from '@/types/chat'

export interface AskVisionImage {
  blob: Blob
  width: number
  height: number
  capturedAt: number
  diffScore: number
  changedRatio?: number
  sequence: number
  eventSequence?: number
  kind?: string
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

  if (params.images.length) {
    formData.append(
      'frameMetadata',
      JSON.stringify(
        params.images.map(image => ({
          sequence: image.sequence,
          eventSequence: image.eventSequence,
          kind: image.kind,
          capturedAt: image.capturedAt,
          width: image.width,
          height: image.height,
          diffScore: Number(image.diffScore.toFixed(4)),
          changedRatio: Number((image.changedRatio ?? 0).toFixed(4)),
          size: image.blob.size
        }))
      )
    )
  }

  params.images.forEach((image, index) => {
    const sequence = String(image.sequence).padStart(3, '0')
    const kind = image.kind ?? 'frame'
    formData.append('images', image.blob, `${params.questionMode}-${kind}-${sequence}-${index + 1}.jpg`)
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
