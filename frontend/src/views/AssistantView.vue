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

          <button class="call-btn" :disabled="!stream" @click="saveFrameNow">
            <span class="call-icon">📌</span>
            立即采样一帧
          </button>

          <button class="call-btn" :disabled="!speech.isSpeaking.value" @click="speech.stop">
            <span class="call-icon">🔇</span>
            停止播报
          </button>
        </div>

        <KeyframePanel
          :frames="rollingBuffer.frames.value"
          :is-running="rollingBuffer.isRunning.value"
          :status-text="rollingBuffer.statusText.value"
          :total-bytes="rollingBuffer.totalBytes.value"
          :coverage-seconds="rollingBuffer.coverageSeconds.value"
          :max-frames="15"
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
              {{ loading ? thinkingText : '发送 · 最近 15 秒视觉上下文' }}
            </button>
          </div>

          <p class="composer-tip">
            当前策略：不再做问题分类；每次提问都会上传最近 15 秒、约 1fps 的视觉抽帧，最后一帧是发送瞬间当前帧。模型自行判断是否结合图片回答。
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
import { useRollingFrameBuffer, type PreparedVisionUpload } from '@/composables/useRollingFrameBuffer'
import { useMediaDevices } from '@/composables/useMediaDevices'
import { useSpeechRecognition } from '@/composables/useSpeechRecognition'
import { useSpeechSynthesis } from '@/composables/useSpeechSynthesis'
import { askVision, clearConversation, getSessionUsage } from '@/services/assistantApi'
import type { ChatMessage, InputType, SessionUsage } from '@/types/chat'
import { getBrowserSupport } from '@/utils/browserSupport'
import { getOrCreateSessionId } from '@/utils/session'

type CameraPreviewExpose = {
  getVideoElement: () => HTMLVideoElement | null
  isReady: () => boolean
}

const sessionId = getOrCreateSessionId()
const support = getBrowserSupport()
const media = useMediaDevices()
const rollingBuffer = useRollingFrameBuffer({
  sampleIntervalMs: 1000,
  maxStoredFrames: 15,
  maxUploadFrames: 15,
  maxLongSide: 640,
  quality: 0.6,
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
    content: '你可以直接像视频聊天一样问我。摄像头开启后，我会在本地保存最近 15 秒的轻量视觉帧；你每次提问时，我会把这些帧按时间顺序发给模型，由模型自行判断是否需要结合画面。'
  }
])
const loading = ref(false)
const thinkingText = ref('我看一下最近 15 秒...')
const usage = ref<SessionUsage | null>(null)

const stream = media.stream
const isStarting = media.isStarting
const mediaError = media.error

const canSend = computed(() => {
  const text = question.value.trim()
  if (!text || loading.value) return false
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
    rollingBuffer.stop()
    rollingBuffer.clear()
  }
})

onMounted(() => {
  refreshUsage()
})

onBeforeUnmount(() => {
  media.stop()
  rollingBuffer.stop()
  rollingBuffer.clear()
  speech.stop()
})

async function startCamera() {
  try {
    await media.start()
    const video = await waitForVideoReady()
    await rollingBuffer.start(video)
  } catch (error) {
    pushError(error instanceof Error ? error.message : '摄像头启动失败，可以检查一下浏览器权限。')
  }
}

function stopCamera() {
  rollingBuffer.stop()
  rollingBuffer.clear()
  media.stop()
}

async function saveFrameNow() {
  const video = cameraRef.value?.getVideoElement()
  if (!video || !cameraRef.value?.isReady()) {
    pushError('摄像头画面还没准备好，稍等一下再试。')
    return
  }

  try {
    await rollingBuffer.forceSaveCurrent(video)
  } catch (error) {
    pushError(error instanceof Error ? error.message : '这次采样没保存成功，可以再试一次。')
  }
}

function onQuestionInput(value: string) {
  question.value = value
  lastInputType.value = 'text'
}

async function submit(inputType: InputType = 'text') {
  const text = question.value.trim()
  if (!text) return

  let preparedUpload: PreparedVisionUpload | null = null

  thinkingText.value = '我看一下最近 15 秒...'
  loading.value = true
  messages.value.push({ id: crypto.randomUUID(), role: 'user', content: text })

  try {
    if (!stream.value) {
      pushError('请先启动摄像头。当前策略要求每次提问都上传最近 15 秒视觉上下文。')
      return
    }

    const video = cameraRef.value?.getVideoElement()
    const isVideoReady = cameraRef.value?.isReady() ?? false

    if (!video || !isVideoReady) {
      pushError('摄像头画面还没准备好，稍等一下再问我。')
      return
    }

    preparedUpload = await rollingBuffer.prepareUpload(video)

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
        offsetMs: frame.offsetMs,
        sequence: frame.sequence,
        role: frame.role
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
      meta: buildMeta(response.model, response.cached, response.latencyMs, preparedUpload.frames.length, totalKb)
    })

    question.value = ''
    lastInputType.value = 'text'

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

function buildMeta(model: string, cached: boolean, latencyMs: number, frameCount: number, totalKb: number) {
  return `${model}${cached ? ' · 命中缓存' : ''} · ${latencyMs}ms · 最近 15 秒视觉上下文 · 已发送 ${frameCount} 帧 · ${totalKb}KB`
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
  rollingBuffer.clear()
  try {
    await clearConversation(sessionId)
    await refreshUsage()
  } catch {
    // 忽略清理错误
  }
}
</script>
