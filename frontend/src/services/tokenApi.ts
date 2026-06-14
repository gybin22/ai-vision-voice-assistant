import { request } from './apiClient'
import type { TokenBalance, TokenRechargeResponse } from '@/types/chat'

export function getTokenBalance(): Promise<TokenBalance> {
  return request<TokenBalance>('/tokens/balance')
}

export function rechargeTokens(amountTokens: number): Promise<TokenRechargeResponse> {
  return request<TokenRechargeResponse>('/tokens/recharge', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ amountTokens })
  })
}
