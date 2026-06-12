<template>
  <div class="messages">
    <div
      v-for="message in messages"
      :key="message.id"
      class="message"
      :class="message.role"
    >
      <div>{{ message.content }}</div>
      <div v-if="message.meta" class="meta">{{ message.meta }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ChatMessage } from '@/types/chat'

defineProps<{
  messages: ChatMessage[]
}>()
</script>
