# API 文档

本文档描述最终项目的主要后端接口。除注册、登录、刷新和健康检查外，其余业务接口默认需要携带：

```http
Authorization: Bearer <accessToken>
```

---

## 1. 健康检查

### `GET /api/health`

响应示例：

```json
{
  "status": "ok",
  "timestamp": "2026-06-14T10:00:00Z"
}
```

---

## 2. 认证接口

### 2.1 注册

`POST /api/auth/register`

请求：

```json
{
  "email": "demo@example.com",
  "password": "password123",
  "nickname": "Demo 用户"
}
```

响应：

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresInSeconds": 1800,
  "refreshExpiresInSeconds": 1209600,
  "user": {
    "id": 1,
    "email": "demo@example.com",
    "nickname": "Demo 用户",
    "avatarUrl": null,
    "status": "ACTIVE",
    "createdAt": "2026-06-14T10:00:00Z",
    "updatedAt": "2026-06-14T10:00:00Z"
  }
}
```

### 2.2 登录

`POST /api/auth/login`

请求：

```json
{
  "email": "demo@example.com",
  "password": "password123"
}
```

响应同注册。

### 2.3 刷新 Token

`POST /api/auth/refresh`

请求：

```json
{
  "refreshToken": "eyJ..."
}
```

响应同登录。

### 2.4 退出登录

`POST /api/auth/logout`

响应：

```json
{
  "loggedOut": true
}
```

---

## 3. 用户资料接口

### 3.1 当前用户

`GET /api/users/me`

响应：

```json
{
  "id": 1,
  "email": "demo@example.com",
  "nickname": "Demo 用户",
  "avatarUrl": null,
  "status": "ACTIVE",
  "createdAt": "2026-06-14T10:00:00Z",
  "updatedAt": "2026-06-14T10:00:00Z"
}
```

### 3.2 修改用户资料

`PUT /api/users/me`

请求：

```json
{
  "nickname": "新的昵称",
  "avatarUrl": "https://example.com/avatar.png"
}
```

响应同当前用户。

---

## 4. 视觉语音对话接口

### `POST /api/chat/vision`

请求类型：`multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `sessionId` | string | 是 | 前端生成的会话 ID |
| `question` | string | 是 | 用户问题，实时语音模式中为识别后的文本 |
| `images` | file[] | 否 | 最近 15 秒视觉帧，推荐字段 |
| `image` | file | 否 | 兼容旧版单图上传 |
| `visualSummary` | string | 否 | 前端视觉摘要，当前可为空 |
| `enableHistory` | boolean | 否 | 是否启用短上下文，默认 true |
| `maxOutputTokens` | number | 否 | 最大输出 tokens，默认 500 |
| `clientImageWidth` | number | 否 | 前端压缩后图片宽度 |
| `clientImageHeight` | number | 否 | 前端压缩后图片高度 |
| `frameMetadata` | string | 否 | 视觉帧元数据 JSON，包含顺序和时间偏移 |

成功响应：

```json
{
  "requestId": "req_6f2a9c1b8e34a012",
  "sessionId": "session_abc",
  "answer": "看起来是手机，正拿在你手里。",
  "model": "qwen3-vl-plus",
  "cached": false,
  "usage": {
    "inputTokens": 1580,
    "outputTokens": 42,
    "totalTokens": 1622,
    "imageBytes": 734218,
    "providerCostAmountYuan": 0.0004
  },
  "billing": {
    "chargedTokens": 5000,
    "balanceAfterTokens": 95000,
    "tokenUnitPriceYuan": 0.00001,
    "revenueAmountYuan": 0.05,
    "providerCostAmountYuan": 0.0004,
    "grossProfitAmountYuan": 0.0496
  },
  "latencyMs": 2100
}
```

说明：

- 成功请求会扣减平台 Tokens；
- 失败请求记录日志，但不扣费；
- 当前版本要求上传视觉帧；
- 图片不保存到数据库。

---

## 5. Session 用量接口

### `GET /api/usage/session/{sessionId}`

响应：

```json
{
  "sessionId": "session_abc",
  "requestCount": 3,
  "requestLimit": 30,
  "estimatedCost": 0.0012,
  "remainingRequests": 27
}
```

---

## 6. 清空短上下文

### `DELETE /api/conversation/{sessionId}`

说明：该接口清理内存中的短上下文，不等同于删除历史记录表中的会话历史。

响应：

```json
{
  "sessionId": "session_abc",
  "deleted": true
}
```

---

## 7. Tokens 接口

### 7.1 查询余额

`GET /api/tokens/balance`

响应：

```json
{
  "balanceTokens": 100000,
  "totalRechargedTokens": 100000,
  "totalUsedTokens": 0
}
```

### 7.2 模拟充值

`POST /api/tokens/recharge`

请求：

```json
{
  "amountTokens": 50000
}
```

响应：

```json
{
  "balance": {
    "balanceTokens": 150000,
    "totalRechargedTokens": 150000,
    "totalUsedTokens": 0
  },
  "addedTokens": 50000,
  "transactionId": 12
}
```

注意：当前为开发版模拟充值。正式上线应改成创建订单、支付平台回调、验签成功后再到账。

---

## 8. 会话历史接口

### 8.1 按日期获取历史会话

`GET /api/history`

响应：

```json
[
  {
    "date": "2026-06-14",
    "label": "今天",
    "sessions": [
      {
        "sessionId": "session_abc",
        "title": "你能看到我吗？",
        "messageCount": 6,
        "lastMessagePreview": "看起来是手机，正拿在你手里。",
        "createdAt": "2026-06-14T10:00:00Z",
        "updatedAt": "2026-06-14T10:05:00Z",
        "lastMessageAt": "2026-06-14T10:05:00Z"
      }
    ]
  }
]
```

### 8.2 获取单个会话详情

`GET /api/history/sessions/{sessionId}`

响应：

```json
{
  "session": {
    "sessionId": "session_abc",
    "title": "你能看到我吗？",
    "messageCount": 2,
    "lastMessagePreview": "能看到。你现在看起来比较平静。",
    "createdAt": "2026-06-14T10:00:00Z",
    "updatedAt": "2026-06-14T10:01:00Z",
    "lastMessageAt": "2026-06-14T10:01:00Z"
  },
  "messages": [
    {
      "id": 1,
      "role": "user",
      "content": "你能看到我吗？",
      "requestId": "req_6f2a9c1b8e34a012",
      "modelName": "qwen3-vl-plus",
      "inputTokens": 1580,
      "outputTokens": 42,
      "totalTokens": 1622,
      "chargedTokens": 5000,
      "createdAt": "2026-06-14T10:00:00Z"
    },
    {
      "id": 2,
      "role": "assistant",
      "content": "能看到。你现在看起来比较平静。",
      "requestId": "req_6f2a9c1b8e34a012",
      "modelName": "qwen3-vl-plus",
      "inputTokens": 1580,
      "outputTokens": 42,
      "totalTokens": 1622,
      "chargedTokens": 5000,
      "createdAt": "2026-06-14T10:00:02Z"
    }
  ]
}
```

### 8.3 删除单个会话历史

`DELETE /api/history/sessions/{sessionId}`

响应：

```json
{
  "deletedSessions": 1,
  "deletedMessages": 6
}
```

### 8.4 清空全部历史

`DELETE /api/history`

响应：

```json
{
  "deletedSessions": 3,
  "deletedMessages": 18
}
```

---

## 9. 常见错误

| HTTP 状态 | 可能原因 | 说明 |
|---|---|---|
| 400 | 参数错误 | 问题为空、图片为空、图片格式不支持 |
| 401 | 未登录或模型认证失败 | 前端接口 401 多为 JWT 失效；模型调用 401 多为 API Key、profile 或上游网关问题 |
| 403 | 权限不足 | 用户状态异常或鉴权失败 |
| 413 | 请求体过大 | 图片或 multipart 总体积超过限制 |
| 429 | 请求过于频繁 | session/IP 触发限流 |
| 500 | 模型服务或后端异常 | 记录 requestId 后排查日志 |

---

## 10. 接口安全要求

- 注册、登录、刷新和健康检查之外，业务接口必须携带 JWT；
- 模型 API Key 只允许在后端环境变量中配置；
- 前端不得直接请求 qwen/DashScope；
- 生产环境必须使用 HTTPS；
- 模拟充值接口不能直接用于生产环境。
