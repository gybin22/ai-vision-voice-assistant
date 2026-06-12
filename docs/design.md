# 设计文档：AI 视觉语音对话助手 MVP

## 1. 项目目标

开发一款与 AI 对话的应用。浏览器打开摄像头与麦克风，用户可以通过语音或文字提问。系统在用户提交问题时截取摄像头当前帧，后端调用多模态 AI 模型生成回答，前端展示答案并使用浏览器语音合成播报。

MVP 不实现连续视频流理解，而采用“用户主动提问时截取当前帧”的模式，降低工程复杂度和模型调用成本。

## 2. 用户故事

| 编号 | 用户故事 | 计划实现 | MVP 实现 |
|---|---|---:|---:|
| US-01 | 用户打开页面后可以授权摄像头和麦克风 | 是 | 是 |
| US-02 | 用户可以看到摄像头实时预览 | 是 | 是 |
| US-03 | 用户可以输入文字问题 | 是 | 是 |
| US-04 | 用户可以通过语音输入问题 | 是 | 是，浏览器不支持时降级 |
| US-05 | 系统在用户提问时截取当前画面 | 是 | 是 |
| US-06 | AI 能结合画面与问题回答 | 是 | 是 |
| US-07 | 前端可以语音播报答案 | 是 | 是 |
| US-08 | 系统可以控制运营成本 | 是 | 是 |
| US-09 | 支持多轮上下文 | 是 | 部分实现，只保留最近 3 轮 |
| US-10 | 支持实时连续视频理解 | 否 | 否 |

## 3. 技术架构

```text
Vue3 Frontend
 ├─ Camera Preview
 ├─ Speech Recognition
 ├─ Frame Capture
 ├─ API Client
 └─ Speech Synthesis

Spring Boot Backend
 ├─ ChatController
 ├─ VisionChatService
 ├─ CostControlService
 ├─ ImagePreprocessService
 ├─ ConversationService
 ├─ CacheService
 └─ MultimodalModelClient

Multimodal AI Provider
 ├─ Mock Client
 └─ OpenAI-Compatible Client
```

## 4. 核心流程

```text
用户点击发送
 → 前端校验问题文本
 → 前端从 video 截取当前帧
 → 前端压缩为 JPEG Blob
 → 前端 multipart/form-data 上传
 → 后端限流与参数校验
 → 后端检查缓存
 → 后端拼接 prompt 和短上下文
 → 后端调用多模态模型
 → 后端记录用量
 → 前端展示答案
 → 前端调用 SpeechSynthesis 播报答案
```

## 5. 模型调用策略

后端通过 `MultimodalModelClient` 接口隔离模型供应商。

- `MockVisionClient`：用于本地开发和演示，无成本。
- `OpenAiCompatibleVisionClient`：用于真实多模态模型调用。

模型配置通过环境变量管理，避免前端泄露 API Key。

## 6. 最终实现说明

本 MVP 已实现：

- Vue3 页面框架；
- 摄像头与麦克风权限请求；
- 视频预览；
- 当前帧截图；
- 前端图片压缩；
- 文字输入；
- 语音输入；
- 后端 multipart 接口；
- Mock 多模态回答；
- OpenAI-Compatible 多模态客户端；
- 限流、缓存、配额、图片大小限制；
- 浏览器语音播报；
- 设计文档和接口文档。

未实现：

- 连续视频流上传；
- WebRTC 双工实时对话；
- 用户登录与计费；
- 向量库语义缓存；
- 私有化端侧视觉模型。
