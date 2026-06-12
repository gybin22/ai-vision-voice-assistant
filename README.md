# AI 视觉语音对话助手 MVP

技术栈：Vue 3 + TypeScript + Vite 前端，Spring Boot 后端。

本项目实现一个最小可交付版本：浏览器打开摄像头和麦克风，用户通过文字或语音提问，系统截取摄像头当前帧并发送到后端，后端调用多模态 AI 模型回答，前端展示并语音播报答案。

## 功能清单

- 摄像头预览：`navigator.mediaDevices.getUserMedia`
- 麦克风授权：与摄像头一起申请
- 文字提问：输入框提交
- 语音提问：优先使用浏览器 Web Speech API，不支持时降级为文字输入
- 当前帧截图：`video` -> `canvas` -> `Blob`
- 图片压缩：最长边限制 + JPEG 质量控制
- 后端多模态问答：支持 Mock 和 OpenAI-Compatible Provider
- 浏览器语音播报：`SpeechSynthesisUtterance`
- 成本控制：限频、图片大小限制、问题长度限制、输出 token 限制、短上下文、缓存、Mock/降级配置

## 目录

```text
ai-vision-voice-assistant/
├─ frontend/        # Vue3 + TS + Vite
├─ backend/         # Spring Boot
└─ docs/            # 设计文档、接口文档、成本控制文档
```

## 启动后端

进入后端目录：

```bash
cd backend
```

默认使用 Mock 模型，不需要 API Key：

```bash
./mvnw spring-boot:run
```

如果本地没有 Maven Wrapper，可使用：

```bash
mvn spring-boot:run
```

后端默认端口：`http://localhost:8080`

健康检查：

```bash
curl http://localhost:8080/api/health
```

### 接入真实多模态模型

后端使用 OpenAI-Compatible Chat Completions 结构。配置环境变量：

```bash
export ASSISTANT_MODEL_PROVIDER=openai-compatible
export ASSISTANT_MODEL_BASE_URL=https://api.openai.com/v1
export ASSISTANT_MODEL_API_KEY=你的APIKey
export ASSISTANT_MODEL_MODEL_NAME=gpt-4o-mini
```

或修改 `backend/src/main/resources/application.yml`。

> 注意：API Key 只允许放后端环境变量，不能放前端。

## 启动前端

进入前端目录：

```bash
cd frontend
npm install
npm run dev
```

前端默认端口：`http://localhost:5173`

Vite 已配置 `/api` 代理到 `http://localhost:8080`。

## 使用流程

1. 启动后端。
2. 启动前端。
3. 浏览器打开 `http://localhost:5173`。
4. 点击“启动摄像头和麦克风”。
5. 输入文字问题，或点击“开始语音输入”。
6. 点击“发送”。
7. 前端截取当前画面并上传。
8. 后端调用模型返回答案。
9. 前端展示并语音播报答案。

## 成本控制默认值

后端默认配置：

```yaml
assistant:
  cost:
    max-image-bytes: 819200
    max-question-length: 500
    max-output-tokens: 500
    min-session-interval-ms: 10000
    max-session-requests-per-day: 30
    max-ip-requests-per-day: 100
    cache-ttl-seconds: 600
```

前端默认压缩：

```text
最长边：768px
JPEG 质量：0.72
```

## 设计文档

见：

- `docs/design.md`
- `docs/api.md`
- `docs/cost-control.md`
- `docs/risk.md`

## 生产部署注意事项

- 前端必须通过 HTTPS 访问，否则摄像头/麦克风权限在多数浏览器中不可用或受限。
- 后端必须开启 CORS 白名单，不要在生产中使用 `*`。
- 不要保存用户上传的摄像头图片。
- 日志中不要打印原始图片 Base64 或 API Key。
- 如果接入真实模型，必须配置限流、配额和监控。
