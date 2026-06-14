# 项目结构说明

本文件用于补充 README 中的项目目录说明，按最终项目代码整理。`.git/`、`.idea/`、`node_modules/`、`target/` 等本地开发、依赖安装或构建产物不属于需要提交说明的核心业务结构。

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
