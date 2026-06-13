import { computed, ref } from 'vue'
import { canvasToBlob, calcTargetSize } from '@/utils/imageCompress'
import type { QuestionMode } from '@/types/chat'

export type KeyframeKind = 'start' | 'peak' | 'end' | 'current' | 'manual'

export interface KeyframeItem {
  id: string
  blob: Blob
  url: string
  width: number
  height: number
  capturedAt: number
  diffScore: number
  changedRatio: number
  sequence: number
  eventSequence?: number
  kind: KeyframeKind
}

export interface VisionEventItem {
  id: string
  sequence: number
  startAt: number
  endAt: number
  lastChangedAt: number
  durationMs: number
  motionScore: number
  changedRatio: number
  status: 'open' | 'closed'
  frames: KeyframeItem[]
  summary: string
}

export interface PreparedVisionUpload {
  mode: QuestionMode
  frames: KeyframeItem[]
  visualSummary: string
  eventCount: number
  dispose: () => void
}

export interface KeyframeRecorderOptions {
  /** 帧差检测间隔，单位 ms */
  intervalMs?: number
  /** 平均灰度差异阈值。0.08 表示平均像素差异约 8%。 */
  diffThreshold?: number
  /** 变化区域占比阈值。用于避免光照轻微波动触发。 */
  changedRatioThreshold?: number
  /** 同一事件内更新峰值帧的最小间隔，单位 ms */
  minSaveIntervalMs?: number
  /** 画面稳定多久后关闭一个动作事件，单位 ms */
  eventQuietMs?: number
  /** 最多保留最近几个视觉事件 */
  maxEvents?: number
  /** 每次上传最多发送多少张图片 */
  maxUploadFrames?: number
  /** 上传图片最长边 */
  maxLongSide?: number
  /** JPEG/WebP 压缩质量 */
  quality?: number
  /** 输出图片格式 */
  mimeType?: 'image/jpeg' | 'image/webp'
}

interface ProcessOptions {
  reason?: string
}

const DEFAULT_OPTIONS: Required<KeyframeRecorderOptions> = {
  intervalMs: 850,
  diffThreshold: 0.075,
  changedRatioThreshold: 0.06,
  minSaveIntervalMs: 1500,
  eventQuietMs: 1400,
  maxEvents: 5,
  maxUploadFrames: 8,
  maxLongSide: 768,
  quality: 0.72,
  mimeType: 'image/jpeg'
}

const ANALYSIS_WIDTH = 72
const ANALYSIS_HEIGHT = 40
const PIXEL_CHANGE_THRESHOLD = 30

export function useKeyframeRecorder(options: KeyframeRecorderOptions = {}) {
  const config = { ...DEFAULT_OPTIONS, ...options }

  const events = ref<VisionEventItem[]>([])
  const activeEvent = ref<VisionEventItem | null>(null)
  const isRunning = ref(false)
  const lastDiffScore = ref(0)
  const lastChangedRatio = ref(0)
  const statusText = ref('未开始检测')

  let timer: number | undefined
  let videoRef: HTMLVideoElement | null = null
  let previousSignature: Uint8ClampedArray | null = null
  let frameSequence = 0
  let eventSequence = 0
  let processing = false
  let lastPeakSavedAt = 0

  const visionEvents = computed(() => {
    return activeEvent.value ? [...events.value, activeEvent.value] : [...events.value]
  })

  const keyframes = computed(() => visionEvents.value.flatMap(event => event.frames))
  const frameCount = computed(() => keyframes.value.length)
  const eventCount = computed(() => visionEvents.value.length)
  const totalBytes = computed(() => keyframes.value.reduce((sum, item) => sum + item.blob.size, 0))
  const latestKeyframe = computed(() => keyframes.value.length ? keyframes.value[keyframes.value.length - 1] : null)

  async function start(video: HTMLVideoElement) {
    stop()
    ensureVideoReady(video)
    videoRef = video
    isRunning.value = true
    statusText.value = '正在按动作事件检测画面变化'
    previousSignature = readSignature(video)

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

  async function prepareUpload(video: HTMLVideoElement, mode: QuestionMode): Promise<PreparedVisionUpload> {
    ensureVideoReady(video)

    if (mode === 'chat') {
      return {
        mode,
        frames: [],
        visualSummary: '本轮是普通聊天：没有上传图片，回答时不应主动使用视觉信息。',
        eventCount: visionEvents.value.length,
        dispose: () => {}
      }
    }

    if (mode === 'current') {
      const currentFrame = await captureFrame(video, 0, 0, 'current')
      return {
        mode,
        frames: [currentFrame],
        visualSummary: '本轮问题需要当前画面：只上传了发送瞬间的当前帧。请只回答用户问到的当前对象或状态，不要展开描述。',
        eventCount: visionEvents.value.length,
        dispose: () => disposeFrames([currentFrame])
      }
    }

    await processFrame(video, { reason: '发送前检测' })
    await closeActiveEvent(video, '发送前结束动作事件')

    const selectedEvents = selectRecentEvents(mode)
    let frames = selectedEvents.flatMap(event => selectRepresentativeFrames(event))
    frames = uniqueFrames(frames).slice(-config.maxUploadFrames)

    if (!frames.length) {
      const fallbackFrame = await captureFrame(video, 0, 0, 'current')
      return {
        mode,
        frames: [fallbackFrame],
        visualSummary: '本轮问题需要视觉信息，但最近没有形成完整动作事件；仅上传当前帧作为兜底。',
        eventCount: 0,
        dispose: () => disposeFrames([fallbackFrame])
      }
    }

    return {
      mode,
      frames,
      visualSummary: buildEventSummary(mode, selectedEvents, frames.length),
      eventCount: selectedEvents.length,
      dispose: () => {}
    }
  }

  async function forceCheck(video?: HTMLVideoElement) {
    const target = video ?? videoRef
    if (!target) return
    ensureVideoReady(target)
    await processFrame(target, { reason: '手动检测' })
  }

  async function forceSaveCurrent(video?: HTMLVideoElement, reason = '手动保存') {
    const target = video ?? videoRef
    if (!target) return
    ensureVideoReady(target)

    const frame = await captureFrame(target, 1, 1, 'manual')
    const event = createEvent(frame.capturedAt, 1, 1)
    event.frames.push(frame)
    event.status = 'closed'
    event.endAt = frame.capturedAt
    event.durationMs = 0
    event.summary = `手动保存了一张当前画面关键帧。原因：${reason}`
    events.value.push(event)
    trimEvents()
    statusText.value = '已手动保存当前关键帧'
  }

  function clear(options: { resetBaseline?: boolean } = {}) {
    for (const event of visionEvents.value) {
      disposeFrames(event.frames)
    }
    events.value = []
    activeEvent.value = null
    lastPeakSavedAt = 0
    statusText.value = isRunning.value ? '正在按动作事件检测画面变化' : '未开始检测'

    if (options.resetBaseline) {
      previousSignature = null
    }
  }

  async function processFrame(video: HTMLVideoElement, processOptions: ProcessOptions = {}) {
    if (processing) return
    processing = true

    try {
      const currentSignature = readSignature(video)
      let diffScore: number
      let changedRatio: number

      if (previousSignature) {
        const diff = compareSignature(previousSignature, currentSignature)
        diffScore = diff.diffScore
        changedRatio = diff.changedRatio
      } else {
        diffScore = 0
        changedRatio = 0
      }

      previousSignature = currentSignature
      lastDiffScore.value = diffScore
      lastChangedRatio.value = changedRatio

      const now = Date.now()
      const isSignificantChange = diffScore >= config.diffThreshold && changedRatio >= config.changedRatioThreshold

      if (isSignificantChange) {
        await handleMotionFrame(video, now, diffScore, changedRatio)
        statusText.value = processOptions.reason
          ? `${processOptions.reason}：检测到动作事件`
          : '检测到动作事件'
      } else if (activeEvent.value && now - activeEvent.value.lastChangedAt >= config.eventQuietMs) {
        await closeActiveEvent(video, '画面恢复稳定，动作事件已结束')
      } else {
        statusText.value = activeEvent.value ? '动作事件进行中，等待画面稳定' : '画面变化不明显，未创建事件'
      }
    } finally {
      processing = false
    }
  }

  async function handleMotionFrame(video: HTMLVideoElement, now: number, diffScore: number, changedRatio: number) {
    if (!activeEvent.value) {
      const startFrame = await captureFrame(video, diffScore, changedRatio, 'start')
      const event = createEvent(now, diffScore, changedRatio)
      event.frames.push(startFrame)
      event.summary = '检测到新的动作开始。'
      activeEvent.value = event
      lastPeakSavedAt = now
      return
    }

    const event = activeEvent.value
    event.lastChangedAt = now
    event.endAt = now
    event.durationMs = Math.max(0, now - event.startAt)
    event.changedRatio = Math.max(event.changedRatio, changedRatio)

    const enoughInterval = now - lastPeakSavedAt >= config.minSaveIntervalMs
    if (diffScore > event.motionScore && enoughInterval) {
      event.motionScore = diffScore
      const peakFrame = await captureFrame(video, diffScore, changedRatio, 'peak', event.sequence)
      removePreviousPeak(event)
      event.frames.push(peakFrame)
      lastPeakSavedAt = now
      event.summary = '动作变化达到峰值。'
    }
  }

  async function closeActiveEvent(video: HTMLVideoElement, reason: string) {
    const event = activeEvent.value
    if (!event) return

    const alreadyHasEnd = event.frames.some(frame => frame.kind === 'end')
    if (!alreadyHasEnd) {
      const endFrame = await captureFrame(video, lastDiffScore.value, lastChangedRatio.value, 'end', event.sequence)
      event.frames.push(endFrame)
    }

    const now = Date.now()
    event.status = 'closed'
    event.endAt = now
    event.durationMs = Math.max(0, now - event.startAt)
    event.summary = summarizeEvent(event)
    events.value.push(event)
    activeEvent.value = null
    trimEvents()
    statusText.value = reason
  }

  function createEvent(now: number, diffScore: number, changedRatio: number): VisionEventItem {
    const sequence = ++eventSequence
    return {
      id: crypto.randomUUID(),
      sequence,
      startAt: now,
      endAt: now,
      lastChangedAt: now,
      durationMs: 0,
      motionScore: diffScore,
      changedRatio,
      status: 'open',
      frames: [],
      summary: '检测到画面变化。'
    }
  }

  async function captureFrame(
    video: HTMLVideoElement,
    diffScore: number,
    changedRatio: number,
    kind: KeyframeKind,
    eventSeq?: number
  ): Promise<KeyframeItem> {
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
      diffScore,
      changedRatio,
      sequence: ++frameSequence,
      eventSequence: eventSeq ?? activeEvent.value?.sequence,
      kind
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

  function selectRecentEvents(mode: QuestionMode): VisionEventItem[] {
    const source = [...events.value]
    if (activeEvent.value) source.push(activeEvent.value)
    const max = mode === 'detailed' ? config.maxEvents : 3
    return source.slice(-max)
  }

  function selectRepresentativeFrames(event: VisionEventItem): KeyframeItem[] {
    const start = event.frames.find(frame => frame.kind === 'start')
    const peak = event.frames
      .filter(frame => frame.kind === 'peak' || frame.kind === 'manual')
      .sort((a, b) => b.diffScore - a.diffScore)[0]
    const end = [...event.frames].reverse().find(frame => frame.kind === 'end')
    return [start, peak, end].filter(Boolean) as KeyframeItem[]
  }

  function buildEventSummary(mode: QuestionMode, selectedEvents: VisionEventItem[], uploadFrameCount: number): string {
    const title = mode === 'detailed'
      ? '本轮问题需要详细视觉分析：上传最近事件的代表帧。'
      : '本轮问题需要理解动作变化：上传最近动作事件的代表帧。'

    if (!selectedEvents.length) {
      return `${title}\n最近没有形成完整动作事件。`
    }

    const lines = selectedEvents.map(event => {
      const duration = Math.round(event.durationMs / 100) / 10
      const motion = Math.round(event.motionScore * 100)
      const ratio = Math.round(event.changedRatio * 100)
      return `事件 #${event.sequence}：持续约 ${duration}s，运动强度 ${motion}%，变化区域 ${ratio}%，代表帧 ${event.frames.length} 张。${event.summary}`
    })

    return [
      title,
      `共选中 ${selectedEvents.length} 个视觉事件，上传 ${uploadFrameCount} 张代表帧。`,
      ...lines,
      '这些信息只是辅助上下文；回答时不要复述运动强度、事件编号或元数据。'
    ].join('\n')
  }

  function summarizeEvent(event: VisionEventItem): string {
    const duration = Math.round(event.durationMs / 100) / 10
    if (duration <= 0.5) {
      return '捕捉到一次很短的画面变化。'
    }
    return `捕捉到一次约 ${duration}s 的画面变化。`
  }

  function uniqueFrames(frames: KeyframeItem[]): KeyframeItem[] {
    const seen = new Set<string>()
    const result: KeyframeItem[] = []
    for (const frame of frames) {
      if (seen.has(frame.id)) continue
      seen.add(frame.id)
      result.push(frame)
    }
    return result
  }

  function removePreviousPeak(event: VisionEventItem) {
    const existingPeakIndex = event.frames.findIndex(frame => frame.kind === 'peak')
    if (existingPeakIndex >= 0) {
      const [removed] = event.frames.splice(existingPeakIndex, 1)
      if (removed) URL.revokeObjectURL(removed.url)
    }
  }

  function trimEvents() {
    while (events.value.length > config.maxEvents) {
      const removed = events.value.shift()
      if (removed) disposeFrames(removed.frames)
    }
  }

  function disposeFrames(frames: KeyframeItem[]) {
    for (const frame of frames) {
      URL.revokeObjectURL(frame.url)
    }
  }

  function ensureVideoReady(video: HTMLVideoElement) {
    if (!video.videoWidth || !video.videoHeight) {
      throw new Error('摄像头画面尚未准备好')
    }
  }

  return {
    events: visionEvents,
    keyframes,
    frameCount,
    eventCount,
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
    prepareUpload
  }
}
