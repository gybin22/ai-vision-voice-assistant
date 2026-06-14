<template>
  <main class="auth-page">
    <section class="auth-card">
      <div class="auth-brand">
        <div class="brand-mark">VL</div>
        <div>
          <h1>VoxLens AI</h1>
        </div>
      </div>

      <div class="auth-tabs" role="tablist">
        <button :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
        <button :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
      </div>

      <form class="auth-form" @submit.prevent="submit">
        <label v-if="mode === 'register'">
          <span>昵称</span>
          <input v-model.trim="nickname" maxlength="80" autocomplete="nickname" placeholder="可选，例如 Michael" />
        </label>

        <label>
          <span>邮箱</span>
          <input v-model.trim="email" type="email" autocomplete="email" placeholder="you@example.com" required />
        </label>

        <label>
          <span>密码</span>
          <input
            v-model="password"
            type="password"
            :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            placeholder="至少 8 位"
            required
            minlength="8"
            maxlength="72"
          />
        </label>

        <div v-if="auth.error.value || localError" class="notice notice-warning">
          {{ auth.error.value || localError }}
        </div>

        <button class="auth-submit" :disabled="auth.loading.value || !canSubmit">
          {{ auth.loading.value ? '处理中...' : mode === 'login' ? '登录' : '注册并登录' }}
        </button>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'

const auth = useAuth()
const mode = ref<'login' | 'register'>('login')
const email = ref('')
const password = ref('')
const nickname = ref('')
const localError = ref('')

const canSubmit = computed(() => {
  return email.value.includes('@') && password.value.length >= 8
})

function switchMode(nextMode: 'login' | 'register') {
  mode.value = nextMode
  localError.value = ''
}

async function submit() {
  localError.value = ''
  if (!canSubmit.value) {
    localError.value = '请填写有效邮箱和至少 8 位密码。'
    return
  }

  if (mode.value === 'login') {
    await auth.login({ email: email.value, password: password.value })
    return
  }

  await auth.register({
    email: email.value,
    password: password.value,
    nickname: nickname.value || undefined
  })
}
</script>
