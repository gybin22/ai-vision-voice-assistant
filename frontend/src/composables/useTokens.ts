import { computed, ref } from 'vue'
import * as tokenApi from '@/services/tokenApi'
import type { TokenBalance } from '@/types/chat'

const balance = ref<TokenBalance | null>(null)
const loading = ref(false)
const error = ref('')

export function useTokens() {
  const balanceTokens = computed(() => balance.value?.balanceTokens ?? 0)

  async function refreshBalance() {
    error.value = ''
    loading.value = true
    try {
      balance.value = await tokenApi.getTokenBalance()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Tokens 余额查询失败。'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function recharge(amountTokens: number) {
    error.value = ''
    loading.value = true
    try {
      const response = await tokenApi.rechargeTokens(amountTokens)
      balance.value = response.balance
      return response
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Tokens 充值失败。'
      throw e
    } finally {
      loading.value = false
    }
  }

  function applyBalanceAfter(balanceAfterTokens: number) {
    const current = balance.value
    if (!current) {
      balance.value = {
        balanceTokens: balanceAfterTokens,
        totalRechargedTokens: 0,
        totalUsedTokens: 0
      }
      return
    }
    balance.value = {
      ...current,
      balanceTokens: balanceAfterTokens
    }
  }

  function clearBalance() {
    balance.value = null
    error.value = ''
  }

  return {
    balance,
    balanceTokens,
    loading,
    error,
    refreshBalance,
    recharge,
    applyBalanceAfter,
    clearBalance
  }
}
