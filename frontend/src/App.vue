<template>
  <AuthView v-if="!isAuthenticated" />
  <ProfileView v-else-if="currentView === 'profile'" @back="currentView = 'assistant'" />
  <AssistantView v-else @open-profile="currentView = 'profile'" />
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import AssistantView from './views/AssistantView.vue'
import AuthView from './views/AuthView.vue'
import ProfileView from './views/ProfileView.vue'
import { useAuth } from '@/composables/useAuth'

const auth = useAuth()
const isAuthenticated = auth.isAuthenticated
const currentView = ref<'assistant' | 'profile'>('assistant')

onMounted(() => {
  auth.refreshProfile()
})

watch(isAuthenticated, value => {
  if (!value) {
    currentView.value = 'assistant'
  }
})
</script>
