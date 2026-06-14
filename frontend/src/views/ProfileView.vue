<template>
  <main class="profile-page">
    <section class="profile-card">
      <div class="profile-header">
        <div>
          <span class="eyebrow">Account</span>
          <h1>个人中心</h1>
          <p>管理账号资料和平台 Tokens。当前支付流程为开发版模拟确认，正式上线时替换为真实支付回调。</p>
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
        <div class="token-card-actions">
          <button class="auth-submit compact" :disabled="tokens.loading.value" @click="openRechargePackages">
            充值
          </button>
          <button class="link-button" :disabled="tokens.loading.value" @click="refreshBalance">
            {{ tokens.loading.value ? '刷新中...' : '刷新余额' }}
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

    <Teleport to="body">
      <div v-if="showPackageModal" class="modal-backdrop" @click.self="closeRechargeFlow">
        <section class="modal-card recharge-modal-card">
          <div class="modal-header">
            <div>
              <span class="eyebrow">Recharge</span>
              <h2>选择充值包</h2>
              <p>选择一个套餐后进入确认支付。当前为模拟支付，不会发起真实扣款。</p>
            </div>
            <button class="modal-close" aria-label="关闭" @click="closeRechargeFlow">×</button>
          </div>

          <div class="recharge-grid">
            <button
              v-for="item in rechargePackages"
              :key="item.tokens"
              class="recharge-card"
              :disabled="tokens.loading.value"
              @click="selectPackage(item)"
            >
              <span class="recharge-price">{{ item.priceLabel }}</span>
              <strong>{{ formatTokens(item.tokens) }} Tokens</strong>
              <span>{{ item.label }}</span>
              <small>{{ item.description }}</small>
            </button>
          </div>
        </section>
      </div>

      <div v-if="showConfirmModal && selectedPackage" class="modal-backdrop" @click.self="backToPackages">
        <section class="modal-card confirm-modal-card">
          <div class="modal-header">
            <div>
              <span class="eyebrow">Confirm</span>
              <h2>确认支付</h2>
              <p>请确认充值套餐。当前环境点击确认后直接模拟到账。</p>
            </div>
            <button class="modal-close" aria-label="关闭" @click="closeRechargeFlow">×</button>
          </div>

          <div class="confirm-payment-box">
            <div>
              <span>套餐</span>
              <strong>{{ selectedPackage.label }}</strong>
            </div>
            <div>
              <span>到账 Tokens</span>
              <strong>{{ formatTokens(selectedPackage.tokens) }}</strong>
            </div>
            <div>
              <span>支付金额</span>
              <strong>{{ selectedPackage.priceLabel }}</strong>
            </div>
          </div>

          <div class="modal-actions">
            <button class="link-button" :disabled="tokens.loading.value" @click="backToPackages">重新选择</button>
            <button class="auth-submit" :disabled="tokens.loading.value" @click="confirmRecharge">
              {{ tokens.loading.value ? '确认中...' : '确认支付（模拟）' }}
            </button>
          </div>
        </section>
      </div>
    </Teleport>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useTokens } from '@/composables/useTokens'

interface RechargePackage {
  tokens: number
  priceLabel: string
  label: string
  description: string
}

const emit = defineEmits<{ (e: 'back'): void }>()
const auth = useAuth()
const tokens = useTokens()
const nickname = ref(auth.user.value?.nickname ?? '')
const avatarUrl = ref(auth.user.value?.avatarUrl ?? '')
const savedText = ref('')
const showPackageModal = ref(false)
const showConfirmModal = ref(false)
const selectedPackage = ref<RechargePackage | null>(null)

const rechargePackages: RechargePackage[] = [
  { tokens: 50_000, priceLabel: '¥ 6.00', label: '体验包', description: '适合轻度体验视频对话' },
  { tokens: 200_000, priceLabel: '¥ 19.90', label: '标准包', description: '适合日常使用' },
  { tokens: 500_000, priceLabel: '¥ 49.90', label: '高频包', description: '适合高频视频对话' }
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

function openRechargePackages() {
  savedText.value = ''
  selectedPackage.value = null
  showConfirmModal.value = false
  showPackageModal.value = true
}

function selectPackage(item: RechargePackage) {
  selectedPackage.value = item
  showPackageModal.value = false
  showConfirmModal.value = true
}

function backToPackages() {
  showConfirmModal.value = false
  showPackageModal.value = true
}

function closeRechargeFlow() {
  showPackageModal.value = false
  showConfirmModal.value = false
  selectedPackage.value = null
}

async function confirmRecharge() {
  if (!selectedPackage.value) return

  savedText.value = ''
  const packageToRecharge = selectedPackage.value
  try {
    const response = await tokens.recharge(packageToRecharge.tokens)
    savedText.value = `已模拟支付 ${packageToRecharge.priceLabel}，到账 ${formatTokens(response.addedTokens)} Tokens。`
    closeRechargeFlow()
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
