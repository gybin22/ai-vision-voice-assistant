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

          <button class="call-btn" :disabled="!speech.isSpeaking.value" @click="speech.stop">
            <span class="call-icon">🔇</span>
            停止播报
          </button>
        </div>
        <!--
        <div class="stage-hints">
          <span>发送问题时才会上传当前画面截图</span>
          <span>图片最长边 768px</span>
          <span>默认保留最近 3 轮上下文</span>
        </div>
        -->
        <div v-if="mediaError" class="notice notice-warning">{{ mediaError }}</div>
        <div v-if="speechRecognition.error.value" class="notice notice-warning">{{ speechRecognition.error.value }}</div>
        <div v-if="!support.speechRecognition" class="notice notice-info">
          当前浏览器不支持语音识别，请使用文字输入。
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
              {{ loading ? '请求中...' : '发送问题' }}
            </button>
          </div>

          <p class="composer-tip">
            语音识别结果会自动填入输入框；点击发送后，系统会截取左侧视频当前帧。
          </p>
        </div>
      </aside>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import CameraPreview from '@/components/CameraPreview.vue'
import ChatMessages from '@/components/ChatMessages.vue'
import CostStatusBar from '@/components/CostStatusBar.vue'
import QuestionInput from '@/components/QuestionInput.vue'
import VoiceButton from '@/components/VoiceButton.vue'
import { useFrameCapture } from '@/composables/useFrameCapture'
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
const frameCapture = useFrameCapture()
const speechRecognition = useSpeechRecognition()
const speech = useSpeechSynthesis()

const cameraRef = ref<CameraPreviewExpose | null>(null)
const question = ref('')
const lastInputType = ref<InputType>('text')
const messages = ref<ChatMessage[]>([
  {
    id: crypto.randomUUID(),
    role: 'assistant',
    content: '请先启动摄像头和麦克风。你可以用文字或语音提问，我会结合当前摄像头画面回答。'
  }
])
const loading = ref(false)
const usage = ref<SessionUsage | null>(null)

const stream = media.stream
const isStarting = media.isStarting
const mediaError = media.error

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

onMounted(() => {
  refreshUsage()
})

onBeforeUnmount(() => {
  media.stop()
  speech.stop()
})

async function startCamera() {
  try {
    await media.start()
  } catch {
    // media.error 已经保存错误文本
  }
}

function stopCamera() {
  media.stop()
}

function onQuestionInput(value: string) {
  question.value = value
  lastInputType.value = 'text'
}

async function submit(inputType: InputType = 'text') {
  const text = question.value.trim()
  if (!text) return

  if (!stream.value) {
    pushError('请先启动摄像头和麦克风。')
    return
  }

  const video = cameraRef.value?.getVideoElement()
  const isVideoReady = cameraRef.value?.isReady() ?? false

  if (!video || !isVideoReady) {
    pushError('摄像头画面尚未准备好，请等待 1 秒后再发送。')
    return
  }

  loading.value = true
  messages.value.push({ id: crypto.randomUUID(), role: 'user', content: text })

  try {
    const captured = await frameCapture.capture(video, {
      maxLongSide: 768,
      quality: 0.72,
      mimeType: 'image/jpeg'
    })

    const response = await askVision({
      sessionId,
      question: text,
      image: captured.blob,
      inputType,
      enableHistory: true,
      maxOutputTokens: 500,
      clientImageWidth: captured.width,
      clientImageHeight: captured.height
    })

    messages.value.push({
      id: crypto.randomUUID(),
      role: 'assistant',
      content: response.answer,
      meta: `${response.model}${response.cached ? ' · 命中缓存' : ''} · ${response.latencyMs}ms · 图片 ${Math.round(response.usage.imageBytes / 1024)}KB`
    })

    question.value = ''
    lastInputType.value = 'text'
    speech.speak(response.answer)
    await refreshUsage()
  } catch (e) {
    pushError(e instanceof Error ? e.message : '请求失败')
  } finally {
    loading.value = false
  }
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
  try {
    await clearConversation(sessionId)
    await refreshUsage()
  } catch {
    // 忽略清理错误
  }
}
</script>
