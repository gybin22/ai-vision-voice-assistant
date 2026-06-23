# AI 视觉语音对话助手


一个基于 **Vue 3 + TypeScript + Vite + Spring Boot + MySQL 8** 的 AI 视频语音对话应用。用户可以通过浏览器摄像头和麦克风与 AI 进行接近视频通话式的交互：前端持续维护最近 15 秒视觉上下文，用户语音停顿后自动提交问题，后端调用多模态模型生成回答，并结合平台 Tokens 体系完成余额校验、用量扣减、成本核算和历史记录保存。

本项目重点综合考虑：

- 视觉内容理解准确性：通过最近 15 秒低频抽帧，而不是只看单张图片；
- 语音交互自然度与流畅性：支持实时语音识别、停顿自动提交、AI 播报和播报期间暂停识别；
- 端云协同成本控制：前端压缩与限帧，后端限流、限额、缓存、按 usage 计费和失败不扣费。

---

## 1. 核心功能

### 1.1 AI 视频语音对话

- 浏览器摄像头实时预览；
- 浏览器麦克风语音识别；
- 实时对话模式：用户点击“开始实时对话”后，系统持续监听语音；
- 用户停顿约 1 秒后自动提交，无需每次点击发送；
- AI 回答自动语音播报；
- AI 播报期间暂停语音识别，避免把助手回答再次识别成用户输入；
- 保留手动输入模式作为兜底。

### 1.2 最近 15 秒视觉上下文

- 前端每秒采样 1 帧；
- 最多保留最近 15 帧，形成滑动窗口；
- 每次提问时上传最近视觉帧，并补采发送瞬间当前帧；
- 后端将多帧按时间顺序传给多模态模型；
- 用户问“当前状态”时优先参考最后一帧；
- 用户问“刚才发生了什么”时按帧顺序理解动作变化；
- 图片只作为本次请求输入，不保存到数据库。

### 1.3 qwen3-vl-plus 多模态模型接入

- 后端通过 OpenAI-Compatible Chat Completions 方式调用模型；
- 默认可使用 Mock 模型进行本地开发；
- qwen profile 支持阿里云百炼 DashScope：`qwen3-vl-plus`；
- API Key 仅保存在后端环境变量，前端不接触模型密钥。

### 1.4 用户体系

- 邮箱 + 密码注册；
- 登录；
- JWT access token；
- refresh token；
- 退出登录；
- 用户资料查看和修改；
- 前端自动携带 Bearer Token，并在 401 时尝试 refresh。

### 1.5 平台 Tokens 充值与扣费

- 用户拥有平台 Tokens 余额；
- 个人中心支持模拟充值套餐；
- 每次 AI 视频对话前检查余额；
- 模型调用成功后，根据真实 usage 计算平台 Tokens 扣减；
- 记录 input tokens、output tokens、total tokens；
- 记录平台收入、模型真实成本和毛利；
- 模型调用失败时记录失败日志，不扣 Tokens。

平台扣费公式：

```text
platform_tokens_used = max(
  min_visual_charge,
  input_tokens × input_multiplier + output_tokens × output_multiplier
)
```

默认配置：

```yaml
assistant:
  billing:
    token-unit-price-yuan: 0.00001
    input-token-multiplier: 1
    output-token-multiplier: 4
    min-visual-charge-tokens: 5000
```

### 1.6 会话历史

- 个人中心进入会话历史；
- 按日期分组展示历史会话；
- 查看单个会话详情；
- 删除单个会话；
- 清空全部历史；
- 只保存文字问题、AI 回答、模型名、token 用量、扣减 Tokens 等元数据；
- 不保存用户图片帧。

---

## 2. 技术栈

### 前端

- Vue 3
- TypeScript
- Vite
- Web Speech API：语音识别
- SpeechSynthesis：语音播报
- Canvas：视频帧采样和图片压缩
- Fetch API：请求后端接口

### 后端

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Security
- Spring Data JPA
- MySQL 8
- JWT：jjwt
- Caffeine Cache
- OpenAI-Compatible HTTP Client

### 数据库

MySQL 8，主要表：

- `users`
- `user_token_accounts`
- `token_transactions`
- `ai_request_logs`
- `chat_sessions`
- `chat_messages`

---

## 3. 项目结构

以下结构按最终项目代码整理，省略了 `.git/`、`.idea/`、`node_modules/`、`target/` 等本地开发或构建产物。

```text
ai-vision-voice-assistant/
├─ README.md                              # 项目说明、启动方式、Demo 流程
├─ docker-compose.yml                     # MySQL 等本地容器环境
├─ .gitignore                             # Git 忽略规则
│
├─ docs/                                  # 提交文档
│  ├─ design.md                           # 设计文档：用户故事、最终实现、核心流程
│  ├─ cost-control.md                     # 成本控制策略：想到的方案与实际采用方案
│  ├─ api.md                              # 后端接口说明
│  ├─ risk.md                             # 风险、限制与后续优化
│  └─ project-structure.md                # 完整项目结构说明
│
├─ frontend/                              # Vue 3 + TypeScript + Vite 前端
│  ├─ package.json                        # 前端依赖与 npm scripts
│  ├─ package-lock.json                   # npm 锁定文件
│  ├─ index.html                          # Vite 入口 HTML
│  ├─ vite.config.ts                      # Vite 配置与 /api 代理
│  ├─ tsconfig.json                       # TypeScript 配置
│  ├─ .env.example                        # 前端环境变量示例
│  │
│  └─ src/
│     ├─ main.ts                          # 前端应用入口
│     ├─ App.vue                          # 根组件与页面切换容器
│     ├─ style.css                        # 全局样式、对话页、充值弹窗、历史页样式
│     ├─ vite-env.d.ts                    # Vite 类型声明
│     │
│     ├─ views/                           # 页面级组件
│     │  ├─ AssistantView.vue             # AI 视频语音对话主界面
│     │  ├─ AuthView.vue                  # 登录 / 注册页面
│     │  ├─ ProfileView.vue               # 个人中心、Tokens 余额、充值入口
│     │  └─ HistoryView.vue               # 会话历史列表与详情
│     │
│     ├─ components/                      # 可复用 UI 组件
│     │  ├─ CameraPreview.vue             # 摄像头预览与 video 暴露方法
│     │  ├─ ChatMessages.vue              # 对话消息列表
│     │  ├─ CostStatusBar.vue             # 旧版成本状态组件，当前主界面弱化使用
│     │  ├─ KeyframePanel.vue             # 旧版关键帧面板，当前主界面不展示
│     │  ├─ QuestionInput.vue             # 手动输入组件
│     │  └─ VoiceButton.vue               # 手动语音按钮组件
│     │
│     ├─ composables/                     # 前端组合式逻辑
│     │  ├─ useAuth.ts                    # 登录态、用户资料、退出登录
│     │  ├─ useTokens.ts                  # Tokens 余额、充值、刷新余额
│     │  ├─ useMediaDevices.ts            # 摄像头 / 麦克风权限和媒体流管理
│     │  ├─ useRollingFrameBuffer.ts      # 最近 15 秒视觉帧滑动窗口
│     │  ├─ useSpeechRecognition.ts       # continuous 语音识别与实时转写
│     │  ├─ useSpeechSynthesis.ts         # AI 回答语音播报与打断
│     │  ├─ useFrameCapture.ts            # 单帧截图工具
│     │  └─ useKeyframeRecorder.ts        # 旧版关键帧变化检测逻辑，保留兼容
│     │
│     ├─ services/                        # 前端 API 封装
│     │  ├─ apiClient.ts                  # fetch 封装、Authorization、401 refresh
│     │  ├─ assistantApi.ts               # 多模态对话接口
│     │  ├─ authApi.ts                    # 注册、登录、刷新、退出、用户资料接口
│     │  ├─ authStorage.ts                # token 本地存储封装
│     │  ├─ tokenApi.ts                   # Tokens 余额与充值接口
│     │  └─ historyApi.ts                 # 会话历史接口
│     │
│     ├─ types/                           # 前端类型声明
│     │  ├─ chat.ts                       # 对话、usage、billing、历史相关类型
│     │  └─ speech.d.ts                   # Web Speech API 类型补充
│     │
│     └─ utils/                           # 前端工具函数
│        ├─ browserSupport.ts             # 浏览器能力检测
│        ├─ imageCompress.ts              # 图片压缩与 Blob 转换
│        ├─ questionMode.ts               # 旧版问题模式判断，当前仅保留兼容
│        └─ session.ts                    # 前端 sessionId 工具
│
└─ backend/                               # Spring Boot 3 + MySQL 8 后端
   ├─ pom.xml                             # Maven 依赖配置
   ├─ Dockerfile                          # 后端容器镜像构建文件
   ├─ .env.qwen                           # 本地 qwen 环境变量文件，生产不要提交真实 Key
   │
   └─ src/
      ├─ main/
      │  ├─ resources/
      │  │  ├─ application.yml            # 默认配置：mock 模型、MySQL、JWT、限额、计费
      │  │  ├─ application-qwen.yml       # qwen3-vl-plus profile 配置
      │  │  └─ db/
      │  │     └─ mysql-auth-schema.sql   # MySQL8 建库建表脚本
      │  │
      │  └─ java/com/example/assistant/
      │     ├─ AssistantApplication.java   # Spring Boot 启动类
      │     │
      │     ├─ controller/                # HTTP 接口层
      │     │  ├─ AuthController.java      # 注册、登录、刷新 token、退出
      │     │  ├─ UserController.java      # 当前用户资料查询与修改
      │     │  ├─ ChatController.java      # 多帧视觉语音对话接口
      │     │  ├─ TokenController.java     # Tokens 余额与模拟充值
      │     │  ├─ ChatHistoryController.java # 会话历史列表、详情、删除、清空
      │     │  └─ HealthController.java    # 健康检查
      │     │
      │     ├─ service/                   # 业务服务层
      │     │  ├─ VisionChatService.java   # 多模态对话主流程：校验、模型调用、扣费、历史写入
      │     │  ├─ CostControlService.java  # 限频、限帧、图片大小、问题长度控制
      │     │  ├─ ConversationService.java # 会话上下文管理
      │     │  ├─ CacheService.java        # 相似请求缓存
      │     │  ├─ ImagePreprocessService.java # 图片预处理入口
      │     │  ├─ auth/AuthService.java    # 注册登录、密码校验、refresh token
      │     │  ├─ billing/TokenBillingService.java # 余额、充值、usage 扣费和成本核算
      │     │  └─ history/ChatHistoryService.java  # 文本会话历史保存与查询
      │     │
      │     ├─ client/                    # 模型客户端适配层
      │     │  ├─ MultimodalModelClient.java # 多模态模型统一接口
      │     │  ├─ MockVisionClient.java    # 本地 mock 模型
      │     │  └─ OpenAiCompatibleVisionClient.java # DashScope OpenAI-Compatible 调用
      │     │
      │     ├─ config/                    # 配置类
      │     │  ├─ AssistantProperties.java # assistant.* 配置绑定
      │     │  ├─ SecurityConfig.java      # Spring Security 配置
      │     │  ├─ CorsConfig.java          # CORS 配置
      │     │  └─ ModelProviderConfig.java # mock / openai-compatible 模型选择
      │     │
      │     ├─ security/                  # JWT 安全认证
      │     │  ├─ JwtAuthenticationFilter.java # Bearer Token 过滤器
      │     │  ├─ JwtService.java          # JWT 签发与校验
      │     │  └─ UserPrincipal.java       # Spring Security 用户主体
      │     │
      │     ├─ dto/                       # 接口请求 / 响应 DTO
      │     │  ├─ ErrorResponse.java
      │     │  ├─ VisionChatResponse.java
      │     │  ├─ ResponseUsageDTO.java
      │     │  ├─ SessionUsageDTO.java
      │     │  ├─ auth/                   # 登录注册相关 DTO
      │     │  ├─ billing/                # Tokens 余额、充值、扣费展示 DTO
      │     │  └─ history/                # 会话历史列表、详情 DTO
      │     │
      │     ├─ entity/                    # JPA 实体
      │     │  ├─ UserEntity.java          # 用户表
      │     │  ├─ UserStatus.java          # 用户状态枚举
      │     │  ├─ billing/                # Token 账户、交易流水、AI 请求日志
      │     │  └─ history/                # 会话和消息历史
      │     │
      │     ├─ repository/                # JPA Repository
      │     │  ├─ UserRepository.java
      │     │  ├─ billing/                # Token 和请求日志 Repository
      │     │  └─ history/                # 会话历史 Repository
      │     │
      │     ├─ model/                     # 领域模型
      │     │  ├─ VisionChatCommand.java   # 一次视觉对话命令
      │     │  ├─ VisionChatResult.java    # 模型调用结果和 usage
      │     │  ├─ VisionFrame.java         # 上传帧及元数据
      │     │  ├─ ChatMessage.java         # 对话上下文消息
      │     │  └─ CachedVisionAnswer.java  # 缓存回答
      │     │
      │     ├─ exception/                 # 异常处理
      │     │  ├─ GlobalExceptionHandler.java
      │     │  ├─ CostLimitExceededException.java
      │     │  └─ ModelCallException.java
      │     │
      │     └─ util/                      # 工具类
      │        ├─ PromptBuilder.java       # 多帧视觉 prompt 构造
      │        └─ ImageHashUtil.java       # 图片 hash / 缓存 key 辅助
      │
      └─ test/                            # 后端测试目录
```

说明：前端当前主流程使用 `useRollingFrameBuffer.ts` 的最近 15 秒滑动帧；早期的 `KeyframePanel.vue`、`useKeyframeRecorder.ts`、`questionMode.ts` 主要是历史迭代保留，不是最终 UI 主路径。

---

## 4. 本地启动

### 4.1 准备环境

需要安装：

```text
JDK 17+
Node.js 18+
MySQL 8+
Maven 3.8+
```

也可以通过 `docker-compose.yml` 启动 MySQL。

---

### 4.2 启动 MySQL

使用 Docker：

```bash
docker compose up -d mysql
```

默认 MySQL 端口：

```text
localhost:3307
```

默认数据库：

```text
ai_vision_voice_assistant
```

如果手动初始化，可执行：

```text
backend/src/main/resources/db/mysql-auth-schema.sql
```

---

### 4.3 启动后端：Mock 模式

Mock 模式不调用真实模型，适合本地开发。

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

---

### 4.4 启动后端：qwen3-vl-plus 模式

配置环境变量：

```bash
export SPRING_PROFILES_ACTIVE=qwen
export DASHSCOPE_API_KEY=sk-你的百炼APIKey
export DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export ASSISTANT_MODEL_MODEL_NAME=qwen3-vl-plus
export ASSISTANT_AUTH_JWT_SECRET=请替换成至少32字节的随机密钥
```

启动：

```bash
cd backend
mvn spring-boot:run
```


---

### 4.5 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 代理会把 `/api` 请求转发到后端。

---

## 5. 使用流程

1. 打开前端页面；
2. 注册或登录；
3. 进入个人中心充值平台 Tokens；
4. 回到 AI 对话页；
5. 点击“开始实时对话”；
6. 授权摄像头和麦克风；
7. 直接说话，停顿后系统自动提交；
8. 后端调用多模态模型；
9. 前端展示并语音播报答案；
10. 每次成功请求后扣减 Tokens；
11. 个人中心查看会话历史。

---


## 6. 文档说明

- `docs/design.md`：设计文档，包含计划实现和最终实现的用户故事；
- `docs/cost-control.md`：运营成本控制策略，包含想到的策略和实际采用的策略；
- `docs/api.md`：后端接口文档；
- `docs/risk.md`：风险与后续优化；
- `docs/project-structure.md`：完整项目结构说明。

---

## 7. 隐私与安全说明

- 摄像头图片只用于本次模型请求，不保存数据库；
- 会话历史只保存文本和调用元数据；
- 模型 API Key 只存在后端环境变量；
- 生产环境必须使用 HTTPS，否则摄像头和麦克风权限会受限；
- 生产环境应关闭模拟充值，改为真实支付回调确认到账；
- 日志中不要打印图片 Base64、API Key、JWT 或用户密码。
