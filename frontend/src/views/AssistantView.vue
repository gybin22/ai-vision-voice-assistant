<template>
  <main class="video-chat-page">
    <header class="app-topbar">
      <div class="brand">
        <div class="brand-mark">AI</div>
        <div>
          <h1>AI 视觉语音对话助手</h1>
        </div>
      </div>

      <CostStatusBar :session-id="sessionId" :usage="usage" />
    </header>

    <section class="video-chat-shell">
      <section class="stage-card">
        <div class="stage-header">
          <div>
            <span class="eyebrow">Live Camera</span>
            <h2>实时画面</h2>
          </div>
          <span class="status-pill" :class="{ active: Boolean(stream) }">
            {{ stream ? '摄像头已连接' : '等待连接' }}
          </span>
        </div>

        <CameraPreview ref="cameraRef" :stream="stream" />

        <div class="call-controls">
          <button class="call-btn call-btn-primary" :disabled="isStarting || Boolean(stream)" @click="startCamera">
            <span class="call-icon">📷</span>
            {{ isStarting ? '启动中...' : '启动摄像头和麦克风' }}
          </button>

          <button class="call-btn" :disabled="!stream" @click="stopCamera">
            <span class="call-icon">⏹</span>
            停止摄像头
          </button>

          <button class="call-btn" :disabled="!stream" @click="saveKeyframeNow">
            <span class="call-icon">📌</span>
            保存当前事件帧
          </button>

          <button class="call-btn" :disabled="!speech.isSpeaking.value" @click="speech.stop">
            <span class="call-icon">🔇</span>
            停止播报
          </button>
        </div>

        <KeyframePanel
          :events="keyframeRecorder.events.value"
          :is-running="keyframeRecorder.isRunning.value"
          :status-text="keyframeRecorder.statusText.value"
          :last-diff-score="keyframeRecorder.lastDiffScore.value"
          :total-bytes="keyframeRecorder.totalBytes.value"
          :question-mode="activeQuestionMode"
        />

        <div v-if="mediaError" class="notice notice-warning">{{ mediaError }}</div>
        <div v-if="speechRecognition.error.value" class="notice notice-warning">{{ speechRecognition.error.value }}</div>
        <div v-if="!support.speechRecognition" class="notice notice-info">
          这个浏览器暂时不能语音识别，你可以直接打字问我。
        </div>
      </section>

      <aside class="chat-card">
        <div class="chat-header">
          <div>
            <span class="eyebrow">Conversation</span>
            <h2>对话</h2>
          </div>
          <button class="link-button" :disabled="loading" @click="clearLocalConversation">清空</button>
        </div>

        <ChatMessages :messages="messages" />

        <div class="composer-card">
          <QuestionInput
            :model-value="question"
            @update:model-value="onQuestionInput"
            @submit="submit(lastInputType)"
          />

          <div class="composer-actions">
            <VoiceButton
              :supported="speechRecognition.isSupported"
              :is-listening="speechRecognition.isListening.value"
              :disabled="loading"
              @start="speechRecognition.start"
              @stop="speechRecognition.stop"
            />

            <button class="send-button" :disabled="!canSend" @click="submit(lastInputType)">
              {{ loading ? thinkingText : `发送 · ${activeQuestionModeLabel}` }}
            </button>
          </div>

          <p class="composer-tip">
            上传策略会按问题自动切换：普通聊天不发图，问当前画面只发当前帧，问刚才发生什么才发事件代表帧。
          </p>
        </div>
      </aside>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import CameraPreview from '@/components/CameraPreview.vue'
import ChatMessages from '@/components/ChatMessages.vue'
import CostStatusBar from '@/components/CostStatusBar.vue'
import KeyframePanel from '@/components/KeyframePanel.vue'
import QuestionInput from '@/components/QuestionInput.vue'
import VoiceButton from '@/components/VoiceButton.vue'
import { useKeyframeRecorder, type PreparedVisionUpload } from '@/composables/useKeyframeRecorder'
import { useMediaDevices } from '@/composables/useMediaDevices'
import { useSpeechRecognition } from '@/composables/useSpeechRecognition'
import { useSpeechSynthesis } from '@/composables/useSpeechSynthesis'
import { askVision, clearConversation, getSessionUsage } from '@/services/assistantApi'
import type { ChatMessage, InputType, QuestionMode, SessionUsage } from '@/types/chat'
import { getBrowserSupport } from '@/utils/browserSupport'
import { classifyQuestionMode, questionModeLabel } from '@/utils/questionMode'
import { getOrCreateSessionId } from '@/utils/session'

type CameraPreviewExpose = {
  getVideoElement: () => HTMLVideoElement | null
  isReady: () => boolean
}

const sessionId = getOrCreateSessionId()
const support = getBrowserSupport()
const media = useMediaDevices()
const keyframeRecorder = useKeyframeRecorder({
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
})
const speechRecognition = useSpeechRecognition()
const speech = useSpeechSynthesis()

const cameraRef = ref<CameraPreviewExpose | null>(null)
const question = ref('')
const lastInputType = ref<InputType>('text')
const messages = ref<ChatMessage[]>([
  {
    id: crypto.randomUUID(),
    role: 'assistant',
    content: '你可以直接像视频聊天一样问我。普通聊天我不会上传图片；你问画面或刚才的动作时，我再使用摄像头上下文。'
  }
])
const loading = ref(false)
const thinkingText = ref('我处理一下...')
const usage = ref<SessionUsage | null>(null)

const stream = media.stream
const isStarting = media.isStarting
const mediaError = media.error

const activeQuestionMode = computed<QuestionMode>(() => classifyQuestionMode(question.value))
const activeQuestionModeLabel = computed(() => questionModeLabel(activeQuestionMode.value))

const canSend = computed(() => {
  const text = question.value.trim()
  if (!text || loading.value) return false
  if (activeQuestionMode.value === 'chat') return true
  return Boolean(stream.value && cameraRef.value?.isReady())
})

watch(speechRecognition.transcript, value => {
  if (value) {
    question.value = value
    lastInputType.value = 'voice'
  }
})

watch(stream, value => {
  if (!value) {
    keyframeRecorder.stop()
    keyframeRecorder.clear({ resetBaseline: true })
  }
})

onMounted(() => {
  refreshUsage()
})

onBeforeUnmount(() => {
  media.stop()
  keyframeRecorder.stop()
  keyframeRecorder.clear({ resetBaseline: true })
  speech.stop()
})

async function startCamera() {
  try {
    await media.start()
    const video = await waitForVideoReady()
    await keyframeRecorder.start(video)
  } catch (error) {
    pushError(error instanceof Error ? error.message : '摄像头启动失败，可以检查一下浏览器权限。')
  }
}

function stopCamera() {
  keyframeRecorder.stop()
  keyframeRecorder.clear({ resetBaseline: true })
  media.stop()
}

async function saveKeyframeNow() {
  const video = cameraRef.value?.getVideoElement()
  if (!video || !cameraRef.value?.isReady()) {
    pushError('摄像头画面还没准备好，稍等一下再试。')
    return
  }

  try {
    await keyframeRecorder.forceSaveCurrent(video, '手动保存')
  } catch (error) {
    pushError(error instanceof Error ? error.message : '这次事件帧没保存成功，可以再试一次。')
  }
}

function onQuestionInput(value: string) {
  question.value = value
  lastInputType.value = 'text'
}

async function submit(inputType: InputType = 'text') {
  const text = question.value.trim()
  if (!text) return

  const mode = classifyQuestionMode(text)
  let preparedUpload: PreparedVisionUpload | null = null

  thinkingText.value = pickThinkingText(mode)
  loading.value = true
  messages.value.push({ id: crypto.randomUUID(), role: 'user', content: text })

  try {
    if (mode === 'chat') {
      preparedUpload = {
        mode,
        frames: [],
        visualSummary: '本轮是普通聊天：没有上传图片，回答时不应主动提画面。',
        eventCount: keyframeRecorder.eventCount.value,
        dispose: () => {}
      }
    } else {
      if (!stream.value) {
        pushError('这个问题需要看摄像头画面，请先启动摄像头。')
        return
      }

      const video = cameraRef.value?.getVideoElement()
      const isVideoReady = cameraRef.value?.isReady() ?? false

      if (!video || !isVideoReady) {
        pushError('摄像头画面还没准备好，稍等一下再问我。')
        return
      }

      preparedUpload = await keyframeRecorder.prepareUpload(video, mode)
    }

    const response = await askVision({
      sessionId,
      question: text,
      questionMode: preparedUpload.mode,
      visualSummary: preparedUpload.visualSummary,
      images: preparedUpload.frames.map(frame => ({
        blob: frame.blob,
        width: frame.width,
        height: frame.height,
        capturedAt: frame.capturedAt,
        diffScore: frame.diffScore,
        changedRatio: frame.changedRatio,
        sequence: frame.sequence,
        eventSequence: frame.eventSequence,
        kind: frame.kind
      })),
      inputType,
      enableHistory: true,
      maxOutputTokens: 500
    })

    const totalKb = Math.round(response.usage.imageBytes / 1024)
    messages.value.push({
      id: crypto.randomUUID(),
      role: 'assistant',
      content: response.answer,
      meta: buildMeta(response.model, response.cached, response.latencyMs, preparedUpload.mode, preparedUpload.frames.length, totalKb)
    })

    question.value = ''
    lastInputType.value = 'text'

    if (preparedUpload.mode === 'motion' || preparedUpload.mode === 'detailed') {
      keyframeRecorder.clear({ resetBaseline: true })
    }

    window.setTimeout(() => {
      speech.speak(response.answer)
    }, 180)
    await refreshUsage()
  } catch (e) {
    pushError(e instanceof Error ? e.message : '我这边刚才没处理成功，可以再问一次。')
  } finally {
    preparedUpload?.dispose()
    loading.value = false
  }
}

async function waitForVideoReady(timeoutMs = 4000): Promise<HTMLVideoElement> {
  const startedAt = Date.now()

  while (Date.now() - startedAt < timeoutMs) {
    await nextTick()
    const video = cameraRef.value?.getVideoElement()
    if (video && cameraRef.value?.isReady()) {
      return video
    }
    await delay(120)
  }

  throw new Error('摄像头画面还没准备好，稍等一下再试。')
}

function pickThinkingText(mode: QuestionMode) {
  const texts: Record<QuestionMode, string[]> = {
    chat: ['我想一下...', '我处理一下...', '稍等一下...'],
    current: ['我看一下当前画面...', '正在看当前帧...', '我确认一下...'],
    motion: ['我在对比刚才的动作...', '正在整理最近的视觉事件...', '我看一下刚才发生了什么...'],
    detailed: ['我会多看几帧再回答...', '正在做更完整的视觉分析...', '我仔细对比一下...']
  }
  const candidates = texts[mode]
  return candidates[Math.floor(Math.random() * candidates.length)]
}

function buildMeta(model: string, cached: boolean, latencyMs: number, mode: QuestionMode, frameCount: number, totalKb: number) {
  const uploadText = frameCount > 0 ? `已发送 ${frameCount} 张视觉帧 · ${totalKb}KB` : '未上传图片'
  return `${model}${cached ? ' · 命中缓存' : ''} · ${latencyMs}ms · ${questionModeLabel(mode)} · ${uploadText}`
}

function delay(ms: number) {
  return new Promise(resolve => window.setTimeout(resolve, ms))
}

function pushError(content: string) {
  messages.value.push({ id: crypto.randomUUID(), role: 'error', content })
}

async function refreshUsage() {
  try {
    usage.value = await getSessionUsage(sessionId)
  } catch {
    // 用量查询失败不影响主流程
  }
}

async function clearLocalConversation() {
  messages.value = []
  question.value = ''
  keyframeRecorder.clear({ resetBaseline: true })
  try {
    await clearConversation(sessionId)
    await refreshUsage()
  } catch {
    // 忽略清理错误
  }
}
</script>
