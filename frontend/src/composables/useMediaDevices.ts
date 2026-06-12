import { ref } from 'vue'

export function useMediaDevices() {
  const stream = ref<MediaStream | null>(null)
  const isStarting = ref(false)
  const error = ref('')

  async function start() {
    error.value = ''
    isStarting.value = true
    try {
      if (!navigator.mediaDevices?.getUserMedia) {
        throw new Error('当前浏览器不支持摄像头/麦克风访问')
      }

      stream.value = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 1280 },
          height: { ideal: 720 },
          facingMode: { ideal: 'environment' }
        },
        audio: true
      })
    } catch (e) {
      error.value = e instanceof Error ? e.message : '启动摄像头或麦克风失败'
      throw e
    } finally {
      isStarting.value = false
    }
  }

  function stop() {
    stream.value?.getTracks().forEach(track => track.stop())
    stream.value = null
  }

  return {
    stream,
    isStarting,
    error,
    start,
    stop
  }
}
