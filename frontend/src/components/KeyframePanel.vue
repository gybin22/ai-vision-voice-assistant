<template>
  <section class="keyframe-panel">
    <div class="keyframe-panel-header">
      <div>
        <span class="eyebrow">Vision Events</span>
        <h3>视觉事件缓冲区</h3>
      </div>
      <div class="keyframe-stats">
        <span>{{ eventCount }} 事件</span>
        <span>{{ frameCount }} 帧</span>
        <span>{{ totalKb }} KB</span>
      </div>
    </div>

    <div class="keyframe-status">
      <span class="status-dot" :class="{ active: isRunning }"></span>
      <span>{{ statusText }}</span>
      <span class="diff-score">差异 {{ diffPercent }}%</span>
    </div>

    <div class="upload-strategy">
      <span>当前问题模式：{{ modeLabel }}</span>
      <span>{{ uploadHint }}</span>
    </div>

    <div v-if="events.length" class="event-list">
      <article v-for="event in events" :key="event.id" class="vision-event-card">
        <div class="vision-event-header">
          <span>事件 #{{ event.sequence }}</span>
          <span>{{ event.status === 'open' ? '进行中' : '已结束' }}</span>
          <span>{{ formatDuration(event.durationMs) }}</span>
        </div>

        <div class="keyframe-strip compact">
          <article v-for="frame in event.frames" :key="frame.id" class="keyframe-thumb">
            <img :src="frame.url" alt="视觉事件代表帧" />
            <div class="keyframe-meta">
              <span>{{ frameKindLabel(frame.kind) }}</span>
              <span>{{ Math.round(frame.diffScore * 100) }}%</span>
            </div>
          </article>
        </div>
      </article>
    </div>

    <div v-else class="keyframe-empty">
      系统会把连续画面变化合并成“视觉事件”。普通聊天不会上传图片；问当前画面时只传当前帧；问刚才发生了什么时才发送事件代表帧。
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { KeyframeKind, VisionEventItem } from '@/composables/useKeyframeRecorder'
import type { QuestionMode } from '@/types/chat'
import { questionModeLabel, questionModeUploadHint } from '@/utils/questionMode'

const props = defineProps<{
  events: VisionEventItem[]
  isRunning: boolean
  statusText: string
  lastDiffScore: number
  totalBytes: number
  questionMode: QuestionMode
}>()

const eventCount = computed(() => props.events.length)
const frameCount = computed(() => props.events.reduce((sum, event) => sum + event.frames.length, 0))
const totalKb = computed(() => Math.round(props.totalBytes / 1024))
const diffPercent = computed(() => Math.round(props.lastDiffScore * 100))
const modeLabel = computed(() => questionModeLabel(props.questionMode))
const uploadHint = computed(() => questionModeUploadHint(props.questionMode))

function formatDuration(durationMs: number) {
  return `${(durationMs / 1000).toFixed(1)}s`
}

function frameKindLabel(kind: KeyframeKind) {
  switch (kind) {
    case 'start':
      return '开始'
    case 'peak':
      return '峰值'
    case 'end':
      return '结束'
    case 'current':
      return '当前'
    case 'manual':
      return '手动'
    default:
      return '帧'
  }
}
</script>
