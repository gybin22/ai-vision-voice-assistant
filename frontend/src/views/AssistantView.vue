<template>
  <main class="video-chat-page">
    <header class="app-topbar">
      <div class="brand">
        <div class="brand-mark">AI</div>
        <div>
          <h1>AI 视觉语音对话助手</h1>
        </div>
      </div>

      <div class="topbar-actions">
        <div class="token-balance-pill">
          <span>Tokens</span>
          <strong>{{ formatTokens(tokens.balanceTokens.value) }}</strong>
        </div>
        <button class="account-button" @click="emit('open-profile')">
          <span class="account-avatar">{{ accountInitial }}</span>
          <span>{{ auth.user.value?.nickname || auth.user.value?.email || '个人中心' }}</span>
        </button>
      </div>
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

        <div class="call-controls compact-call-controls">
          <button class="call-btn call-btn-primary" :disabled="isStarting || Boolean(stream)" @click="startCamera">
            <span class="call-icon">📷</span>
            {{ isStarting ? '启动中...' : '启动摄像头和麦克风' }}
          </button>

          <button class="call-btn" :disabled="!stream" @click="stopCamera">
            <span class="call-icon">⏹</span>
            停止摄像头
          </button>

          <button class="call-btn" :disabled="!speech.isSpeaking.value" @click="interruptSpeaking">
            <span class="call-icon">🔇</span>
            {{ liveMode ? '打断播报' : '停止播报' }}
          </button>
        </div>

        <div v-if="liveMode" class="live-status-card" :class="liveStatus">
          <div class="live-status-main">
            <span class="live-dot" />
            <strong>{{ liveStatusText }}</strong>
          </div>
          <p>{{ liveStatusHint }}</p>
        </div>

        <div v-if="mediaError" class="notice notice-warning">{{ mediaError }}</div>
        <div v-if="speechRecognition.error.value" class="notice notice-warning">{{ speechRecognition.error.value }}</div>
        <div v-if="!support.speechRecognition" class="notice notice-info">
          这个浏览器暂时不能语音识别，你可以切换到手动输入。
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

        <div
          class="composer-card"
          :class="{
            'composer-card-live': liveMode,
            'composer-card-entry': !liveMode && !manualMode,
            'composer-card-manual': !liveMode && manualMode
          }"
        >
          <div v-if="liveMode" class="live-transcript-panel">
            <div class="live-transcript-header">
              <span>{{ liveStatusText }}</span>
              <strong>{{ liveCountdownText }}</strong>
            </div>
            <p :class="{ muted: !liveTranscriptDisplay }">
              {{ liveTranscriptDisplay || '你可以直接说话。停顿约 1 秒后，我会自动提交这一句。' }}
            </p>
          </div>

          <div v-else-if="!manualMode" class="live-entry-panel">
            <span class="eyebrow">Live Conversation</span>
            <h3>开启实时对话</h3>
            <p>
              点击一次后就可以直接说话；系统会在你停顿约 1 秒后自动提交，并结合最近 15 秒画面回答。
            </p>
            <div class="live-entry-actions">
              <button class="send-button live-entry-primary" :disabled="isStarting || loading" @click="startLiveConversation">
                {{ isStarting ? '正在准备...' : '开始实时对话' }}
              </button>
              <button class="text-button" @click="showManualInput">
                切换到手动输入
              </button>
            </div>
          </div>

          <QuestionInput
            v-else
            :model-value="question"
            @update:model-value="onQuestionInput"
            @submit="submit(lastInputType)"
          />

          <div v-if="liveMode" class="composer-actions live-conversation-actions">
            <button class="send-button live-stop-button" @click="stopLiveConversation({ stopSpeech: true, showManual: true })">
              停止实时对话
            </button>
            <button class="voice-button" :disabled="!speech.isSpeaking.value" @click="interruptSpeaking">
              打断播报
            </button>
            <button class="text-button live-manual-switch" @click="switchToManualInput">
              手动输入
            </button>
          </div>

          <div v-else-if="manualMode" class="composer-actions manual-composer-actions">
            <VoiceButton
              :supported="speechRecognition.isSupported"
              :is-listening="speechRecognition.isListening.value"
              :disabled="loading"
              @start="startManualVoiceInput"
              @stop="speechRecognition.stop"
            />

            <button class="send-button" :disabled="!canSend" @click="submit(lastInputType)">
              {{ loading ? thinkingText : '发送' }}
            </button>

            <button class="text-button realtime-switch-button" :disabled="isStarting || loading" @click="startLiveConversation">
              切换到实时对话
            </button>
          </div>

          <p class="composer-tip">
            {{ liveMode
              ? '实时模式：系统持续听你说话；检测到约 1 秒停顿后自动提交，并在 AI 播报结束后继续监听。'
              : manualMode
                ? '手动模式：你可以输入文字后点击发送；仍会上传最近 15 秒视觉上下文。'
                : '推荐使用实时对话；手动输入保留为备用入口，适合不方便开麦或需要输入长文本时使用。' }}
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
import QuestionInput from '@/components/QuestionInput.vue'
import VoiceButton from '@/components/VoiceButton.vue'
import { useRollingFrameBuffer, type PreparedVisionUpload } from '@/composables/useRollingFrameBuffer'
import { useMediaDevices } from '@/composables/useMediaDevices'
import { useSpeechRecognition } from '@/composables/useSpeechRecognition'
import { useSpeechSynthesis } from '@/composables/useSpeechSynthesis'
import { askVision, clearConversation } from '@/services/assistantApi'
import { useAuth } from '@/composables/useAuth'
import { useTokens } from '@/composables/useTokens'
import type { ChatMessage, InputType } from '@/types/chat'
import { getBrowserSupport } from '@/utils/browserSupport'
import { getOrCreateSessionId } from '@/utils/session'

const emit = defineEmits<{ (e: 'open-profile'): void }>()
const auth = useAuth()
const tokens = useTokens()

type CameraPreviewExpose = {
  getVideoElement: () => HTMLVideoElement | null
  isReady: () => boolean
}

type SubmitOptions = {
  fromLive?: boolean
  textOverride?: string
}

type StopLiveOptions = {
  stopSpeech?: boolean
  showManual?: boolean
}

type LiveConversationStatus = 'off' | 'listening' | 'thinking' | 'speaking' | 'error'

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

const liveMode = ref(false)
const manualMode = ref(false)
const liveStatus = ref<LiveConversationStatus>('off')
const liveTranscript = ref('')
const liveSilenceDelayMs = 1000
const minLiveTextLength = 2
const minLiveSubmitIntervalMs = 3000
let liveSilenceTimer: number | undefined
let liveRestartTimer: number | undefined
let lastLiveSubmittedAt = 0

const stream = media.stream
const isStarting = media.isStarting
const mediaError = media.error

const accountInitial = computed(() => {
  const source = auth.user.value?.nickname || auth.user.value?.email || 'U'
  return source.slice(0, 1).toUpperCase()
})

const canSend = computed(() => {
  const text = question.value.trim()
  if (!text || loading.value || liveMode.value || !manualMode.value) return false
  return Boolean(stream.value && cameraRef.value?.isReady())
})

const liveStatusText = computed(() => {
  switch (liveStatus.value) {
    case 'listening':
      return speechRecognition.isListening.value ? '正在听你说话' : '正在准备继续听'
    case 'thinking':
      return '正在思考'
    case 'speaking':
      return '正在回答'
    case 'error':
      return '实时对话异常'
    default:
      return '实时对话未开启'
  }
})

const liveStatusHint = computed(() => {
  switch (liveStatus.value) {
    case 'listening':
      return '说完一句后停顿约 1 秒，我会自动提交。'
    case 'thinking':
      return '我已经听到这一句，正在结合最近 15 秒画面生成回答。'
    case 'speaking':
      return 'AI 播报期间会暂停识别，避免把回答再次识别成你的话。'
    case 'error':
      return '可以停止实时对话后重新开启。'
    default:
      return '点击“开始实时对话”后，无需每次手动发送。'
  }
})

const liveTranscriptDisplay = computed(() => liveTranscript.value.trim())

const liveCountdownText = computed(() => {
  if (liveStatus.value === 'listening') return '停顿 1 秒自动提交'
  if (liveStatus.value === 'thinking') return '等待回答'
  if (liveStatus.value === 'speaking') return '播报中'
  return ''
})

watch(speechRecognition.transcript, value => {
  if (liveMode.value) {
    handleLiveTranscript(value)
    return
  }

  if (value) {
    question.value = value
    lastInputType.value = 'voice'
  }
})

watch(speechRecognition.isListening, value => {
  if (!value && liveMode.value && liveStatus.value === 'listening' && !loading.value && !speech.isSpeaking.value) {
    scheduleLiveRecognitionRestart()
  }
})

watch(speechRecognition.error, value => {
  if (value && liveMode.value) {
    liveStatus.value = 'error'
    clearLiveTimers()
  }
})

watch(stream, value => {
  if (!value) {
    rollingBuffer.stop()
    rollingBuffer.clear()
    if (liveMode.value) {
      stopLiveConversation({ stopSpeech: true })
    }
  }
})

onMounted(() => {
  void refreshTokenBalance()
})

onBeforeUnmount(() => {
  stopLiveConversation({ stopSpeech: true })
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
  stopLiveConversation({ stopSpeech: true })
  rollingBuffer.stop()
  rollingBuffer.clear()
  media.stop()
}

function onQuestionInput(value: string) {
  question.value = value
  lastInputType.value = 'text'
}

function startManualVoiceInput() {
  speechRecognition.start({
    continuous: false,
    interimResults: true,
    clearTranscriptOnStart: true
  })
}

async function toggleLiveConversation() {
  if (liveMode.value) {
    stopLiveConversation({ stopSpeech: true })
    return
  }

  await startLiveConversation()
}

async function startLiveConversation() {
  if (!speechRecognition.isSupported) {
    pushError('当前浏览器不支持语音识别，无法开启实时对话。')
    return
  }

  try {
    if (!stream.value) {
      await startCamera()
    }

    await waitForVideoReady()
    speech.stop()
    question.value = ''
    liveTranscript.value = ''
    manualMode.value = false
    liveMode.value = true
    liveStatus.value = 'listening'
    startLiveRecognition()
  } catch (error) {
    liveMode.value = false
    liveStatus.value = 'off'
    pushError(error instanceof Error ? error.message : '实时对话启动失败。')
  }
}

function stopLiveConversation(options: StopLiveOptions = {}) {
  clearLiveTimers()
  speechRecognition.stop()
  speechRecognition.reset()
  if (options.stopSpeech ?? true) {
    speech.stop()
  }
  liveMode.value = false
  liveStatus.value = 'off'
  liveTranscript.value = ''
  question.value = ''
  if (options.showManual ?? false) {
    manualMode.value = true
  }
}

function showManualInput() {
  manualMode.value = true
}

function switchToManualInput() {
  stopLiveConversation({ stopSpeech: true, showManual: true })
}

function interruptSpeaking() {
  speech.stop()
  if (liveMode.value) {
    resumeLiveListening()
  }
}

function startLiveRecognition() {
  if (!liveMode.value || loading.value || speech.isSpeaking.value) return

  clearLiveTimers()
  liveTranscript.value = ''
  question.value = ''
  liveStatus.value = 'listening'
  speechRecognition.start({
    continuous: true,
    interimResults: true,
    clearTranscriptOnStart: true
  })
}

function handleLiveTranscript(value: string) {
  if (liveStatus.value !== 'listening') return

  const text = value.trim()
  liveTranscript.value = text
  question.value = text

  if (text.length >= minLiveTextLength) {
    scheduleLiveAutoSubmit()
  } else {
    clearLiveSilenceTimer()
  }
}

function scheduleLiveAutoSubmit() {
  clearLiveSilenceTimer()
  liveSilenceTimer = window.setTimeout(() => {
    void submitLiveUtterance()
  }, liveSilenceDelayMs)
}

async function submitLiveUtterance() {
  const text = liveTranscript.value.trim()
  if (!liveMode.value || liveStatus.value !== 'listening' || loading.value || text.length < minLiveTextLength) return

  const now = Date.now()
  const elapsed = now - lastLiveSubmittedAt
  if (elapsed < minLiveSubmitIntervalMs) {
    clearLiveSilenceTimer()
    liveSilenceTimer = window.setTimeout(() => {
      void submitLiveUtterance()
    }, minLiveSubmitIntervalMs - elapsed)
    return
  }

  lastLiveSubmittedAt = now
  clearLiveTimers()
  liveStatus.value = 'thinking'
  speechRecognition.stop()
  speechRecognition.reset()
  question.value = text
  await submit('voice', { fromLive: true, textOverride: text })
}

function resumeLiveListening() {
  if (!liveMode.value) return
  if (loading.value || speech.isSpeaking.value) return

  clearLiveTimers()
  speechRecognition.reset()
  question.value = ''
  liveTranscript.value = ''
  liveStatus.value = 'listening'
  scheduleLiveRecognitionRestart(220)
}

function scheduleLiveRecognitionRestart(delayMs = 360) {
  if (!liveMode.value || liveStatus.value !== 'listening' || loading.value || speech.isSpeaking.value) return

  if (liveRestartTimer !== undefined) {
    window.clearTimeout(liveRestartTimer)
  }

  liveRestartTimer = window.setTimeout(() => {
    liveRestartTimer = undefined
    if (liveMode.value && liveStatus.value === 'listening' && !loading.value && !speech.isSpeaking.value) {
      startLiveRecognition()
    }
  }, delayMs)
}

function clearLiveTimers() {
  clearLiveSilenceTimer()
  if (liveRestartTimer !== undefined) {
    window.clearTimeout(liveRestartTimer)
    liveRestartTimer = undefined
  }
}

function clearLiveSilenceTimer() {
  if (liveSilenceTimer !== undefined) {
    window.clearTimeout(liveSilenceTimer)
    liveSilenceTimer = undefined
  }
}

async function submit(_inputType: InputType = 'text', options: SubmitOptions = {}) {
  const text = (options.textOverride ?? question.value).trim()
  if (!text || loading.value) return

  let preparedUpload: PreparedVisionUpload | null = null
  let shouldResumeLiveOnFailure = Boolean(options.fromLive && liveMode.value)

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
      enableHistory: true,
      maxOutputTokens: 500
    })

    const totalKb = Math.round(response.usage.imageBytes / 1024)
    tokens.applyBalanceAfter(response.billing.balanceAfterTokens)
    messages.value.push({
      id: crypto.randomUUID(),
      role: 'assistant',
      content: response.answer,
      meta: buildMeta(response.model, response.cached, response.latencyMs, preparedUpload.frames.length, totalKb, response.billing.chargedTokens, response.usage.totalTokens)
    })

    question.value = ''
    lastInputType.value = 'text'

    if (options.fromLive && liveMode.value) {
      shouldResumeLiveOnFailure = false
      liveStatus.value = 'speaking'
      const didSpeak = speech.speak(response.answer, {
        onEnd: resumeLiveListening,
        onError: resumeLiveListening
      })
      if (!didSpeak) {
        resumeLiveListening()
      }
    } else {
      window.setTimeout(() => {
        speech.speak(response.answer)
      }, 180)
    }

    await refreshTokenBalance()
  } catch (e) {
    pushError(e instanceof Error ? e.message : '我这边刚才没处理成功，可以再问一次。')
  } finally {
    preparedUpload?.dispose()
    loading.value = false
    if (shouldResumeLiveOnFailure && liveMode.value) {
      resumeLiveListening()
    }
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

function buildMeta(model: string, cached: boolean, latencyMs: number, frameCount: number, totalKb: number, chargedTokens: number, totalTokens: number) {
  return `${model}${cached ? ' · 命中缓存' : ''} · ${latencyMs}ms · 最近 15 秒视觉上下文 · 已发送 ${frameCount} 帧 · ${totalKb}KB · 模型 ${formatTokens(totalTokens)} tokens · 扣减 ${formatTokens(chargedTokens)} tokens`
}

function delay(ms: number) {
  return new Promise(resolve => window.setTimeout(resolve, ms))
}

function pushError(content: string) {
  messages.value.push({ id: crypto.randomUUID(), role: 'error', content })
}

async function refreshTokenBalance() {
  try {
    await tokens.refreshBalance()
  } catch {
    // Tokens 查询失败不影响页面打开；发送请求时后端仍会校验余额。
  }
}

function formatTokens(value: number) {
  return Math.floor(value).toLocaleString('zh-CN')
}

async function clearLocalConversation() {
  if (liveMode.value) {
    stopLiveConversation({ stopSpeech: true })
  }

  messages.value = []
  question.value = ''
  rollingBuffer.clear()
  try {
    await clearConversation(sessionId)
  } catch {
    // 忽略清理错误
  }
}
</script>
