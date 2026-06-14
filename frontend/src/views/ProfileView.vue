<template>
  <main class="profile-page">
    <section class="profile-card">
      <div class="profile-header">
        <div>
          <span class="eyebrow">Account</span>
          <h1>个人中心</h1>
          <p>管理账号资料和平台 Tokens。当前充值为开发版模拟充值，正式上线应替换为支付回调确认充值。</p>
        </div>
        <button class="link-button" @click="emit('back')">返回对话</button>
      </div>

      <div v-if="auth.user.value" class="profile-summary">
        <div class="profile-avatar">
          <img v-if="auth.user.value.avatarUrl" :src="auth.user.value.avatarUrl" alt="头像" />
          <span v-else>{{ initials }}</span>
        </div>
        <div>
          <h2>{{ auth.user.value.nickname }}</h2>
          <p>{{ auth.user.value.email }}</p>
          <span class="status-pill active">{{ auth.user.value.status === 'ACTIVE' ? '正常' : '已禁用' }}</span>
        </div>
      </div>

      <section class="token-card">
        <div>
          <span class="eyebrow">AI Tokens</span>
          <h2>{{ formatTokens(tokens.balanceTokens.value) }}</h2>
          <p>累计充值 {{ formatTokens(tokens.balance.value?.totalRechargedTokens ?? 0) }}，累计消耗 {{ formatTokens(tokens.balance.value?.totalUsedTokens ?? 0) }}。</p>
        </div>
        <button class="link-button" :disabled="tokens.loading.value" @click="refreshBalance">
          {{ tokens.loading.value ? '刷新中...' : '刷新余额' }}
        </button>
      </section>

      <section class="recharge-panel">
        <div class="profile-section-title">
          <h3>充值 Tokens</h3>
          <p>开发阶段直接模拟到账；正式接入支付时，保留账户和流水逻辑，只替换充值确认入口。</p>
        </div>
        <div class="recharge-grid">
          <button
            v-for="item in rechargePackages"
            :key="item.tokens"
            class="recharge-card"
            :disabled="tokens.loading.value"
            @click="recharge(item.tokens)"
          >
            <strong>{{ formatTokens(item.tokens) }} Tokens</strong>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </section>

      <form class="auth-form" @submit.prevent="save">
        <label>
          <span>昵称</span>
          <input v-model.trim="nickname" maxlength="80" placeholder="昵称" />
        </label>

        <label>
          <span>头像 URL</span>
          <input v-model.trim="avatarUrl" maxlength="512" placeholder="https://..." />
        </label>

        <div v-if="auth.error.value || tokens.error.value || savedText" :class="savedText ? 'notice notice-info' : 'notice notice-warning'">
          {{ auth.error.value || tokens.error.value || savedText }}
        </div>

        <div class="profile-actions">
          <button class="auth-submit" :disabled="auth.loading.value">
            {{ auth.loading.value ? '保存中...' : '保存资料' }}
          </button>
          <button type="button" class="danger-button" @click="logout">退出登录</button>
        </div>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useTokens } from '@/composables/useTokens'

const emit = defineEmits<{ (e: 'back'): void }>()
const auth = useAuth()
const tokens = useTokens()
const nickname = ref(auth.user.value?.nickname ?? '')
const avatarUrl = ref(auth.user.value?.avatarUrl ?? '')
const savedText = ref('')

const rechargePackages = [
  { tokens: 50_000, label: '体验包' },
  { tokens: 200_000, label: '标准包' },
  { tokens: 500_000, label: '高频使用' }
]

const initials = computed(() => {
  const source = auth.user.value?.nickname || auth.user.value?.email || 'U'
  return source.slice(0, 1).toUpperCase()
})

watch(auth.user, user => {
  nickname.value = user?.nickname ?? ''
  avatarUrl.value = user?.avatarUrl ?? ''
})

onMounted(() => {
  refreshBalance()
})

async function refreshBalance() {
  try {
    await tokens.refreshBalance()
  } catch {
    // 错误文案由 tokens.error 展示。
  }
}

async function recharge(amountTokens: number) {
  savedText.value = ''
  try {
    const response = await tokens.recharge(amountTokens)
    savedText.value = `已充值 ${formatTokens(response.addedTokens)} Tokens。`
  } catch {
    // 错误文案由 tokens.error 展示。
  }
}

async function save() {
  savedText.value = ''
  await auth.updateProfile({
    nickname: nickname.value || undefined,
    avatarUrl: avatarUrl.value || undefined
  })
  savedText.value = '资料已保存。'
}

async function logout() {
  tokens.clearBalance()
  await auth.logout()
}

function formatTokens(value: number) {
  return Math.floor(value).toLocaleString('zh-CN')
}
</script>
