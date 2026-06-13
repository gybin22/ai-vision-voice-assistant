<template>
  <main class="profile-page">
    <section class="profile-card">
      <div class="profile-header">
        <div>
          <span class="eyebrow">Account</span>
          <h1>个人中心</h1>
          <p>当前只保存基础账号资料；充值点数和请求数控制后续可以接在这个用户体系上。</p>
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

      <form class="auth-form" @submit.prevent="save">
        <label>
          <span>昵称</span>
          <input v-model.trim="nickname" maxlength="80" placeholder="昵称" />
        </label>

        <label>
          <span>头像 URL</span>
          <input v-model.trim="avatarUrl" maxlength="512" placeholder="https://..." />
        </label>

        <div v-if="auth.error.value || savedText" :class="savedText ? 'notice notice-info' : 'notice notice-warning'">
          {{ auth.error.value || savedText }}
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
import { computed, ref, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'

const emit = defineEmits<{ (e: 'back'): void }>()
const auth = useAuth()
const nickname = ref(auth.user.value?.nickname ?? '')
const avatarUrl = ref(auth.user.value?.avatarUrl ?? '')
const savedText = ref('')

const initials = computed(() => {
  const source = auth.user.value?.nickname || auth.user.value?.email || 'U'
  return source.slice(0, 1).toUpperCase()
})

watch(auth.user, user => {
  nickname.value = user?.nickname ?? ''
  avatarUrl.value = user?.avatarUrl ?? ''
})

async function save() {
  savedText.value = ''
  await auth.updateProfile({
    nickname: nickname.value || undefined,
    avatarUrl: avatarUrl.value || undefined
  })
  savedText.value = '资料已保存。'
}

async function logout() {
  await auth.logout()
}
</script>
