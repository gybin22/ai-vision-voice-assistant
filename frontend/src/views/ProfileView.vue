<template>
  <main class="profile-page profile-page-single">
    <section class="profile-card profile-shell">
      <div class="profile-header profile-topbar">
        <div>
          <span class="eyebrow">Account Center</span>
          <h1>个人中心</h1>
          <p class="profile-subtitle">管理账号资料、头像、Tokens 余额与会话入口。</p>
        </div>
        <button type="button" class="link-button profile-back-button" @click="emit('back')">返回对话</button>
      </div>

      <div v-if="auth.user.value" class="profile-stack">
        <form class="profile-section identity-section identity-form" @submit.prevent="save">
          <div class="identity-card-heading">
            <span class="eyebrow">Profile</span>
          </div>

          <div class="identity-main-row">
            <div class="avatar-block">
              <div class="profile-avatar identity-avatar">
                <img v-if="avatarUrl" :src="avatarUrl" alt="头像预览" />
                <span v-else>{{ initials }}</span>
              </div>
              <input
                ref="avatarInputRef"
                class="sr-only-file"
                type="file"
                accept="image/png,image/jpeg,image/webp,image/gif,image/*"
                @change="onAvatarFileChange"
              />
            </div>

            <div class="identity-content">
              <div class="identity-title-row">
                <h2>{{ auth.user.value.nickname }}</h2>
                <span class="status-pill active">{{ auth.user.value.status === 'ACTIVE' ? '正常' : '已禁用' }}</span>
              </div>

              <p class="identity-email">{{ auth.user.value.email }}</p>
              <p class="identity-hint">头像和昵称用于视频语音对话中的身份展示。选择本地头像或修改昵称后，点击“保存资料”统一生效。</p>

              <div class="identity-actions identity-action-grid">
                <button type="button" class="auth-submit compact" @click="openNameEditor">更改名称</button>
                <button type="button" class="auth-submit compact" @click="avatarInputRef?.click()">更改头像</button>
                <button v-if="avatarUrl" type="button" class="link-button" @click="clearAvatar">移除头像</button>
                <button type="submit" class="auth-submit compact save-profile-button" :disabled="auth.loading.value">
                  {{ auth.loading.value ? '保存中...' : '保存资料' }}
                </button>
              </div>
            </div>
          </div>
        </form>

        <section class="profile-section token-section">
          <div class="section-heading-flat token-heading">
            <div>
              <span class="eyebrow">AI Tokens</span>
              <h2>账户余额</h2>
            </div>
            <span class="token-refresh-state">{{ tokens.loading.value ? '余额同步中' : '余额已同步' }}</span>
          </div>

          <div class="token-balance-panel">
            <span>当前可用 Tokens</span>
            <strong>{{ formatTokens(tokens.balanceTokens.value) }}</strong>
            <p>用于实时语音、视觉关键帧分析和多模态回答。</p>
          </div>

          <div class="token-metrics-row">
            <div class="token-metric-card">
              <span>累计充值</span>
              <strong>{{ formatTokens(tokens.balance.value?.totalRechargedTokens ?? 0) }}</strong>
            </div>
            <div class="token-metric-card">
              <span>累计消耗</span>
              <strong>{{ formatTokens(tokens.balance.value?.totalUsedTokens ?? 0) }}</strong>
            </div>
          </div>

          <div class="token-primary-actions">
            <button type="button" class="auth-submit compact" :disabled="tokens.loading.value" @click="openRechargePackages">
              充值
            </button>
            <button type="button" class="link-button" :disabled="tokens.loading.value" @click="refreshBalance">
              {{ tokens.loading.value ? '刷新中...' : '刷新余额' }}
            </button>
          </div>
        </section>

        <section class="profile-section history-section">
          <div>
            <span class="eyebrow">Conversation</span>
            <h2>会话历史</h2>
            <p>查看过往视频语音对话、关键帧记录和 AI 回复内容。</p>
          </div>
          <button type="button" class="link-button history-button" @click="emit('open-history')">
            打开会话历史
          </button>
        </section>

        <div v-if="auth.error.value || tokens.error.value || savedText" :class="savedText ? 'notice notice-info' : 'notice notice-warning'">
          {{ auth.error.value || tokens.error.value || savedText }}
        </div>

        <section class="profile-section security-section">
          <div>
            <span class="eyebrow">Security</span>
            <h2>账号操作</h2>
            <p>退出当前登录状态，不会删除账户数据。</p>
          </div>
          <button type="button" class="danger-button" @click="logout">退出登录</button>
        </section>
      </div>

      <div v-else class="notice notice-warning">当前未获取到账户信息，请重新登录后再试。</div>
    </section>

    <Teleport to="body">
      <div v-if="showPackageModal" class="modal-backdrop" @click.self="closeRechargeFlow">
        <section class="modal-card recharge-modal-card">
          <div class="modal-header">
            <div>
              <span class="eyebrow">Recharge</span>
              <h2>选择充值包</h2>
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

      <div v-if="showNameModal" class="modal-backdrop" @click.self="closeNameEditor">
        <section class="modal-card name-modal-card">
          <div class="modal-header">
            <div>
              <span class="eyebrow">Profile</span>
              <h2>更改名称</h2>
              <p>新的名称会显示在个人中心和视频语音对话身份信息中。</p>
            </div>
            <button class="modal-close" aria-label="关闭" @click="closeNameEditor">×</button>
          </div>

          <label class="profile-field name-modal-field">
            <span>新名称</span>
            <input v-model.trim="draftNickname" maxlength="80" placeholder="请输入新的名称" @keyup.enter="confirmNameChange" />
          </label>

          <div class="modal-actions">
            <button class="link-button" :disabled="auth.loading.value" @click="closeNameEditor">取消</button>
            <button class="auth-submit" :disabled="auth.loading.value" @click="confirmNameChange">
              {{ auth.loading.value ? '保存中...' : '确认更改' }}
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

const emit = defineEmits<{ (e: 'back'): void; (e: 'open-history'): void }>()
const auth = useAuth()
const tokens = useTokens()
const nickname = ref(auth.user.value?.nickname ?? '')
const draftNickname = ref('')
const avatarUrl = ref(auth.user.value?.avatarUrl ?? '')
const avatarInputRef = ref<HTMLInputElement | null>(null)
const savedText = ref('')
const showPackageModal = ref(false)
const showConfirmModal = ref(false)
const showNameModal = ref(false)
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

function openNameEditor() {
  savedText.value = ''
  auth.error.value = ''
  draftNickname.value = nickname.value
  showNameModal.value = true
}

function closeNameEditor() {
  showNameModal.value = false
  draftNickname.value = nickname.value
}

async function confirmNameChange() {
  const nextName = draftNickname.value.trim()
  if (!nextName) {
    auth.error.value = '名称不能为空。'
    return
  }

  nickname.value = nextName
  savedText.value = ''
  await auth.updateProfile({
    nickname: nickname.value,
    avatarUrl: avatarUrl.value || ''
  })
  showNameModal.value = false
  savedText.value = '名称已更新。'
}

async function save() {
  savedText.value = ''
  await auth.updateProfile({
    nickname: nickname.value || undefined,
    avatarUrl: avatarUrl.value || ''
  })
  savedText.value = '资料已保存。'
}

async function onAvatarFileChange(event: Event) {
  savedText.value = ''
  auth.error.value = ''
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    auth.error.value = '请选择图片文件。'
    input.value = ''
    return
  }

  try {
    avatarUrl.value = await compressAvatarFile(file)
    savedText.value = '头像已选择，点击“保存资料”后生效。'
  } catch (error) {
    auth.error.value = error instanceof Error ? error.message : '头像处理失败。'
  } finally {
    input.value = ''
  }
}

function clearAvatar() {
  avatarUrl.value = ''
  savedText.value = '头像已移除，点击“保存资料”后生效。'
}

function compressAvatarFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('读取头像文件失败。'))
    reader.onload = () => {
      const image = new Image()
      image.onerror = () => reject(new Error('头像图片无法解析。'))
      image.onload = () => {
        const size = 256
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('当前浏览器不支持头像压缩。'))
          return
        }

        const sourceSize = Math.min(image.width, image.height)
        const sourceX = Math.max(0, (image.width - sourceSize) / 2)
        const sourceY = Math.max(0, (image.height - sourceSize) / 2)
        canvas.width = size
        canvas.height = size
        ctx.drawImage(image, sourceX, sourceY, sourceSize, sourceSize, 0, 0, size, size)

        resolve(canvas.toDataURL('image/jpeg', 0.82))
      }
      image.src = String(reader.result)
    }
    reader.readAsDataURL(file)
  })
}

async function logout() {
  tokens.clearBalance()
  await auth.logout()
}

function formatTokens(value: number) {
  return Math.floor(value).toLocaleString('zh-CN')
}
</script>



<style scoped>
.profile-page-single {
  width: 100%;
}

.profile-shell {
  max-width: 880px;
  margin: 0 auto;
  padding: 22px;
}

.profile-topbar {
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.profile-subtitle {
  margin: 8px 0 0;
  color: rgba(226, 232, 240, 0.68);
  line-height: 1.7;
}

.profile-back-button {
  flex: 0 0 auto;
}

.profile-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.profile-section {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.055);
  box-shadow: 0 18px 52px rgba(0, 0, 0, 0.16);
  padding: 18px;
}

.identity-section {
  display: block;
}

.identity-card-heading {
  text-align: left;
  margin-bottom: 12px;
}

.identity-main-row {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar-block {
  flex: 0 0 auto;
}

.identity-avatar {
  width: 96px;
  height: 96px;
  font-size: 2rem;
  box-shadow: 0 14px 36px rgba(96, 165, 250, 0.18);
}

.identity-content {
  min-width: 0;
  flex: 1;
}

.identity-title-row,
.section-heading-flat,
.security-section,
.history-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.identity-title-row {
  justify-content: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.identity-title-row h2,
.section-heading-flat h2,
.history-section h2,
.security-section h2 {
  margin: 0;
  font-size: 1.18rem;
}

.section-heading-flat h2,
.history-section h2,
.security-section h2 {
  margin-top: 6px;
}

.identity-email {
  margin: 10px 0 0;
  color: rgba(226, 232, 240, 0.72);
  word-break: break-all;
}

.identity-hint,
.history-section p,
.security-section p {
  margin: 6px 0 0;
  color: rgba(226, 232, 240, 0.62);
  line-height: 1.65;
}

.identity-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.sr-only-file {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.token-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.token-heading {
  align-items: flex-start;
}

.token-refresh-state {
  flex: 0 0 auto;
  border: 1px solid rgba(96, 165, 250, 0.22);
  border-radius: 999px;
  padding: 7px 11px;
  background: rgba(96, 165, 250, 0.09);
  color: rgba(191, 219, 254, 0.92);
  font-size: 0.78rem;
  white-space: nowrap;
}

.token-balance-panel {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  padding: 16px 18px;
  background: #ffffff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.12);
}

.token-balance-panel span {
  display: block;
  color: rgba(15, 23, 42, 0.62);
  font-size: 0.86rem;
}

.token-balance-panel strong {
  display: block;
  margin-top: 6px;
  color: #020617;
  font-size: clamp(2rem, 6vw, 3.6rem);
  letter-spacing: -0.07em;
  line-height: 1;
}

.token-balance-panel p {
  margin: 8px 0 0;
  color: rgba(15, 23, 42, 0.58);
  line-height: 1.45;
}

.token-metrics-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.token-metric-card {
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.045);
}

.token-metric-card span {
  display: block;
  color: rgba(226, 232, 240, 0.56);
  font-size: 0.86rem;
}

.token-metric-card strong {
  display: block;
  margin-top: 6px;
  color: rgba(248, 250, 252, 0.94);
  font-size: 1.08rem;
}

.token-primary-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.history-button,
.security-section .danger-button {
  flex: 0 0 auto;
}

.profile-field {
  display: grid;
  gap: 9px;
}

.profile-field span {
  color: rgba(226, 232, 240, 0.74);
  font-size: 0.92rem;
}

.name-modal-field {
  margin-top: 16px;
}

.identity-action-grid {
  align-items: center;
}

.save-profile-button {
  margin-left: auto;
}

@media (max-width: 700px) {
  .profile-shell {
    padding: 20px;
  }

  .profile-topbar,
  .identity-main-row,
  .identity-title-row,
  .section-heading-flat,
  .history-section,
  .security-section {
    align-items: flex-start;
    flex-direction: column;
  }

  .identity-card-heading,
  .identity-section {
    text-align: left;
  }

  .token-metrics-row {
    grid-template-columns: 1fr;
  }

  .identity-actions,
  .token-primary-actions,
  .history-button,
  .security-section .danger-button,
  .profile-back-button {
    width: 100%;
  }

  .save-profile-button {
    margin-left: 0;
  }
}
</style>
