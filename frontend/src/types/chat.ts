export type InputType = 'text' | 'voice'

//判断用户问题属于哪种类型
export type QuestionMode = 'chat' | 'current' | 'motion' | 'detailed'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'error'
  content: string
  meta?: string
}

export interface VisionChatResponse {
  requestId: string
  sessionId: string
  answer: string
  model: string
  cached: boolean
  usage: {
    inputTokens: number
    outputTokens: number
    imageBytes: number
    estimatedCost: number
  }
  latencyMs: number
}

export interface SessionUsage {
  sessionId: string
  requestCount: number
  requestLimit: number
  estimatedCost: number
  remainingRequests: number
}

export interface ApiErrorResponse {
  requestId?: string
  code: string
  message: string
}
