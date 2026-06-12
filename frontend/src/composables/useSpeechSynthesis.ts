import { ref } from 'vue'

export function useSpeechSynthesis() {
  const isSupported = Boolean(window.speechSynthesis)
  const isSpeaking = ref(false)

  function speak(text: string) {
    if (!isSupported || !text.trim()) return

    stop()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'zh-CN'
    utterance.rate = 1
    utterance.pitch = 1
    utterance.volume = 1
    utterance.onstart = () => {
      isSpeaking.value = true
    }
    utterance.onend = () => {
      isSpeaking.value = false
    }
    utterance.onerror = () => {
      isSpeaking.value = false
    }

    window.speechSynthesis.speak(utterance)
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
