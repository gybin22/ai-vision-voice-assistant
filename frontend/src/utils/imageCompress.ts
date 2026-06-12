export interface ImageCompressionOptions {
  maxLongSide: number
  quality: number
  mimeType: 'image/jpeg' | 'image/webp'
}

export interface CapturedImage {
  blob: Blob
  width: number
  height: number
}

export async function canvasToBlob(
  canvas: HTMLCanvasElement,
  options: ImageCompressionOptions
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      blob => {
        if (!blob) {
          reject(new Error('无法生成图片 Blob'))
          return
        }
        resolve(blob)
      },
      options.mimeType,
      options.quality
    )
  })
}

export function calcTargetSize(width: number, height: number, maxLongSide: number) {
  const longSide = Math.max(width, height)
  if (longSide <= maxLongSide) {
    return { width, height }
  }
  const ratio = maxLongSide / longSide
  return {
    width: Math.round(width * ratio),
    height: Math.round(height * ratio)
  }
}
