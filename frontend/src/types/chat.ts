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
    totalTokens: number
    imageBytes: number
    providerCostAmountYuan: number
  }
  billing: {
    chargedTokens: number
    balanceAfterTokens: number
    tokenUnitPriceYuan: number
    revenueAmountYuan: number
    providerCostAmountYuan: number
    grossProfitAmountYuan: number
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

export interface AuthUser {
  id: number
  email: string
  nickname: string
  avatarUrl?: string | null
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
  updatedAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
  refreshExpiresInSeconds: number
  user: AuthUser
}

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload {
  email: string
  password: string
  nickname?: string
}

export interface UpdateProfilePayload {
  nickname?: string
  avatarUrl?: string
}


export interface TokenBalance {
  balanceTokens: number
  totalRechargedTokens: number
  totalUsedTokens: number
}

export interface TokenRechargeResponse {
  balance: TokenBalance
  addedTokens: number
  transactionId: number
}
