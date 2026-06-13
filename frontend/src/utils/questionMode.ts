import type { QuestionMode } from '@/types/chat'

/**
 * 保留这个工具文件只是为了兼容旧组件导入。
 * 当前版本不再做问题分类：所有问题统一走 rolling 视觉上下文。
 */
export function classifyQuestionMode(): QuestionMode {
  return 'rolling'
}

export function questionModeLabel(): string {
  return '最近 15 秒视觉上下文'
}

export function questionModeUploadHint(): string {
  return '每次提问上传最近 15 秒、约 1fps 的视觉抽帧'
}
