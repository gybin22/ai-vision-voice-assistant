<template>
  <main class="history-page">
    <section class="history-card">
      <header class="history-header">
        <div>
          <span class="eyebrow">History</span>
          <h1>会话历史</h1>
          <p>这里只保存文字对话、模型与 token 元数据，不保存摄像头图片帧。</p>
        </div>
        <div class="history-header-actions">
          <button class="link-button" @click="emit('back')">返回个人中心</button>
          <button class="danger-button" :disabled="loading || days.length === 0" @click="confirmClearAll">
            清空历史
          </button>
        </div>
      </header>

      <div v-if="error" class="notice notice-warning">{{ error }}</div>
      <div v-if="successText" class="notice notice-info">{{ successText }}</div>

      <div class="history-layout">
        <aside class="history-list-panel">
          <div class="history-list-toolbar">
            <strong>按日期</strong>
            <button class="link-button" :disabled="loading" @click="loadHistory">
              {{ loading ? '刷新中...' : '刷新' }}
            </button>
          </div>

          <div v-if="loading && days.length === 0" class="history-empty">正在加载历史记录...</div>
          <div v-else-if="days.length === 0" class="history-empty">暂无历史会话。</div>

          <section v-for="day in days" :key="day.date" class="history-day-group">
            <h2>{{ day.label }}</h2>
            <button
              v-for="session in day.sessions"
              :key="session.sessionId"
              class="history-session-item"
              :class="{ active: selectedSessionId === session.sessionId }"
              @click="selectSession(session.sessionId)"
            >
              <span>{{ session.title || '新的对话' }}</span>
              <small>{{ formatTime(session.lastMessageAt) }} · {{ session.messageCount }} 条</small>
              <em>{{ session.lastMessagePreview || '暂无摘要' }}</em>
            </button>
          </section>
        </aside>

        <section class="history-detail-panel">
          <div v-if="!selectedDetail" class="history-empty detail-empty">
            从左侧选择一个会话查看完整对话记录。
          </div>

          <template v-else>
            <div class="history-detail-header">
              <div>
                <span class="eyebrow">Session</span>
                <h2>{{ selectedDetail.session.title || '新的对话' }}</h2>
                <p>{{ formatDateTime(selectedDetail.session.lastMessageAt) }} · {{ selectedDetail.session.messageCount }} 条消息</p>
              </div>
              <button class="danger-button ghost" :disabled="loadingDetail" @click="confirmDeleteSession(selectedDetail.session.sessionId)">
                删除该会话
              </button>
            </div>

            <div class="history-messages">
              <article
                v-for="message in selectedDetail.messages"
                :key="message.id"
                class="history-message"
                :class="message.role"
              >
                <div class="history-message-role">{{ message.role === 'user' ? '我' : 'AI' }}</div>
                <div class="history-message-body">
                  <p>{{ message.content }}</p>
                  <small v-if="message.role === 'assistant'">
                    {{ message.modelName || '模型' }} · 模型 {{ formatTokens(message.totalTokens) }} tokens · 扣减 {{ formatTokens(message.chargedTokens) }} tokens · {{ formatTime(message.createdAt) }}
                  </small>
                  <small v-else>{{ formatTime(message.createdAt) }}</small>
                </div>
              </article>
            </div>
          </template>
        </section>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as historyApi from '@/services/historyApi'
import type { ChatHistoryDay, ChatSessionDetail } from '@/types/chat'

const emit = defineEmits<{ (e: 'back'): void }>()
const days = ref<ChatHistoryDay[]>([])
const selectedSessionId = ref('')
const selectedDetail = ref<ChatSessionDetail | null>(null)
const loading = ref(false)
const loadingDetail = ref(false)
const error = ref('')
const successText = ref('')

onMounted(() => {
  void loadHistory()
})

async function loadHistory() {
  error.value = ''
  successText.value = ''
  loading.value = true
  try {
    days.value = await historyApi.listChatHistory()
    if (selectedSessionId.value && !findSession(selectedSessionId.value)) {
      selectedSessionId.value = ''
      selectedDetail.value = null
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '历史记录加载失败。'
  } finally {
    loading.value = false
  }
}

async function selectSession(sessionId: string) {
  if (selectedSessionId.value === sessionId && selectedDetail.value) return
  selectedSessionId.value = sessionId
  selectedDetail.value = null
  error.value = ''
  successText.value = ''
  loadingDetail.value = true
  try {
    selectedDetail.value = await historyApi.getChatSessionDetail(sessionId)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '会话详情加载失败。'
  } finally {
    loadingDetail.value = false
  }
}

async function confirmClearAll() {
  if (!window.confirm('确定清空全部历史会话吗？该操作只删除文字记录，不影响账号和 Tokens。')) return
  error.value = ''
  successText.value = ''
  loading.value = true
  try {
    const result = await historyApi.clearChatHistory()
    days.value = []
    selectedSessionId.value = ''
    selectedDetail.value = null
    successText.value = `已清空 ${result.deletedSessions} 个会话、${result.deletedMessages} 条消息。`
  } catch (e) {
    error.value = e instanceof Error ? e.message : '清空历史失败。'
  } finally {
    loading.value = false
  }
}

async function confirmDeleteSession(sessionId: string) {
  if (!window.confirm('确定删除该会话记录吗？')) return
  error.value = ''
  successText.value = ''
  loadingDetail.value = true
  try {
    const result = await historyApi.deleteChatSession(sessionId)
    successText.value = `已删除 ${result.deletedSessions} 个会话、${result.deletedMessages} 条消息。`
    selectedSessionId.value = ''
    selectedDetail.value = null
    await loadHistory()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '删除会话失败。'
  } finally {
    loadingDetail.value = false
  }
}

function findSession(sessionId: string) {
  return days.value.flatMap(day => day.sessions).find(session => session.sessionId === sessionId)
}

function formatTokens(value: number) {
  return Math.floor(value || 0).toLocaleString('zh-CN')
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}
</script>
