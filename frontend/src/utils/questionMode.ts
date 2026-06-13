import type { QuestionMode } from '@/types/chat'

const DETAILED_KEYWORDS = [
  '详细', '仔细', '完整', '全面', '分析', '逐步', '每一步', '全过程', '过程', '解释一下'
]

const MOTION_KEYWORDS = [
  '刚才', '刚刚', '之前', '发生了什么', '做了什么', '动作', '变化', '移动', '动了',
  '拿起', '放下', '举起', '靠近', '离开', '转动', '打开', '关闭', '过程', '先后'
]

const CURRENT_VISUAL_KEYWORDS = [
  '看到', '看见', '能看', '画面', '镜头', '摄像头', '手里', '拿的', '拿着', '这是什么',
  '是什么', '什么东西', '颜色', '几个', '哪里', '现在', '当前', '面前', '桌上', '旁边'
]

const CHAT_ONLY_HINTS = [
  '你好', '在吗', '你是谁', '你叫什么', '你能做什么', '谢谢', '再见', '早上好', '晚上好'
]

export function classifyQuestionMode(question: string): QuestionMode {
  const normalized = question.trim().toLowerCase()
  if (!normalized) return 'chat'

  if (CHAT_ONLY_HINTS.some(keyword => normalized === keyword || normalized.includes(keyword))) {
    return 'chat'
  }

  const wantsDetailed = DETAILED_KEYWORDS.some(keyword => normalized.includes(keyword))
  const asksMotion = MOTION_KEYWORDS.some(keyword => normalized.includes(keyword))
  const asksCurrentVisual = CURRENT_VISUAL_KEYWORDS.some(keyword => normalized.includes(keyword))

  if (asksMotion && wantsDetailed) return 'detailed'
  if (asksMotion) return 'motion'
  if (asksCurrentVisual && wantsDetailed) return 'detailed'
  if (asksCurrentVisual) return 'current'
  return 'chat'
}

export function questionModeLabel(mode: QuestionMode): string {
  switch (mode) {
    case 'chat':
      return '普通聊天'
    case 'current':
      return '当前画面'
    case 'motion':
      return '动作变化'
    case 'detailed':
      return '详细视觉分析'
    default:
      return '普通聊天'
  }
}

export function questionModeUploadHint(mode: QuestionMode): string {
  switch (mode) {
    case 'chat':
      return '不上传图片'
    case 'current':
      return '上传当前帧'
    case 'motion':
      return '上传事件关键帧'
    case 'detailed':
      return '上传更多视觉证据'
    default:
      return '不上传图片'
  }
}
