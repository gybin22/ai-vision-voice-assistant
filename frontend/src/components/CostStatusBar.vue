<template>
  <div class="status-bar">
    <span class="badge">Session {{ shortSessionId }}</span>
    <span v-if="usage" class="badge">今日 {{ usage.requestCount }}/{{ usage.requestLimit }}</span>
    <span v-if="usage" class="badge">剩余 {{ usage.remainingRequests }}</span>
    <span v-if="usage" class="badge">成本 {{ usage.estimatedCost.toFixed(4) }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SessionUsage } from '@/types/chat'

const props = defineProps<{
  sessionId: string
  usage: SessionUsage | null
}>()

const shortSessionId = computed(() => `${props.sessionId.slice(0, 8)}...`)
</script>
