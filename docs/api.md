# API 文档

## 1. 健康检查

### `GET /api/health`

响应：

```json
{
  "status": "ok",
  "timestamp": "2026-06-12T10:00:00Z"
}
```

## 2. 视觉问答

### `POST /api/chat/vision`

请求类型：`multipart/form-data`

字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| sessionId | string | 是 | 前端生成的会话 ID |
| question | string | 是 | 用户问题 |
| image | file | 是 | 当前帧图片，JPEG/WebP/PNG |
| inputType | string | 否 | text 或 voice |
| enableHistory | boolean | 否 | 是否启用短上下文 |
| maxOutputTokens | number | 否 | 最大输出 token |
| clientImageWidth | number | 否 | 前端压缩后图片宽度 |
| clientImageHeight | number | 否 | 前端压缩后图片高度 |

成功响应：

```json
{
  "requestId": "req_20260612_000001",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "answer": "画面中可以看到一张桌子，上面有水杯和笔记本电脑。",
  "model": "mock-vision-model",
  "cached": false,
  "usage": {
    "inputTokens": 900,
    "outputTokens": 40,
    "imageBytes": 82341,
    "estimatedCost": 0.0
  },
  "latencyMs": 150
}
```

错误响应：

```json
{
  "requestId": "req_20260612_000002",
  "code": "IMAGE_TOO_LARGE",
  "message": "图片过大，请降低摄像头截图分辨率后重试。"
}
```

## 3. 会话用量

### `GET /api/usage/session/{sessionId}`

响应：

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "requestCount": 8,
  "requestLimit": 30,
  "estimatedCost": 0.021,
  "remainingRequests": 22
}
```

## 4. 清空会话

### `DELETE /api/conversation/{sessionId}`

响应：

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "deleted": true
}
```
