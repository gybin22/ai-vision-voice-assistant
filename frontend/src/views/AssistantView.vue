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
            保存当前关键帧
          </button>

          <button class="call-btn" :disabled="!speech.isSpeaking.value" @click="speech.stop">
            <span class="call-icon">🔇</span>
            停止播报
          </button>
        </div>

        <KeyframePanel
            :keyframes="keyframeRecorder.keyframes.value"
            :is-running="keyframeRecorder.isRunning.value"
            :status-text="keyframeRecorder.statusText.value"
            :last-diff-score="keyframeRecorder.lastDiffScore.value"
            :total-bytes="keyframeRecorder.totalBytes.value"
        />
        <!--
        <div class="stage-hints">
          <span>画面变化明显才保存关键帧</span>
          <span>提问时按时间顺序上传全部关键帧</span>
          <span>默认每轮最多 8 张，控制成本</span>
        </div>
        -->
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
              {{ loading ? thinkingText : `发送 ${keyframeRecorder.frameCount.value || 1} 帧` }}
            </button>
          </div>

          <p class="composer-tip">
            我会在你对话时自动记住明显变化的关键帧。你提问时，我会结合这段动作过程来回答，而不是只看最后一张图。
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
import { useKeyframeRecorder } from '@/composables/useKeyframeRecorder'
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
const keyframeRecorder = useKeyframeRecorder({
  intervalMs: 850,
  diffThreshold: 0.075,
  changedRatioThreshold: 0.06,
  minSaveIntervalMs: 1800,
  maxFrames: 8,
  maxLongSide: 768,
  quality: 0.72,
  mimeType: 'image/jpeg',
  captureInitialFrame: true
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
    content: '先打开摄像头和麦克风吧。你可以像视频聊天一样问我，我会结合刚才捕捉到的关键帧来理解动作变化。'
  }
])
const loading = ref(false)
const thinkingText = ref('我看一下...')
const usage = ref<SessionUsage | null>(null)

const stream = media.stream
const isStarting = media.isStarting
const mediaError = media.error

const thinkingTexts = [
  '我看一下...',
  '让我观察一下刚才的动作...',
  '稍等，我正在理解画面...',
  '我在对比这几张关键帧...',
  '我先看看发生了什么...'
]

const canSend = computed(() => {
  return Boolean(
      question.value.trim() &&
      stream.value &&
      cameraRef.value?.isReady() &&
      !loading.value
  )
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
    pushError('我还没拿到清晰的摄像头画面，稍等一下再试。')
    return
  }

  try {
    await keyframeRecorder.forceSaveCurrent(video, '手动保存')
  } catch (error) {
    pushError(error instanceof Error ? error.message : '这次关键帧没保存成功，可以再试一次。')
  }
}

function onQuestionInput(value: string) {
  question.value = value
  lastInputType.value = 'text'
}

async function submit(inputType: InputType = 'text') {
  const text = question.value.trim()
  if (!text) return

  if (!stream.value) {
    pushError('先启动摄像头和麦克风，我才能看到你这边的画面。')
    return
  }

  const video = cameraRef.value?.getVideoElement()
  const isVideoReady = cameraRef.value?.isReady() ?? false

  if (!video || !isVideoReady) {
    pushError('摄像头画面还没准备好，稍等一下再问我。')
    return
  }

  thinkingText.value = pickThinkingText()
  loading.value = true
  messages.value.push({ id: crypto.randomUUID(), role: 'user', content: text })

  try {
    // 发送前再检测一次。若本轮还没有任何关键帧，则保存当前帧作为动作序列基准。
    await keyframeRecorder.forceCheck(video, { saveIfEmpty: true, reason: '发送前检测' })
    const frames = keyframeRecorder.getFramesForUpload()

    if (!frames.length) {
      pushError('我还没捕捉到明显变化。你可以稍微动一下，或者直接问当前画面。')
      return
    }

    const response = await askVision({
      sessionId,
      question: text,
      images: frames.map(frame => ({
        blob: frame.blob,
        width: frame.width,
        height: frame.height,
        capturedAt: frame.capturedAt,
        diffScore: frame.diffScore,
        sequence: frame.sequence
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
      meta: `${response.model}${response.cached ? ' · 命中缓存' : ''} · ${response.latencyMs}ms · 已发送 ${frames.length} 张关键帧 · ${totalKb}KB`
    })

    question.value = ''
    lastInputType.value = 'text'
    keyframeRecorder.clear({ resetBaseline: true })
    window.setTimeout(() => {
      speech.speak(response.answer)
    }, 220)
    await refreshUsage()
  } catch (e) {
    pushError(e instanceof Error ? e.message : '我这边刚才没处理成功，可以再问一次。')
  } finally {
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

function pickThinkingText() {
  return thinkingTexts[Math.floor(Math.random() * thinkingTexts.length)]
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
