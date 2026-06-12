import { computed, ref } from 'vue'
import { canvasToBlob, calcTargetSize } from '@/utils/imageCompress'

export interface KeyframeItem {
  id: string
  blob: Blob
  url: string
  width: number
  height: number
  capturedAt: number
  diffScore: number
  sequence: number
}

export interface KeyframeRecorderOptions {
  /** 帧差检测间隔，单位 ms */
  intervalMs?: number
  /** 平均灰度差异阈值。0.08 表示平均像素差异约 8%。 */
  diffThreshold?: number
  /** 变化区域占比阈值。用于避免光照轻微波动触发。 */
  changedRatioThreshold?: number
  /** 保存关键帧的最小间隔，单位 ms */
  minSaveIntervalMs?: number
  /** 每轮最多保留关键帧数量 */
  maxFrames?: number
  /** 上传关键帧最长边 */
  maxLongSide?: number
  /** JPEG/WebP 压缩质量 */
  quality?: number
  /** 输出图片格式 */
  mimeType?: 'image/jpeg' | 'image/webp'
  /** 是否在开始检测时保存第一帧，建议开启，给模型动作基准画面 */
  captureInitialFrame?: boolean
}

interface ProcessOptions {
  forceSave?: boolean
  saveIfEmpty?: boolean
  reason?: string
}

const DEFAULT_OPTIONS: Required<KeyframeRecorderOptions> = {
  intervalMs: 850,
  diffThreshold: 0.075,
  changedRatioThreshold: 0.06,
  minSaveIntervalMs: 1800,
  maxFrames: 8,
  maxLongSide: 768,
  quality: 0.72,
  mimeType: 'image/jpeg',
  captureInitialFrame: true
}

const ANALYSIS_WIDTH = 72
const ANALYSIS_HEIGHT = 40
const PIXEL_CHANGE_THRESHOLD = 30

export function useKeyframeRecorder(options: KeyframeRecorderOptions = {}) {
  const config = { ...DEFAULT_OPTIONS, ...options }

  const keyframes = ref<KeyframeItem[]>([])
  const isRunning = ref(false)
  const lastDiffScore = ref(0)
  const lastChangedRatio = ref(0)
  const lastSavedAt = ref(0)
  const statusText = ref('未开始检测')

  let timer: number | undefined
  let videoRef: HTMLVideoElement | null = null
  let previousSignature: Uint8ClampedArray | null = null
  let sequence = 0
  let processing = false

  const frameCount = computed(() => keyframes.value.length)
  const totalBytes = computed(() => keyframes.value.reduce((sum, item) => sum + item.blob.size, 0))
  const latestKeyframe = computed(() => keyframes.value.length ? keyframes.value[keyframes.value.length - 1] : null)

  async function start(video: HTMLVideoElement) {
    stop()
    ensureVideoReady(video)
    videoRef = video
    isRunning.value = true
    statusText.value = '正在检测画面变化'

    previousSignature = null
    if (config.captureInitialFrame) {
      await processFrame(video, { forceSave: true, reason: '初始画面' })
    } else {
      previousSignature = readSignature(video)
    }

    timer = window.setInterval(() => {
      if (!videoRef || processing) return
      void processFrame(videoRef, { reason: '变化检测' })
    }, config.intervalMs)
  }

  function stop() {
    if (timer !== undefined) {
      window.clearInterval(timer)
      timer = undefined
    }
    isRunning.value = false
    videoRef = null
    processing = false
    statusText.value = '检测已停止'
  }

  async function forceCheck(video?: HTMLVideoElement, processOptions: ProcessOptions = {}) {
    const target = video ?? videoRef
    if (!target) return
    ensureVideoReady(target)
    await processFrame(target, {
      saveIfEmpty: true,
      reason: '发送前检测',
      ...processOptions
    })
  }

  async function forceSaveCurrent(video?: HTMLVideoElement, reason = '手动保存') {
    const target = video ?? videoRef
    if (!target) return
    ensureVideoReady(target)
    await processFrame(target, { forceSave: true, reason })
  }

  function getFramesForUpload(): KeyframeItem[] {
    return [...keyframes.value]
  }

  function clear(options: { resetBaseline?: boolean } = {}) {
    for (const item of keyframes.value) {
      URL.revokeObjectURL(item.url)
    }
    keyframes.value = []
    lastSavedAt.value = 0
    statusText.value = isRunning.value ? '正在检测画面变化' : '未开始检测'

    if (options.resetBaseline) {
      previousSignature = null
    }
  }

  async function processFrame(video: HTMLVideoElement, processOptions: ProcessOptions = {}) {
    if (processing) return
    processing = true

    try {
      const currentSignature = readSignature(video)
      let diffScore = 0
      let changedRatio = 0

      if (previousSignature) {
        const diff = compareSignature(previousSignature, currentSignature)
        diffScore = diff.diffScore
        changedRatio = diff.changedRatio
      } else {
        diffScore = 1
        changedRatio = 1
      }

      previousSignature = currentSignature
      lastDiffScore.value = diffScore
      lastChangedRatio.value = changedRatio

      const now = Date.now()
      const enoughInterval = now - lastSavedAt.value >= config.minSaveIntervalMs
      const isSignificantChange =
        diffScore >= config.diffThreshold && changedRatio >= config.changedRatioThreshold
      const shouldSave =
        processOptions.forceSave ||
        (processOptions.saveIfEmpty && keyframes.value.length === 0) ||
        (isSignificantChange && enoughInterval)

      if (shouldSave) {
        await saveKeyframe(video, diffScore)
        statusText.value = processOptions.reason
          ? `${processOptions.reason}，已保存关键帧`
          : '检测到明显变化，已保存关键帧'
      } else if (isSignificantChange && !enoughInterval) {
        statusText.value = '检测到变化，但距离上一帧过近，跳过保存'
      } else {
        statusText.value = '画面变化不明显，未保存'
      }
    } finally {
      processing = false
    }
  }

  async function saveKeyframe(video: HTMLVideoElement, diffScore: number) {
    const sourceWidth = video.videoWidth
    const sourceHeight = video.videoHeight
    const target = calcTargetSize(sourceWidth, sourceHeight, config.maxLongSide)

    const canvas = document.createElement('canvas')
    canvas.width = target.width
    canvas.height = target.height

    const ctx = canvas.getContext('2d')
    if (!ctx) {
      throw new Error('当前浏览器无法创建 Canvas 上下文')
    }

    ctx.drawImage(video, 0, 0, target.width, target.height)
    const blob = await canvasToBlob(canvas, {
      maxLongSide: config.maxLongSide,
      quality: config.quality,
      mimeType: config.mimeType
    })

    const item: KeyframeItem = {
      id: crypto.randomUUID(),
      blob,
      url: URL.createObjectURL(blob),
      width: target.width,
      height: target.height,
      capturedAt: Date.now(),
      diffScore,
      sequence: ++sequence
    }

    keyframes.value.push(item)
    lastSavedAt.value = item.capturedAt

    while (keyframes.value.length > config.maxFrames) {
      const removed = keyframes.value.shift()
      if (removed) URL.revokeObjectURL(removed.url)
    }
  }

  function readSignature(video: HTMLVideoElement): Uint8ClampedArray {
    ensureVideoReady(video)

    const canvas = document.createElement('canvas')
    canvas.width = ANALYSIS_WIDTH
    canvas.height = ANALYSIS_HEIGHT

    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    if (!ctx) {
      throw new Error('当前浏览器无法创建 Canvas 上下文')
    }

    ctx.drawImage(video, 0, 0, ANALYSIS_WIDTH, ANALYSIS_HEIGHT)
    const { data } = ctx.getImageData(0, 0, ANALYSIS_WIDTH, ANALYSIS_HEIGHT)
    const gray = new Uint8ClampedArray(ANALYSIS_WIDTH * ANALYSIS_HEIGHT)

    for (let i = 0, j = 0; i < data.length; i += 4, j += 1) {
      gray[j] = Math.round(data[i] * 0.299 + data[i + 1] * 0.587 + data[i + 2] * 0.114)
    }

    return gray
  }

  function compareSignature(previous: Uint8ClampedArray, current: Uint8ClampedArray) {
    let totalDiff = 0
    let changedPixels = 0

    for (let i = 0; i < current.length; i += 1) {
      const diff = Math.abs(current[i] - previous[i])
      totalDiff += diff
      if (diff >= PIXEL_CHANGE_THRESHOLD) {
        changedPixels += 1
      }
    }

    return {
      diffScore: totalDiff / current.length / 255,
      changedRatio: changedPixels / current.length
    }
  }

  function ensureVideoReady(video: HTMLVideoElement) {
    if (!video.videoWidth || !video.videoHeight) {
      throw new Error('摄像头画面尚未准备好')
    }
  }

  return {
    keyframes,
    frameCount,
    totalBytes,
    latestKeyframe,
    isRunning,
    lastDiffScore,
    lastChangedRatio,
    statusText,
    start,
    stop,
    clear,
    forceCheck,
    forceSaveCurrent,
    getFramesForUpload
  }
}
