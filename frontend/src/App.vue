<template>
  <AuthView v-if="!isAuthenticated" />
  <ProfileView v-else-if="currentView === 'profile'" @back="currentView = 'assistant'" @open-history="currentView = 'history'" />
  <HistoryView v-else-if="currentView === 'history'" @back="currentView = 'profile'" />
  <AssistantView v-else @open-profile="currentView = 'profile'" />
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import AssistantView from './views/AssistantView.vue'
import AuthView from './views/AuthView.vue'
import ProfileView from './views/ProfileView.vue'
import HistoryView from './views/HistoryView.vue'
import { useAuth } from '@/composables/useAuth'

const auth = useAuth()
const isAuthenticated = auth.isAuthenticated
const currentView = ref<'assistant' | 'profile' | 'history'>('assistant')

onMounted(() => {
  auth.refreshProfile()
})

watch(isAuthenticated, value => {
  if (!value) {
    currentView.value = 'assistant'
  }
})
</script>
