export function getBrowserSupport() {
  return {
    mediaDevices: Boolean(navigator.mediaDevices?.getUserMedia),
    speechRecognition: Boolean(window.SpeechRecognition || window.webkitSpeechRecognition),
    speechSynthesis: Boolean(window.speechSynthesis)
  }
}
