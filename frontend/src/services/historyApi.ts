import { request } from './apiClient'
import type { ChatHistoryDay, ChatSessionDetail, ClearChatHistoryResponse } from '@/types/chat'

export function listChatHistory(): Promise<ChatHistoryDay[]> {
  return request<ChatHistoryDay[]>('/history')
}

export function getChatSessionDetail(sessionId: string): Promise<ChatSessionDetail> {
  return request<ChatSessionDetail>(`/history/sessions/${encodeURIComponent(sessionId)}`)
}

export function clearChatHistory(): Promise<ClearChatHistoryResponse> {
  return request<ClearChatHistoryResponse>('/history', { method: 'DELETE' })
}

export function deleteChatSession(sessionId: string): Promise<ClearChatHistoryResponse> {
  return request<ClearChatHistoryResponse>(`/history/sessions/${encodeURIComponent(sessionId)}`, { method: 'DELETE' })
}
