<template>
  <div class="video-wrap">
    <video
      v-if="stream"
      ref="videoEl"
      autoplay
      muted
      playsinline
      @loadedmetadata="handleVideoReady"
      @canplay="handleVideoReady"
    />

    <div v-else class="video-placeholder">
      <div>
        <strong>摄像头未启动</strong>
        <p>点击“启动摄像头和麦克风”后，系统会在你提问时截取当前画面。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'

const props = defineProps<{
  stream: MediaStream | null
}>()

const videoEl = ref<HTMLVideoElement | null>(null)
const videoReady = ref(false)

watch(
  () => props.stream,
  async stream => {
    videoReady.value = false
    await nextTick()

    if (!stream || !videoEl.value) {
      return
    }

    videoEl.value.srcObject = stream

    try {
      await videoEl.value.play()
    } catch {
      // 部分移动端浏览器可能要求用户手势。这里不直接报错，等待 canplay/loadedmetadata。
    }
  },
  { immediate: true }
)

function handleVideoReady() {
  const video = videoEl.value
  videoReady.value = Boolean(video && video.videoWidth > 0 && video.videoHeight > 0)
}

function getVideoElement(): HTMLVideoElement | null {
  return videoEl.value
}

function isReady(): boolean {
  const video = videoEl.value
  return Boolean(videoReady.value && video && video.videoWidth > 0 && video.videoHeight > 0)
}

defineExpose({
  getVideoElement,
  isReady
})
</script>
