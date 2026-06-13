export type InputType = 'text' | 'voice'

// 当前不再做问题分类；所有提问统一携带最近 15 秒滚动视觉上下文。
export type QuestionMode = 'rolling' | 'chat' | 'current' | 'motion' | 'detailed'

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
