<template>
  <section class="keyframe-panel">
    <div class="keyframe-panel-header">
      <div>
        <span class="eyebrow">Rolling Frame Buffer</span>
        <h3>最近 15 秒视觉帧</h3>
      </div>
      <div class="keyframe-stats">
        <span>{{ frameCount }}/{{ maxFrames }} 帧</span>
        <span>{{ totalKb }} KB</span>
        <span>1 fps</span>
      </div>
    </div>

    <div class="keyframe-status">
      <span class="status-dot" :class="{ active: isRunning }"></span>
      <span>{{ statusText }}</span>
      <span class="diff-score">覆盖约 {{ coverageSeconds.toFixed(1) }}s</span>
    </div>

    <div class="upload-strategy">
      <span>固定策略：每次提问上传最近 {{ maxFrames }} 帧</span>
      <span>最后一帧为发送瞬间当前帧</span>
    </div>

    <div v-if="frames.length" class="keyframe-strip rolling-frame-strip">
      <article v-for="frame in frames" :key="frame.id" class="keyframe-thumb">
        <img :src="frame.url" alt="最近视觉采样帧" />
        <div class="keyframe-meta">
          <span>#{{ frame.sequence }}</span>
          <span>{{ frameRoleLabel(frame.role) }}</span>
        </div>
      </article>
    </div>

    <div v-else class="keyframe-empty">
      启动摄像头后，前端会每秒保存 1 张压缩帧，只在内存中保留最近 {{ maxFrames }} 张。发送问题时会补采一张当前帧，并把这些图片按时间顺序发给后端。
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RollingFrameItem, RollingFrameRole } from '@/composables/useRollingFrameBuffer'

const props = defineProps<{
  frames: RollingFrameItem[]
  isRunning: boolean
  statusText: string
  totalBytes: number
  coverageSeconds: number
  maxFrames: number
}>()

const frameCount = computed(() => props.frames.length)
const totalKb = computed(() => Math.round(props.totalBytes / 1024))

function frameRoleLabel(role: RollingFrameRole) {
  switch (role) {
    case 'current':
      return '当前'
    case 'manual':
      return '手动'
    default:
      return '历史'
  }
}
</script>
