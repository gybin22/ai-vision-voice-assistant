<template>
  <button
    class="btn"
    :class="isListening ? 'btn-danger' : 'btn-ghost'"
    :disabled="disabled || !supported"
    @click="toggle"
  >
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
  if (!props.supported) return '不支持语音输入'
  return props.isListening ? '停止语音输入' : '开始语音输入'
})

function toggle() {
  if (props.isListening) {
    emit('stop')
  } else {
    emit('start')
  }
}
</script>
