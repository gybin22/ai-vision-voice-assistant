import { ref } from 'vue'

export interface SpeechRecognitionStartOptions {
  continuous?: boolean
  interimResults?: boolean
  clearTranscriptOnStart?: boolean
}

const DEFAULT_START_OPTIONS: Required<SpeechRecognitionStartOptions> = {
  continuous: false,
  interimResults: true,
  clearTranscriptOnStart: true
}

export function useSpeechRecognition() {
  const isSupported = Boolean(window.SpeechRecognition || window.webkitSpeechRecognition)
  const isListening = ref(false)
  const transcript = ref('')
  const finalTranscript = ref('')
  const interimTranscript = ref('')
  const error = ref('')
  let recognition: SpeechRecognition | null = null

  function createRecognition(options: Required<SpeechRecognitionStartOptions>) {
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!Recognition) return null

    const instance = new Recognition()
    instance.lang = 'zh-CN'
    instance.continuous = options.continuous
    instance.interimResults = options.interimResults
    instance.maxAlternatives = 1

    instance.onresult = event => {
      let finalText = finalTranscript.value
      let interimText = ''

      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const text = event.results[i][0].transcript
        if (event.results[i].isFinal) {
          finalText = `${finalText}${text}`
        } else {
          interimText = `${interimText}${text}`
        }
      }

      finalTranscript.value = finalText.trim()
      interimTranscript.value = interimText.trim()
      transcript.value = `${finalTranscript.value}${interimTranscript.value}`.trim()
    }

    instance.onerror = event => {
      error.value = `语音识别失败：${event.error}`
      isListening.value = false
    }

    instance.onend = () => {
      isListening.value = false
    }

    return instance
  }

  function start(options: SpeechRecognitionStartOptions = {}) {
    error.value = ''
    if (!isSupported) {
      error.value = '当前浏览器不支持语音识别，请使用文字输入。'
      return
    }

    const mergedOptions = { ...DEFAULT_START_OPTIONS, ...options }
    if (mergedOptions.clearTranscriptOnStart) {
      reset()
    }

    disposeRecognition()
    recognition = createRecognition(mergedOptions)

    try {
      recognition?.start()
      isListening.value = true
    } catch (e) {
      error.value = e instanceof Error ? e.message : '语音识别启动失败。'
      isListening.value = false
    }
  }

  function stop() {
    try {
      recognition?.stop()
    } catch {
      // 浏览器在非 listening 状态 stop 可能抛错，忽略即可。
    }
    isListening.value = false
  }

  function reset() {
    transcript.value = ''
    finalTranscript.value = ''
    interimTranscript.value = ''
  }

  function disposeRecognition() {
    if (!recognition) return
    recognition.onresult = null
    recognition.onerror = null
    recognition.onend = null
    try {
      recognition.stop()
    } catch {
      // ignore
    }
    recognition = null
  }

  return {
    isSupported,
    isListening,
    transcript,
    finalTranscript,
    interimTranscript,
    error,
    start,
    stop,
    reset
  }
}
