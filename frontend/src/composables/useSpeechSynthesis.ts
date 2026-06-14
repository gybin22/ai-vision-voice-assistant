import { ref } from 'vue'

export interface SpeechSynthesisSpeakOptions {
  onStart?: () => void
  onEnd?: () => void
  onError?: () => void
}

export function useSpeechSynthesis() {
  const isSupported = Boolean(window.speechSynthesis)
  const isSpeaking = ref(false)

  function speak(text: string, options: SpeechSynthesisSpeakOptions = {}) {
    if (!isSupported || !text.trim()) return false

    stop()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'zh-CN'
    utterance.rate = 1
    utterance.pitch = 1
    utterance.volume = 1
    utterance.onstart = () => {
      isSpeaking.value = true
      options.onStart?.()
    }
    utterance.onend = () => {
      isSpeaking.value = false
      options.onEnd?.()
    }
    utterance.onerror = () => {
      isSpeaking.value = false
      options.onError?.()
    }

    window.speechSynthesis.speak(utterance)
    return true
  }

  function stop() {
    if (!isSupported) return
    window.speechSynthesis.cancel()
    isSpeaking.value = false
  }

  return {
    isSupported,
    isSpeaking,
    speak,
    stop
  }
}
