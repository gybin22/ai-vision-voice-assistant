<template>
  <div class="status-bar">
    <span class="badge">Session：{{ shortSessionId }}</span>
    <span v-if="usage" class="badge">今日请求：{{ usage.requestCount }}/{{ usage.requestLimit }}</span>
    <span v-if="usage" class="badge">剩余：{{ usage.remainingRequests }}</span>
    <span v-if="usage" class="badge">估算成本：{{ usage.estimatedCost.toFixed(4) }}</span>
    <span class="badge">图片最长边：768px</span>
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
