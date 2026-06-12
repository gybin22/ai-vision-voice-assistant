import { ref } from 'vue'

export function useSpeechRecognition() {
  const isSupported = Boolean(window.SpeechRecognition || window.webkitSpeechRecognition)
  const isListening = ref(false)
  const transcript = ref('')
  const error = ref('')
  let recognition: SpeechRecognition | null = null

  function createRecognition() {
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!Recognition) return null

    const instance = new Recognition()
    instance.lang = 'zh-CN'
    instance.continuous = false
    instance.interimResults = true
    instance.maxAlternatives = 1

    instance.onresult = event => {
      let text = ''
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        text += event.results[i][0].transcript
      }
      transcript.value = text.trim()
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

  function start() {
    error.value = ''
    if (!isSupported) {
      error.value = '当前浏览器不支持语音识别，请使用文字输入。'
      return
    }

    if (!recognition) recognition = createRecognition()
    transcript.value = ''
    recognition?.start()
    isListening.value = true
  }

  function stop() {
    recognition?.stop()
    isListening.value = false
  }

  return {
    isSupported,
    isListening,
    transcript,
    error,
    start,
    stop
  }
}
