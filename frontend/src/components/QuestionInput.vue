<template>
  <div class="input-panel">
    <textarea
      :value="modelValue"
      placeholder="输入问题，例如：请描述你看到的内容。"
      maxlength="500"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
      @keydown.ctrl.enter.prevent="$emit('submit')"
    />
    <div class="meta">{{ modelValue.length }}/500，Ctrl + Enter 可发送</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
  submit: []
}>()
</script>
