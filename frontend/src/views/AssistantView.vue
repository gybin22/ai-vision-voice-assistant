<template>
  <main class="app-shell">
    <header class="header">
      <div>
        <h1>AI 视觉语音对话助手</h1>
        <p>提问时截取当前摄像头画面，后端调用多模态模型回答，前端语音播报。</p>
      </div>
      <CostStatusBar :session-id="sessionId" :usage="usage" />
    </header>

    <section class="grid">
      <div class="card">
        <div class="card-body">
          <CameraPreview ref="cameraRef" :stream="stream" />

          <div class="controls">
            <button class="btn btn-primary" :disabled="isStarting || Boolean(stream)" @click="startCamera">
              {{ isStarting ? '启动中...' : '启动摄像头和麦克风' }}
            </button>
            <button class="btn btn-ghost" :disabled="!stream" @click="stopCamera">停止摄像头</button>
            <button class="btn btn-ghost" :disabled="!speech.isSpeaking.value" @click="speech.stop">停止播报</button>
          </div>

          <div v-if="mediaError" class="notice">{{ mediaError }}</div>
          <div v-if="speechRecognition.error.value" class="notice">{{ speechRecognition.error.value }}</div>
          <div v-if="!support.speechRecognition" class="notice">当前浏览器不支持语音识别，请使用文字输入。</div>
        </div>
      </div>

      <div class="card">
        <div class="card-body">
          <QuestionInput :model-value="question" @update:model-value="onQuestionInput" @submit="submit(lastInputType)" />

          <div class="controls">
            <button class="btn btn-primary" :disabled="!canSend" @click="submit(lastInputType)">
              {{ loading ? '请求中...' : '发送问题' }}
            </button>
            <VoiceButton
              :supported="speechRecognition.isSupported"
              :is-listening="speechRecognition.isListening.value"
              :disabled="loading"
              @start="speechRecognition.start"
              @stop="speechRecognition.stop"
            />
            <button class="btn btn-ghost" :disabled="loading" @click="clearLocalConversation">清空本地对话</button>
          </div>

          <div class="meta">语音识别结果会自动填入输入框。发送时才会上传当前帧图片。</div>
        </div>
      </div>
    </section>

    <section class="card" style="margin-top: 18px;">
      <div class="card-body">
        <ChatMessages :messages="messages" />
      </div>
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
    content: '请先启动摄像头和麦克风，然后用文字或语音提问。MVP 会在你点击发送时上传当前画面截图。'
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
