import { computed, ref } from 'vue'
import { canvasToBlob, calcTargetSize } from '@/utils/imageCompress'

export type RollingFrameRole = 'history' | 'current' | 'manual'

export interface RollingFrameItem {
  id: string
  blob: Blob
  url: string
  width: number
  height: number
  capturedAt: number
  offsetMs: number
  sequence: number
  role: RollingFrameRole
}

export interface PreparedVisionUpload {
  frames: RollingFrameItem[]
  visualSummary: string
  dispose: () => void
}

export interface RollingFrameBufferOptions {
  /** 固定采样间隔。默认 1000ms，也就是 1fps。 */
  sampleIntervalMs?: number
  /** 前端内存中最多保留多少张历史帧。 */
  maxStoredFrames?: number
  /** 每次提问上传多少张视觉帧。 */
  maxUploadFrames?: number
  /** 图片最长边。 */
  maxLongSide?: number
  /** JPEG/WebP 压缩质量。 */
  quality?: number
  /** 输出图片格式。 */
  mimeType?: 'image/jpeg' | 'image/webp'
}

const DEFAULT_OPTIONS: Required<RollingFrameBufferOptions> = {
  sampleIntervalMs: 1000,
  maxStoredFrames: 15,
  maxUploadFrames: 15,
  maxLongSide: 640,
  quality: 0.6,
  mimeType: 'image/jpeg'
}

export function useRollingFrameBuffer(options: RollingFrameBufferOptions = {}) {
  const config = { ...DEFAULT_OPTIONS, ...options }

  const frames = ref<RollingFrameItem[]>([])
  const isRunning = ref(false)
  const statusText = ref('未开始采样')
  const lastCapturedAt = ref<number | null>(null)

  let timer: number | undefined
  let videoRef: HTMLVideoElement | null = null
  let frameSequence = 0
  let processing = false

  const frameCount = computed(() => frames.value.length)
  const totalBytes = computed(() => frames.value.reduce((sum, frame) => sum + frame.blob.size, 0))
  const totalKb = computed(() => Math.round(totalBytes.value / 1024))
  const latestFrame = computed(() => frames.value.length ? frames.value[frames.value.length - 1] : null)
  const coverageSeconds = computed(() => {
    if (frames.value.length < 2) return 0
    const first = frames.value[0]
    const last = frames.value[frames.value.length - 1]
    return Math.max(0, Math.round((last.capturedAt - first.capturedAt) / 100) / 10)
  })

  async function start(video: HTMLVideoElement) {
    stop()
    ensureVideoReady(video)
    videoRef = video
    isRunning.value = true
    statusText.value = '正在以 1fps 保存最近 15 秒视觉帧'

    await captureSample('history')
    timer = window.setInterval(() => {
      void captureSample('history')
    }, config.sampleIntervalMs)
  }

  function stop() {
    if (timer !== undefined) {
      window.clearInterval(timer)
      timer = undefined
    }
    videoRef = null
    isRunning.value = false
    processing = false
    statusText.value = '采样已停止'
  }

  function clear() {
    disposeFrames(frames.value)
    frames.value = []
    frameSequence = 0
    lastCapturedAt.value = null
    statusText.value = isRunning.value ? '正在以 1fps 保存最近 15 秒视觉帧' : '未开始采样'
  }

  async function captureSample(role: RollingFrameRole = 'history') {
    if (!videoRef || processing) return
    processing = true
    try {
      const frame = await captureFrame(videoRef, role)
      pushFrame(frame)
      lastCapturedAt.value = frame.capturedAt
      statusText.value = `已保存最近 ${frames.value.length}/${config.maxStoredFrames} 帧，覆盖约 ${coverageSeconds.value}s`
    } finally {
      processing = false
    }
  }

  async function forceSaveCurrent(video?: HTMLVideoElement) {
    const target = video ?? videoRef
    if (!target) return
    ensureVideoReady(target)
    const frame = await captureFrame(target, 'manual')
    pushFrame(frame)
    lastCapturedAt.value = frame.capturedAt
    statusText.value = '已手动补充一张当前采样帧'
  }

  async function prepareUpload(video: HTMLVideoElement): Promise<PreparedVisionUpload> {
    ensureVideoReady(video)

    const currentFrame = await captureFrame(video, 'current')
    const historyFrames = frames.value
      .slice()
      .sort((a, b) => a.capturedAt - b.capturedAt)
      .slice(-(config.maxUploadFrames - 1))

    const selected = [...historyFrames, currentFrame]
      .sort((a, b) => a.capturedAt - b.capturedAt)
      .slice(-config.maxUploadFrames)

    const latestAt = selected[selected.length - 1]?.capturedAt ?? Date.now()
    const normalized = selected.map((frame, index) => ({
      ...frame,
      sequence: index + 1,
      offsetMs: Math.round(frame.capturedAt - latestAt),
      role: frame.id === currentFrame.id ? 'current' as RollingFrameRole : frame.role === 'manual' ? 'manual' as RollingFrameRole : 'history' as RollingFrameRole
    }))

    const first = normalized[0]
    const last = normalized[normalized.length - 1]
    const coverage = first && last ? Math.max(0, Math.round((last.capturedAt - first.capturedAt) / 100) / 10) : 0
    const visualSummary = [
      `本轮始终上传最近视觉上下文：共 ${normalized.length} 张图片。`,
      `这些图片按时间从早到晚排列，来自最近约 ${coverage}s 摄像头画面，采样频率约 1fps。`,
      `最后一张是用户点击发送时补采的当前帧，offsetMs=0。`,
      `如果用户问题需要当前状态，请优先参考最后一张；如果用户问刚才发生了什么，请按第 1 张到第 ${normalized.length} 张理解变化。`,
      `如果用户问题不需要视觉信息，请忽略图片并正常回答。`
    ].join('\n')

    return {
      frames: normalized,
      visualSummary,
      dispose: () => disposeFrames([currentFrame])
    }
  }

  function pushFrame(frame: RollingFrameItem) {
    frames.value.push(frame)
    while (frames.value.length > config.maxStoredFrames) {
      const removed = frames.value.shift()
      if (removed) URL.revokeObjectURL(removed.url)
    }
  }

  async function captureFrame(video: HTMLVideoElement, role: RollingFrameRole): Promise<RollingFrameItem> {
    ensureVideoReady(video)

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

    return {
      id: crypto.randomUUID(),
      blob,
      url: URL.createObjectURL(blob),
      width: target.width,
      height: target.height,
      capturedAt: Date.now(),
      offsetMs: 0,
      sequence: ++frameSequence,
      role
    }
  }

  function disposeFrames(targetFrames: RollingFrameItem[]) {
    for (const frame of targetFrames) {
      URL.revokeObjectURL(frame.url)
    }
  }

  function ensureVideoReady(video: HTMLVideoElement) {
    if (!video.videoWidth || !video.videoHeight) {
      throw new Error('摄像头画面尚未准备好')
    }
  }

  return {
    frames,
    frameCount,
    totalBytes,
    totalKb,
    latestFrame,
    coverageSeconds,
    isRunning,
    statusText,
    lastCapturedAt,
    start,
    stop,
    clear,
    captureSample,
    forceSaveCurrent,
    prepareUpload
  }
}
