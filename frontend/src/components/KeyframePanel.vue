<template>
  <section class="keyframe-panel">
    <div class="keyframe-panel-header">
      <div>
        <span class="eyebrow">Keyframes</span>
        <h3>动作关键帧</h3>
      </div>
      <div class="keyframe-stats">
        <span>{{ frameCount }} 帧</span>
        <span>{{ totalKb }} KB</span>
      </div>
    </div>

    <div class="keyframe-status">
      <span class="status-dot" :class="{ active: isRunning }"></span>
      <span>{{ statusText }}</span>
      <span class="diff-score">差异 {{ diffPercent }}%</span>
    </div>

    <div v-if="keyframes.length" class="keyframe-strip">
      <article v-for="frame in keyframes" :key="frame.id" class="keyframe-thumb">
        <img :src="frame.url" alt="动作关键帧" />
        <div class="keyframe-meta">
          <span>#{{ frame.sequence }}</span>
          <span>{{ Math.round(frame.diffScore * 100) }}%</span>
        </div>
      </article>
    </div>

    <div v-else class="keyframe-empty">
      启动摄像头后会自动检测画面变化。明显动作会保存为关键帧，并在提问时按时间顺序发送给后端。
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { KeyframeItem } from '@/composables/useKeyframeRecorder'

const props = defineProps<{
  keyframes: KeyframeItem[]
  isRunning: boolean
  statusText: string
  lastDiffScore: number
  totalBytes: number
}>()

const frameCount = computed(() => props.keyframes.length)
const totalKb = computed(() => Math.round(props.totalBytes / 1024))
const diffPercent = computed(() => Math.round(props.lastDiffScore * 100))
</script>
