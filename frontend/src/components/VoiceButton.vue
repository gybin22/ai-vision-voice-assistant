<template>
  <button
    class="voice-button"
    :class="{ listening: isListening }"
    :disabled="disabled || !supported"
    @click="toggle"
  >
    <span>{{ isListening ? '●' : '🎙' }}</span>
    {{ label }}
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  isListening: boolean
  supported: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  start: []
  stop: []
}>()

const label = computed(() => {
  if (!props.supported) return '不支持语音'
  return props.isListening ? '停止语音' : '语音提问'
})

function toggle() {
  if (props.isListening) {
    emit('stop')
  } else {
    emit('start')
  }
}
</script>
