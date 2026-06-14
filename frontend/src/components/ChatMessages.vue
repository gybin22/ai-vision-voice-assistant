<template>
  <div class="messages">
    <div
      v-for="message in messages"
      :key="message.id"
      class="message-row"
      :class="message.role"
    >
      <div class="avatar">
        {{ message.role === 'user' ? '你' : message.role === 'assistant' ? 'VL' : '!' }}
      </div>

      <div class="message-bubble">
        <div>{{ message.content }}</div>
        <div v-if="message.meta" class="message-meta">{{ message.meta }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ChatMessage } from '@/types/chat'

defineProps<{
  messages: ChatMessage[]
}>()
</script>
