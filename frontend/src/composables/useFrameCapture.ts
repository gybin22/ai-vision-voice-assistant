import { canvasToBlob, calcTargetSize, type CapturedImage } from '@/utils/imageCompress'

export interface CaptureOptions {
  maxLongSide?: number
  quality?: number
  mimeType?: 'image/jpeg' | 'image/webp'
}

export function useFrameCapture() {
  async function capture(video: HTMLVideoElement, options: CaptureOptions = {}): Promise<CapturedImage> {
    const sourceWidth = video.videoWidth
    const sourceHeight = video.videoHeight

    if (!sourceWidth || !sourceHeight) {
      throw new Error('摄像头画面尚未准备好')
    }

    const maxLongSide = options.maxLongSide ?? 768
    const quality = options.quality ?? 0.72
    const mimeType = options.mimeType ?? 'image/jpeg'
    const target = calcTargetSize(sourceWidth, sourceHeight, maxLongSide)

    const canvas = document.createElement('canvas')
    canvas.width = target.width
    canvas.height = target.height

    const ctx = canvas.getContext('2d')
    if (!ctx) {
      throw new Error('当前浏览器无法创建 Canvas 上下文')
    }

    ctx.drawImage(video, 0, 0, target.width, target.height)
    const blob = await canvasToBlob(canvas, { maxLongSide, quality, mimeType })

    return {
      blob,
      width: target.width,
      height: target.height
    }
  }

  return { capture }
}
